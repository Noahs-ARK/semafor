/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * TrainSGAModelDer.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.identification.latentmodel;

import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.util.FileUtil;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.optimization.SGA;

import java.io.*;
import java.util.*;

import static edu.cmu.cs.lti.ark.fn.parsing.Training.getRandArray;


public class TrainSGAModelDer
{
	
	public static void main(String[] args)
	{
		FNModelOptions io = new FNModelOptions(args);
		String alphabetFile = io.alphabetFile.get();
		String modelFile = io.modelFile.get();
		String reg = io.reg.get();
		double lambda = io.lambda.get();
		String restartFile = io.restartFile.get();
		if(restartFile.equals("null"))
			restartFile=null;
		int batchSize = io.batchSize.get();
		
		String eventsFilePrefix = io.eventsFile.get();
		File dirFile = new File(eventsFilePrefix);	
		FilenameFilter filter = new FilenameFilter(){
			public boolean accept(File dir, String name) {
				return name.startsWith("feats")&&name.endsWith(".jobj");
			}			
		};
		String[] files = dirFile.list(filter);
		Comparator<String> comp = new Comparator<String>(){
			public int compare(String o1, String o2) {
				o1=o1.substring(0,o1.length()-5);
				o2=o2.substring(0,o2.length()-5);
				int lastIndex = o1.lastIndexOf("_");
				int i1 = Integer.parseInt(o1.substring(lastIndex+1));
				lastIndex = o2.lastIndexOf("_");
				int i2 = Integer.parseInt(o2.substring(lastIndex+1));
				if(i1<i2)
					return -1;
				else if(i1==i2)
					return 0;
				else 
					return 1;
			}
		};
		Arrays.sort(files,comp);
		for(int i = 0; i < files.length; i ++)
		{
			files[i]=dirFile.getAbsolutePath()+"/"+files[i];
			//System.out.println(files[i]);
		}
		
		TrainSGAModelDer tbm = new TrainSGAModelDer();
		tbm.init(modelFile, alphabetFile, files, restartFile);
		tbm.trainSGA(30,batchSize,reg,lambda);
	}	
	
	private String mModelFile;
	private String mAlphabetFile;
	private String[] mEvents; 
	private double[] W;
	private int numFeatures;
	private Random rand;
	
	public TrainSGAModelDer()
	{

	}
	
	public void init(String modelFile, 
					 String alphabetFile, 
					 String[] list,
			         String restartFile
			         )
	{
		mModelFile = modelFile;
		mAlphabetFile = alphabetFile;
		initModel(restartFile);
		mEvents = list;
		rand = new Random(new Date().getTime());
	}

