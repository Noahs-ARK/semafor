/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * OptimizeMapReduce.java is part of SEMAFOR 2.0.
 * 
 * SEMAFOR 2.0 is free software: you can redistribute it and/or modify  it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * SEMAFOR 2.0 is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. 
 * 
 * You should have received a copy of the GNU General Public License along
 * with SEMAFOR 2.0.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package edu.cmu.cs.lti.ark.fn.identification;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.util.ExternalCommands;
import edu.cmu.cs.lti.ark.util.Shuffle;
import edu.cmu.cs.lti.ark.util.optimization.LDouble;

import riso.numerical.LBFGS;


import gnu.trove.THashMap;


public class OptimizeMapReduce
{
	/*
	 * for LBFGS
	 */
	protected static int m_max_its = 2000;
    //not sure how this parameter comes into play
    protected static double m_eps = 1.0e-4;
    protected static double xtol = 1.0e-10; //estimate of machine precision.  get this right
    //number of corrections, between 3 and 7
    //a higher number means more computation and time, but more accuracy, i guess
    protected static int m_num_corrections = 3; 
    protected static boolean m_debug = true;    
     
    private static boolean SHUFFLE=true;
    
    private String outputFileInHDFS;
    private String initialParamFileInHDFS;
    private String HADOOP_HOME;
    private String outputModelFile;
    private String outputDirectory;
    private String inputDirectory;
    private String intermediateDirectory;
    private String jarFile;
    private double totalValue=0;
    private String projectRoot;
    private THashMap<String,Double> gradients;
    private String HOD_DIR;
    private String mReg;
    private double mLambda;
    private String mLexSem;
    private String mAblationConfig;
    private String mWNZipFileInHDFS;
    private String mUnigramsFileInHDFS;
    private String mParamsSerFile;
    private String mTmpDir;
    private int mNumMapTasks;
    private int mMapMemorySize;
    private int mNumReduceTasks;
    private FNModelOptions opts;
    private boolean mIsHierarchical=false;
    private static final String MAP_REDUCE_JAR="framenet.jar";
    
    
   
    public OptimizeMapReduce(FNModelOptions opts)
    {
    	this.opts=opts;
    	inputDirectory = opts.m45InputDir.get();
    	outputFileInHDFS = opts.m45OutputDir+"/part-00000";
    	outputDirectory = opts.m45OutputDir.get();
    	intermediateDirectory = opts.m45IntermediateDir.get();
    	initialParamFileInHDFS = opts.m45IntermediateDir+"/initParamFile.txt";;
    	HADOOP_HOME=opts.m45HadoopHome.get();
    	outputModelFile=opts.outputParamFile.get();
    	jarFile=opts.projectRoot+"/lib/"+MAP_REDUCE_JAR;
    	projectRoot=opts.projectRoot.get();
    	totalValue=0.0;
    	HOD_DIR=opts.hodDir.get();
    	mReg=opts.reg.get();
    	int dataSize = opts.dataSize.get();
    	//mLambda=(io.lambda)/(double)dataSize;
    	mLambda=(opts.lambda.get());
    	mTmpDir=opts.tempDir.get();
		mWNZipFileInHDFS=opts.m45WordNetZipFile.get();
		gradients=new THashMap<String,Double>();
    	mNumMapTasks=new Integer(opts.numMapTasks.get());
    	mNumReduceTasks=new Integer(opts.numReduceTasks.get());
    	mMapMemorySize=new Integer(opts.memory.get());
    	String hM  = opts.m45IsHierarchicalModel.get();
    	if(hM.equals("yes"))
    		mIsHierarchical=true;
    }
    
