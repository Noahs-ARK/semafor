/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * Training.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.parsing;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.fn.optimization.Lbfgs;
import edu.cmu.cs.lti.ark.fn.optimization.SGA;
import edu.cmu.cs.lti.ark.fn.utils.ThreadPool;
import edu.cmu.cs.lti.ark.util.FileUtil;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import riso.numerical.LBFGS;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

public class Training {
	private String mModelFile;
	private String mAlphabetFile;
	private ArrayList<FrameFeatures> mFrameList; 
	private double[] W;
	private double[] mGradients;
	private int numFeatures;
	private List<String> mFrameLines;
	private Random rand;
	private double mLambda;
	private int numDataPoints;
	private int mNumThreads;
	private double[][] tGradients;
	private double[] tValues;
	
	public Training()
	{

	}
	
	public void init(String modelFile,
					 String alphabetFile,
					 ArrayList<FrameFeatures> list,
					 String frFile) throws IOException {
		mModelFile = modelFile;
		mAlphabetFile = alphabetFile;
		initModel();
		mFrameList = list;
		mFrameLines = ParsePreparation.readLines(frFile);
		rand = new Random(new Date().getTime());
		mLambda = 0.0;
		numDataPoints = mFrameList.size();
		mNumThreads = 1;
	}

	public void init(String modelFile,
					 String alphabetFile,
					 ArrayList<FrameFeatures> list,
					 String frFile,
					 String reg,
					 double lambda,
					 int numThreads) throws IOException {
		mModelFile = modelFile;
		mAlphabetFile = alphabetFile;
		initModel();
		mFrameList = list;
		mFrameLines = ParsePreparation.readLines(frFile);
		rand = new Random(new Date().getTime());
		mLambda = lambda;
		numDataPoints = mFrameList.size();
		mNumThreads = numThreads;
	}
	
	private void initModel()
	{
		Scanner localsc = FileUtil.openInFile(mAlphabetFile);
		numFeatures = localsc.nextInt() + 1;
		localsc.close();
		W = new double[numFeatures];
		for (int i = 0; i < numFeatures; i++)
		{
			W[i] = 0.0;
		}
	}

