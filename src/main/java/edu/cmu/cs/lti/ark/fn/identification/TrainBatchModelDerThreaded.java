/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * TrainBatchModelDerThreaded.java is part of SEMAFOR 2.0.
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
import java.util.Comparator;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import riso.numerical.LBFGS;

import edu.cmu.cs.lti.ark.fn.constants.FNConstants;
import edu.cmu.cs.lti.ark.fn.optimization.*;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.fn.utils.ThreadPool;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import gnu.trove.THashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;


public class TrainBatchModelDerThreaded
{
	protected double[] params = null;
	protected String[] eventFiles = null;	
	protected String modelFile = null;
	protected int modelSize = 0;
	protected String mReg = null;
	protected double mLambda = 0.0;
	protected double[] gradients = null;
	public static Logger logger;
	protected int mNumThreads = 1;
	protected double[][] tGradients = null;
	protected double[] tValues = null;
	
	public static void main(String[] args)
	{
		FNModelOptions io = new FNModelOptions(args);
		String alphabetFile = io.alphabetFile.get();
		String eventsFilePrefix = io.eventsFile.get();
		String modelFile = io.modelFile.get();
		String reg = io.reg.get();
		double lambda = io.lambda.get();
		String restartFile = io.restartFile.get();
		if(restartFile.equals("null"))
			restartFile=null;
		String logoutputfile = io.logOutputFile.get();
		FileHandler fh = null;
		LogManager logManager = LogManager.getLogManager();
		logManager.reset();
		try {
			fh = new FileHandler(logoutputfile, true);
       	 	fh.setFormatter(new SimpleFormatter());
       	 	logger = Logger.getLogger("TrainBatch");
            logger.addHandler(fh);   
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}	
		int numThreads = io.numThreads.get();
		TrainBatchModelDerThreaded tbm = new TrainBatchModelDerThreaded(alphabetFile, 
				eventsFilePrefix, 
				modelFile, 
				reg, lambda, 
				restartFile,
				numThreads);
		tbm.trainModel();
		fh.close();
	}	
	