	private void initModel(String restartFile)
	{
		Scanner localsc = FileUtil.openInFile(mAlphabetFile);
		numFeatures = localsc.nextInt() + 1;
		localsc.close();
		W = new double[numFeatures];
		if(restartFile!=null)
		{	
			int i = 0;
			try
			{
				BufferedReader bReader = new BufferedReader(new FileReader(restartFile));
				String line = null;
				while((line=bReader.readLine())!=null)
				{
					W[i]=new Double(line.trim());
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
		else
		{
			for (int i = 0; i < numFeatures; i++)
			{
				W[i] = 0.0;
			}
		}
	}
	
	public double getValueOfSample(int index)
	{
		return 0.0;
	}
	
	public double[] getDerivativesOfSample(double[] sumDers, int index, double lambda)
	{
		int[][][] featureArray=(int[][][])SerializedObjects.readSerializedObject(mEvents[index]);
		
		double[] gradients = new double[sumDers.length];
		Arrays.fill(gradients, 0.0);
		
		int featArrLen = featureArray.length;
		
		double exp[][] = new double[featArrLen][];
		double sumExp[] = new double[featArrLen];
		double totalExp = 0.0;
		for(int i = 0; i < featArrLen; i ++)
		{
			exp[i] = new double[featureArray[i].length];
			sumExp[i] = 0.0;		
			for(int j = 0; j < exp[i].length; j ++)
			{
				double weiFeatSum = 0.0;
				int[] feats = featureArray[i][j];
				for (int k = 0; k < feats.length; k++)
				{
					weiFeatSum += W[feats[k]];
				}
				exp[i][j] = Math.exp(weiFeatSum);
				sumExp[i] += exp[i][j];
			}
			totalExp+=sumExp[i];
		}
		
		for(int i = 0; i < featArrLen; i ++)
		{
			for(int j = 0; j < exp[i].length; j ++)
			{
				double Y = 0.0;
				if(i==0)
				{
					Y = exp[i][j]/sumExp[i];
				}
				double YMinusP = Y - (exp[i][j]/totalExp);
				int[] feats = featureArray[i][j];
				for(int k = 0; k < feats.length; k ++)
				{
					gradients[feats[k]] -= YMinusP;
				}
			}
		}	
//		for(int j = 0; j < featArrLen; j ++)
//		{
//			int Y = 0;
//			if (j == goldSpan)
//				Y = 1;
//			int[] feats = featureArray[j].features;
//			YMinusP[j] = Y - exp[j]/sumExp;
//			gradients[0] -= YMinusP[j];
//			for(int k = 0; k < feats.length; k ++)
//			{
//				gradients[feats[k]] -= YMinusP[j];
//			}
//		}	
		for(int i = 0; i < sumDers.length; i ++)
		{
			sumDers[i] += (gradients[i] + lambda*W[i]);
		}
		return sumDers;
	}
	
	
	public void trainSGA(int TOTAL_PASSES, int batchsize, String reg, double lambda)
	{
		int sizeOfData = mEvents.length;
		lambda = lambda/sizeOfData;
		int maxUpdates = (int)(((double)TOTAL_PASSES*(double)sizeOfData)/(double)batchsize);
		int totalUpdates=0;
		int countPasses = 0;
		int countDataEncountered=0;
		System.out.println("Max updates:"+maxUpdates);
		while(totalUpdates<maxUpdates)
		{
			int[] arr = getRandArray(batchsize, sizeOfData, rand);
			double[] sumDers = new double[W.length];
			Arrays.fill(sumDers, 0.0);
			for(int j = 0; j < arr.length; j ++)
			{
				int sampleIndex = arr[j];
				System.out.println("Sample index:"+sampleIndex);
				sumDers = getDerivativesOfSample(sumDers,sampleIndex,lambda);
			}
			countDataEncountered+=batchsize;
			W = SGA.updateGradient(W, sumDers,0.1);
			System.out.println("Performed update number:"+totalUpdates);
			totalUpdates++;
			if(countDataEncountered>=sizeOfData)
			{
				System.out.println("\nCompleted pass number:"+(countPasses+1)+" total updates till now:"+totalUpdates);
				writeModel(mModelFile+"_"+countPasses);
				countPasses++;
				countDataEncountered=0;
			}
		}		
	}
	
	public void writeModel(String modelFile) {
		PrintStream ps = FileUtil.openOutFile(modelFile);
		// ps.println(w[0]);
		System.out.println("Writing Model... ...");
		// for (String key : paramIndex.keySet()) {\
		for (int i = 0; i < W.length; i++) {
			// ps.println(key + "\t" + w[paramIndex.get(key)]);
			ps.println(W[i]);
		}
		System.out.println("Finished Writing Model");
		ps.close();
	}
	
	public void writeModel() {
		PrintStream ps = FileUtil.openOutFile(mModelFile);
		// ps.println(w[0]);
		System.out.println("Writing Model... ...");
		// for (String key : paramIndex.keySet()) {\
		for (int i = 0; i < W.length; i++) {
			// ps.println(key + "\t" + w[paramIndex.get(key)]);
			ps.println(W[i]);
		}
		System.out.println("Finished Writing Model");
		ps.close();
	}
	
}