	public Pair<Double, double[]> getDerivativesOfSample(double[] sumDers, int index) {
		FrameFeatures f = mFrameList.get(index);
		List<SpanAndCorrespondingFeatures[]> featsList = f.fElementSpansAndFeatures;
		List<Integer> goldSpans = f.goldSpanIdxs;
		double[] gradients = new double[sumDers.length];
		for(int i = 0; i < gradients.length; i ++)
			gradients[i] = 0.0;		
		double value = 0.0;
		for(int i = 0; i < featsList.size(); i ++) {
			SpanAndCorrespondingFeatures[] featureArray = featsList.get(i);
			int goldSpan = goldSpans.get(i);
			int featArrLen = featureArray.length;
			double weiFeatSum[] = new double[featArrLen];
			double exp[] = new double[featArrLen];
			double sumExp = 0.0;
			for(int j = 0; j < featArrLen; j ++)
			{
				weiFeatSum[j] = W[0];
				int[] feats = featureArray[j].features;
				for (int k = 0; k < feats.length; k++) {
					double weight = 0;
					try {
						weight = W[feats[k]];
					} catch (Exception e) {
						System.out.println(e.getMessage() + W.length + "|"
								+ numFeatures);
					}
					if (feats[k] == 0) {
						continue;
					}
					weiFeatSum[j] += weight;
				//	System.out.println("k = "+k);
				}
				exp[j] = Math.exp(weiFeatSum[j]);
				sumExp += exp[j];
				//System.out.println("j = "+j);
			}
			value -= Math.log(exp[goldSpan] / sumExp);
			double YMinusP[] = new double[featureArray.length];
			for(int j = 0; j < featArrLen; j ++)
			{
				int Y = 0;
				if (j == goldSpan)
					Y = 1;
				int[] feats = featureArray[j].features;
				YMinusP[j] = Y - exp[j]/sumExp;
				gradients[0] -= YMinusP[j];
				for(int k = 0; k < feats.length; k ++)
				{
					gradients[feats[k]] -= YMinusP[j];
					//System.out.println("k = "+j);
				}
			//	System.out.println("j = "+j);
			}
		}		
		double lambda = mLambda / numDataPoints;
		for(int i = 0; i < sumDers.length; i ++)
		{
			sumDers[i] += (gradients[i] + 2*lambda*W[i]);
			value += lambda * W[i] * W[i];
		}
		return new Pair<Double, double[]>(value, sumDers);
	}
	
	
	public void trainSGA(int TOTAL_PASSES, int batchsize)
	{
		int sizeOfData = mFrameList.size();
		int maxUpdates = (int)(((double)TOTAL_PASSES*(double)sizeOfData)/(double)batchsize);
		int totalUpdates=0;
		int countPasses = 0;
		int countDataEncountered=0;
		System.out.println("Max updates:"+maxUpdates);
		double[] sumDers = new double[W.length];
		while(totalUpdates<maxUpdates)
		{
			int[] arr = getRandArray(batchsize, sizeOfData, rand);
			Arrays.fill(sumDers, 0.0);
			for(int j = 0; j < arr.length; j ++)
			{
				int sampleIndex = arr[j];
				System.out.println("Sample index:"+sampleIndex);
				Pair<Double, double[]> p = getDerivativesOfSample(sumDers,sampleIndex);
				sumDers = p.second;
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
	
	
	public void trainBatch()
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
	
	
	public void processBatch(int taskID, int start, int end) {
		int threadID = taskID % mNumThreads;
		System.out.println("Processing batch:" + taskID + " thread ID:" + threadID);
		if (end > mFrameList.size()) {
			end = mFrameList.size();
		}
		double[] sumDers = new double[W.length];
		Arrays.fill(sumDers, 0.0);
		for (int index = start; index < end; index ++) {
			Pair<Double, double[]> p = getDerivativesOfSample(sumDers, index);
			sumDers = p.second;
			tValues[threadID] += p.first;
		}
		for (int i = 0; i < W.length; i++) {
			tGradients[threadID][i] += sumDers[i];
		}
	}
	
	
	public Runnable createTask(final int count, final int start, final int end)
	{
		return new Runnable() {
		      public void run() {
		        System.out.println("Task " + count + " : start");
		        processBatch(count, start, end);
		        System.out.println("Task " + count + " : end");
		      }
		};
	}

	/**
	 * @return the value of the function. fills out mGradients as a side-effect
	 */
	private double getValuesAndGradients() {
		double value = 0.0;
		Arrays.fill(mGradients, 0.0);
		for (int i = 0; i < mNumThreads; i++) {
			Arrays.fill(tGradients[i], 0.0);
		}
		Arrays.fill(tValues, 0.0);
		ThreadPool threadPool = new ThreadPool(mNumThreads);
		int batchSize = 10;
		int count = 0;
		for (int i = 0; i < mFrameList.size(); i = i + batchSize) {
			threadPool.runTask(createTask(count, i, i + batchSize));
			count++;
		}
		threadPool.join();		
		for (int i = 0; i < mNumThreads; i++) {
			value += tValues[i];
			for (int j = 0; j < W.length; j++) {
				mGradients[j] += tGradients[i][j];
			}
		}
		System.out.println("Finished value and gradient computation.");
		return value;
	}
	
	public void runCustomLBFGS() throws Exception
	{   
		int modelSize = W.length;
		double[] diagco = new double[modelSize];
		int[] iprint = new int[2];
		iprint[0] = Lbfgs.DEBUG ?1:-1;
		iprint[1] = 0; //output the minimum level of info
		int[] iflag = new int[1];
		iflag[0] = 0;
		mGradients = new double[modelSize];
		tGradients =  new double[mNumThreads][modelSize];
		tValues = new double[mNumThreads];
		int iteration = 0;
		do {
			Arrays.fill(mGradients, 0.0);
			System.out.println("Starting iteration:" + iteration);
			double m_value = getValuesAndGradients();
			System.out.println("Function value:"+m_value);
			LBFGS.lbfgs(modelSize,
					Lbfgs.NUM_CORRECTIONS,
					W, 
					m_value,
					mGradients, 
					false, //true if we're providing the diag of cov matrix Hk0 (?)
					diagco, //the cov matrix
					iprint, //type of output generated
					Lbfgs.STOPPING_THRESHOLD,
					Lbfgs.XTOL, //estimate of machine precision
					iflag //i don't get what this is about
			);
			System.out.println("Finished iteration:"+iteration);
			iteration++;
			if (iteration% Lbfgs.SAVE_EVERY_K ==0)
				writeModel(mModelFile+"_"+iteration);
		} while (iteration <= Lbfgs.MAX_ITERATIONS &&iflag[0] != 0);
		writeModel(mModelFile);
	}	
	
	
	public static int[] getRandArray(int batchSize, int sizeOfData, Random rand) {
		int count = 0;
		ArrayList<Integer> list = new ArrayList<Integer>();
		while(count<batchSize) {
			Integer next = rand.nextInt(sizeOfData);
			if (!list.contains(next)) {
				list.add(next);
				count++;
			}
		}
		int[] arr = new int[batchSize];
		for(int i = 0; i < arr.length; i ++)
		{
			arr[i] = list.get(i);
		}
		return arr;
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