    public void optimize()
    {
    	THashMap<String,Double> paramList = new THashMap<String, Double>();
    	try
    	{
    		String localInitParamFile=mTmpDir+"/initParamFile.txt";
    		BufferedReader bReader = new BufferedReader(new FileReader(localInitParamFile));
    		String pattern = null;
			while ((pattern = bReader.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(pattern.trim(),"\t");
				String paramName = st.nextToken().trim();
				String rest = st.nextToken().trim();
				String[] arr = rest.split(",");
				double value = new Double(arr[0].trim());
				boolean sign = new Boolean(arr[1].trim());
				LDouble val = new LDouble(value,sign);
				paramList.put(paramName, val.exponentiate());
			}
			bReader.close();
			runCustomLBFGS(paramList);
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    }    
    
    
    private void shuffleData()
    {
    	String tempFile = mTmpDir+"/trainData.dat";
    	File f = new File(tempFile);
    	f.delete();
    	String hdfsInputFile=inputDirectory+"/trainData.dat";
    	String command = HADOOP_HOME+"/bin/hadoop dfs -get "+hdfsInputFile+" "+tempFile;
    	ExternalCommands.runExternalCommand(command);
    	System.out.println("Removing input directory............");
    	command = HADOOP_HOME+"/bin/hadoop dfs -rmr "+inputDirectory;
    	ExternalCommands.runExternalCommand(command);
    	ArrayList<String> sentences = ParsePreparation.readSentencesFromFile(tempFile);
    	int size = sentences.size();
    	int[] n = Shuffle.permutation(size);
    	ArrayList<String> shuffledSentences = new ArrayList<String>();
    	for(int i = 0; i < size; i ++)
    	{
    		shuffledSentences.add(sentences.get(n[i]));
    	}
    	ParsePreparation.writeSentencesToTempFile(tempFile, shuffledSentences);
    	ExternalCommands.runExternalCommand(HADOOP_HOME+"/bin/hadoop dfs -put "+tempFile+" "+hdfsInputFile);  	
    }
    
    private void runAnIterationOfMapReduce(String paramFile)
    {
    	System.out.println("Removing output directory............");
    	String command = HADOOP_HOME+"/bin/hadoop dfs -rmr "+outputDirectory;
    	ExternalCommands.runExternalCommand(command);
    	System.out.println("\n\n");
    	if(SHUFFLE)
    	{
    		shuffleData();
    	}  	
    	String cmdLine="";
    	cmdLine+="m45inputdir:"+inputDirectory+" ";
    	cmdLine+="m45outputdir:"+outputDirectory+" ";
    	cmdLine+="m45framemap:"+opts.m45FrameMap+" ";
    	cmdLine+="m45allparses:"+opts.m45AllParsesFile+" ";
    	cmdLine+="m45parameterfile:"+paramFile+" ";
    	cmdLine+="memory:"+opts.memory+" ";
    	cmdLine+="nummaptasks:"+opts.numMapTasks+" ";
    	cmdLine+="numreducetasks:"+opts.numReduceTasks+" ";
    	cmdLine+="regularization:"+mReg+" ";
    	cmdLine+="lambda:"+mLambda+" ";
    	cmdLine+="m45wordnetzipfile:"+opts.m45WordNetZipFile+" ";
    	cmdLine+="m45wnrelationscachefile:"+opts.m45WordNetRelationsCacheFile;
    	if(mIsHierarchical)
    	{
    		cmdLine+=" m45hierarchyfile:"+opts.m45HierarchyFile.get()+" ";
    		cmdLine+=" m45ishierarchical:yes";
    	}
    	else
    	{
    		cmdLine+=" m45ishierarchical:no";
    	}    	
    	command = HADOOP_HOME+"/bin/hadoop --config "+HOD_DIR+" jar "+jarFile+" edu.cmu.cs.lti.ark.fn.identification.LRIterationMapReduce "+cmdLine;
    	ExternalCommands.runExternalCommand(command);
    	System.out.println("Finished running map reduce");
    }    
    
    public boolean deleteDir(File dir)
    {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}	    
		// The directory is now empty so delete it
		return dir.delete();
	}
    
    private void readParams(File f)
    {
    	FilenameFilter filter = new FilenameFilter(){
			public boolean accept(File dir, String name) {
				return name.startsWith("part");
			}
    	};
    	String[] files = f.list(filter);
    	for(int i = 0; i < files.length; i ++)
    	{
    		String filePath = f.getAbsolutePath()+"/"+files[i];
    		try
        	{
        		BufferedReader bReader = new BufferedReader(new FileReader(filePath));
        		String pattern = null;
    			while ((pattern = bReader.readLine()) != null)
    			{
    				StringTokenizer st = new StringTokenizer(pattern.trim(),"\t");
    				String paramName = st.nextToken().trim();
    				String rest = st.nextToken().trim();
    				String[] arr = rest.split(",");
    				double value = new Double(arr[0].trim());
    				boolean sign = true;
    				if(arr[1].trim().equals("false"))
    					sign=false;
    				LDouble val = new LDouble(value,sign);
    				if(paramName.equals("total"))
    				{
    					totalValue=new Double(val.exponentiate());
    				}
    				else
    				{
    					gradients.put(paramName, new Double(val.exponentiate()));
    				}
    			}
    			bReader.close();
        	}
        	catch(Exception e)
        	{
        		e.printStackTrace();
        	}
    	}
    }
    
    private void readValueAndGradients()
    {
    	gradients=new THashMap<String,Double>();
    	System.out.println("Copying over map reduce output");
    	File f = new File(mTmpDir+"/parts");
    	deleteDir(f);
    	f.mkdir();
    	String command = HADOOP_HOME+"/bin/hadoop dfs -get "+outputDirectory+"/part-* "+mTmpDir+"/parts/";
    	ExternalCommands.runExternalCommand(command);
    	readParams(f);	
    }
    
    public double[] getValues(String paramFile)
    {
    	System.out.println("Copying over paramfile");
    	File f = new File(initialParamFileInHDFS);
    	f.delete();
    	f = new File(mTmpDir+"/paramFile.txt");
    	f.delete();
    	String command = HADOOP_HOME+"/bin/hadoop dfs -get "+paramFile+" "+mTmpDir+"/paramFile.txt";
    	ExternalCommands.runExternalCommand(command);
    	THashMap<String,Double> paramValues=new THashMap<String,Double>();
    	ArrayList<String> keys =  new ArrayList<String>();
    	try
    	{
    		BufferedReader bReader = new BufferedReader(new FileReader(mTmpDir+"/paramFile.txt"));
    		String pattern = null;
			while ((pattern = bReader.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(pattern.trim(),"\t");
				String paramName = st.nextToken().trim();
				String rest = st.nextToken().trim();
				String[] arr = rest.split(",");
				double value = new Double(arr[0].trim());
				boolean sign = new Boolean(arr[1].trim());
				LDouble val = new LDouble(value,sign);
				paramValues.put(paramName, val.exponentiate());
				keys.add(paramName);
			}
			bReader.close();
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    	String[] paramArray = new String[keys.size()];
    	double[] result = new double[keys.size()];
    	keys.toArray(paramArray);
    	Arrays.sort(paramArray);
    	for(int i = 0; i < keys.size(); i ++)
    	{
    		result[i] = paramValues.get(paramArray[i]);
    	}
    	return result;
    }
    
    public void setValues(double[] values, THashMap<String,Double> paramList, String file)
    {
    	ArrayList<String> keys = new ArrayList<String>(paramList.keySet());
    	int size = keys.size();
    	String[] keyArray = new String[size];
    	keys.toArray(keyArray);
    	Arrays.sort(keyArray);
    	try {
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(file));
	    	for(int i = 0; i < size; i ++)
	    	{
	    		LDouble val = LDouble.convertToLogDomain(values[i]);
	    		bWriter.write(keyArray[i]+"\t"+val+"\n");
	    	}
	    	bWriter.close();
    	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    	
    	String command = HADOOP_HOME+"/bin/hadoop dfs -rmr "+intermediateDirectory+"/paramFile.txt"	;
    	ExternalCommands.runExternalCommand(command);
		ExternalCommands.runExternalCommand(HADOOP_HOME+"/bin/hadoop dfs -put "+file+" "+intermediateDirectory+"/paramFile.txt");
    }
    
    
    public double extractFunctionValueForLBFGS(boolean maximize)
    {
    	if(maximize)
    		return -totalValue;
    	else
    		return totalValue;
    }
    
    public double[] getGradientsForOptimizingRoutine(boolean maximize)
    {
    	int numParams = gradients.size();
    	ArrayList<String> keys = new ArrayList<String>(gradients.keySet());
    	double[] result = new double[numParams];
    	String[] keyArray = new String[numParams];
    	keys.toArray(keyArray);
    	Arrays.sort(keyArray);
    	for(int i = 0; i < numParams; i ++)
    	{
    		result[i] = gradients.get(keyArray[i]);
    		if(maximize)
    			result[i]=-1.0*result[i];
    		//System.out.println(result[i]);
    	}
    	return result;
    }    
    
    
	public void runCustomLBFGS(THashMap<String,Double> paramList) throws Exception
	{    
		int numParams = paramList.size();
		double[] diagco = new double[numParams];
		int[] iprint = new int[2];
		iprint[0] = m_debug?1:-1;  //output at every iteration (0 for 1st and last, -1 for never)
		iprint[1] = 0; //output the minimum level of info
		int[] iflag = new int[1];
		iflag[0] = 0;
		double[] gradient = new double[numParams];
		double[] m_estimate = new double[numParams];
		int iteration = 0;
		boolean maximize = true;
		String paramFile = initialParamFileInHDFS;
		do {
			m_estimate=getValues(paramFile);
			runAnIterationOfMapReduce(paramFile);
			readValueAndGradients();
			double m_value = extractFunctionValueForLBFGS(maximize);
			gradient = getGradientsForOptimizingRoutine(maximize);
			System.out.println("Function Value:"+m_value);
			LBFGS.lbfgs(numParams,
					m_num_corrections, 
					m_estimate, 
					m_value,
					gradient, 
					false, //true if we're providing the diag                                                                                of cov matrix Hk0 (?)
					diagco, //the cov matrix
					iprint, //type of output generated
					m_eps,
					xtol, //estimate of machine precision
					iflag //i don't get what this is about
			);
			setValues(m_estimate,paramList,mTmpDir+"/paramFile.txt");
			setValues(m_estimate,paramList,mTmpDir+"/paramFile_"+iteration+".txt");
			paramFile=intermediateDirectory+"/paramFile.txt";	
			iteration++;
		}		
		while (iteration <= m_max_its&&iflag[0] != 0);
		setValues(m_estimate,paramList,outputModelFile);
		
	}		
}