	private void setFeatNum(String alphabetFile) {
		try
		{
			BufferedReader bReader = new BufferedReader(new FileReader(alphabetFile));
			String line = bReader.readLine().trim();
			line=line.trim();
			modelSize = (new Integer(line))+1;
			bReader.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	private void setEventFiles(String eventsFilePrefix) {
		File dirFile = new File(eventsFilePrefix);	
		FilenameFilter filter = new FilenameFilter(){
			public boolean accept(File dir, String name) {
				return name.startsWith("feats")&&name.contains(".jobj");
			}			
		};
		String[] files = dirFile.list(filter);
		Comparator<String> comp = new Comparator<String>(){
			public int compare(String o1, String o2) {
				o1=o1.substring(0,o1.length()-8);
				o2=o2.substring(0,o2.length()-8);
				int lastIndex = o1.lastIndexOf("_");
				int i1 = new Integer(o1.substring(lastIndex+1));
				lastIndex = o2.lastIndexOf("_");
				int i2 = new Integer(o2.substring(lastIndex+1));
				if(i1<i2)
					return -1;
				else if(i1==i2)
					return 0;
				else 
					return 1;
			}
		};
		Arrays.sort(files,comp);
		eventFiles = new String[files.length];
		for(int i = 0; i < files.length; i ++)
		{
			eventFiles[i] = dirFile.getAbsolutePath()+"/"+files[i];
		}
		System.out.println("Total number of datapoints:"+eventFiles.length);
		logger.info("Total number of datapoints:"+eventFiles.length);
	}
	
	private void initParams(String restartFile) {
		for(int i = 0; i < modelSize; i ++)
			params[i] = 1.0;
		if(restartFile!=null)
		{
			int i = 0;
			try
			{
				BufferedReader bReader = new BufferedReader(new FileReader(restartFile));
				String line = null;
				while((line=bReader.readLine())!=null)
				{
					params[i]=new Double(line.trim());
					i++;
				}
				bReader.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}
	}
	
	public TrainBatchModelDerThreaded(String alphabetFile, 
			String eventsFilePrefix, 
			String modelFile0, 
			String reg, 
			double lambda, 
			String restartFile,
			int numThreads)
	{
		setFeatNum(alphabetFile);
		modelFile = modelFile0;
		setEventFiles(eventsFilePrefix);
		mReg = reg;
		mLambda= lambda/(double)eventFiles.length;
		params = new double[modelSize];
		mNumThreads = numThreads;
		initParams(restartFile);
	}
	
	
	public void trainModel()
	{
		try
		{
			runCustomLBFGS();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void saveModel(String modelFile)
	{
		try
		{
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(modelFile));
			for(int i = 0; i < modelSize; i ++)		
			{
				bWriter.write(params[i]+"\n");
			}
			bWriter.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void processBatch(int taskID, int start, int end) {
		int threadID = taskID % mNumThreads;
		logger.info("Processing batch:" + taskID + " thread ID:" + threadID);
		if (end > eventFiles.length) {
			end = eventFiles.length;
		}
		for (int index = start; index < end; index ++) {
			int[][][] featureArray=(int[][][])SerializedObjects.readSerializedObject(eventFiles[index]);
			int featArrLen = featureArray.length;
			double exp[][] = new double[featArrLen][];
			double sumExp[] = new double[featArrLen];
			double totalExp = 0.0;
			for(int i = 0; i < featArrLen; i ++) {
				exp[i] = new double[featureArray[i].length];
				sumExp[i] = 0.0;		
				for(int j = 0; j < exp[i].length; j ++) {
					double weiFeatSum = 0.0;
					int[] feats = featureArray[i][j];
					for (int k = 0; k < feats.length; k++)
					{
						weiFeatSum += params[feats[k]];
					}
					exp[i][j] = Math.exp(weiFeatSum);
					sumExp[i] += exp[i][j];
				}
				totalExp += sumExp[i];
			}
			tValues[threadID] -= Math.log(sumExp[0]) - Math.log(totalExp);			
			for(int i = 0; i < featArrLen; i ++) {
				for(int j = 0; j < exp[i].length; j ++) {
					double Y = 0.0;
					if(i==0) {
						Y = exp[i][j]/sumExp[i];
					}
					double YMinusP = Y - (exp[i][j]/totalExp);
					int[] feats = featureArray[i][j];
					for(int k = 0; k < feats.length; k ++) {
						tGradients[threadID][feats[k]] -= YMinusP;
					}
				}
			}
			if (index % 100 == 0) { System.out.print("."); logger.info(""+index);};
		}
		if (mReg.equals("reg")) {
			for (int i = 0; i < params.length; ++i) {
				double weight = params[i];
				tValues[threadID] += mLambda * (weight * weight);
				tGradients[threadID][i] += 2 * mLambda * weight;
			}
		}	
	}
	
	public Runnable createTask(final int count, final int start, final int end)
	{
		return new Runnable() {
		      public void run() {
		        logger.info("Task " + count + " : start");
		        processBatch(count, start, end);
		        logger.info("Task " + count + " : end");
		      }
		};
	}	
	
	private double getValuesAndGradients() {
		double value = 0.0;
		Arrays.fill(gradients, 0.0);
		for (int i = 0; i < mNumThreads; i++) {
			Arrays.fill(tGradients[i], 0.0);
		}
		Arrays.fill(tValues, 0.0);
		ThreadPool threadPool = new ThreadPool(mNumThreads);
		int batchSize = 10;
		int count = 0;
		for (int i = 0; i < eventFiles.length; i = i + batchSize) {
			threadPool.runTask(createTask(count, i, i + batchSize));
			count++;
		}
		threadPool.join();		
		for (int i = 0; i < mNumThreads; i++) {
			value += tValues[i];
			for (int j = 0; j < modelSize; j++) {
				gradients[j] += tGradients[i][j];
			}
		}
		System.out.println("Finished value and gradient computation.");
		return value;
	}
	
	public void runCustomLBFGS() throws Exception
	{   
		double[] diagco = new double[modelSize];
		int[] iprint = new int[2];
		iprint[0] = FNConstants.m_debug?1:-1;
		iprint[1] = 0; //output the minimum level of info
		int[] iflag = new int[1];
		iflag[0] = 0;
		gradients = new double[modelSize];
		tGradients = new double[mNumThreads][modelSize];
		tValues = new double[mNumThreads];
		int iteration = 0;
		do {
			Arrays.fill(gradients, 0.0);
			logger.info("Starting iteration:" + iteration);
			double m_value = getValuesAndGradients();
			logger.info("Function value:"+m_value);
			LBFGS.lbfgs(modelSize,
					FNConstants.m_num_corrections, 
					params, 
					m_value,
					gradients, 
					false, //true if we're providing the diag of cov matrix Hk0 (?)
					diagco, //the cov matrix
					iprint, //type of output generated
					FNConstants.m_eps,
					FNConstants.xtol, //estimate of machine precision
					iflag //i don't get what this is about
			);
			logger.info("Finished iteration:"+iteration);
			iteration++;
			if (iteration%FNConstants.save_every_k==0)
				saveModel(modelFile+"_"+iteration);
		} while (iteration <= FNConstants.m_max_its&&iflag[0] != 0);
		saveModel(modelFile);
	}	
}
