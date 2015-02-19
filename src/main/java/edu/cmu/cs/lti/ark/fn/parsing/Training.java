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

import edu.cmu.cs.lti.ark.fn.optimization.Lbfgs;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.fn.utils.ThreadPool;
import edu.cmu.cs.lti.ark.util.FileUtil;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import riso.numerical.LBFGS;

import java.io.PrintStream;
import java.util.*;

public class Training {
	private String mModelFile;
	private String mAlphabetFile;
	private ArrayList<FrameFeatures> mFrameList; 
	private double[] W;
	private double[] mGradients;
	private int numFeatures;
	private double mLambda;
	private int numDataPoints;
	private int mNumThreads;
	private double[][] tGradients;
	private double[] tValues;
	
	/**
	 *
	 * @param args command-line arguments as follows:
	 *             frameFeaturesCacheFile: path to file containing a serialized cache of all of the features
	 *                 extracted from the training data
	 *             alphabetFile: path to file containing the alphabet
	 *             lambda: regularization hyperparameter
	 *             numThreads: the number of parallel threads to run while optimizing
	 *             modelFile: path to output file to write resulting model to. intermediate models will be written to
	 *                 modelFile + "_" + i
	 */
	public static void main(String[] args) throws Exception {
		FNModelOptions opts = new FNModelOptions(args);
		String modelFile = opts.modelFile.get();
		String alphabetFile = opts.alphabetFile.get();
		String frameFeaturesCacheFile = opts.frameFeaturesCacheFile.get();
		double lambda = opts.lambda.get();
		int numThreads = opts.numThreads.get();
		ArrayList<FrameFeatures> list = SerializedObjects.readObject(frameFeaturesCacheFile);
		Training bpt = new Training();
		bpt.init(modelFile, alphabetFile, list, lambda, numThreads);
		bpt.runCustomLBFGS();
	}

	public Training() { }

	public void init(String modelFile,
					 String alphabetFile,
					 ArrayList<FrameFeatures> list,
					 double lambda,
					 int numThreads) {
		mModelFile = modelFile;
		mAlphabetFile = alphabetFile;
		initModel();
		mFrameList = list;
		mLambda = lambda;
		numDataPoints = mFrameList.size();
		mNumThreads = numThreads;
	}
	
	private void initModel() {
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
			for(int j = 0; j < featArrLen; j ++) {
				weiFeatSum[j] = W[0];
				int[] feats = featureArray[j].features;
				for (int feat : feats) {
					double weight = 0;
					try {
						weight = W[feat];
					} catch (Exception e) {
						System.out.println(e.getMessage() + W.length + "|"
								+ numFeatures);
					}
					if (feat == 0) {
						continue;
					}
					weiFeatSum[j] += weight;
				}
				exp[j] = Math.exp(weiFeatSum[j]);
				sumExp += exp[j];
			}
			value -= Math.log(exp[goldSpan] / sumExp);
			double YMinusP[] = new double[featureArray.length];
			for(int j = 0; j < featArrLen; j ++) {
				int Y = 0;
				if (j == goldSpan)
					Y = 1;
				int[] feats = featureArray[j].features;
				YMinusP[j] = Y - exp[j]/sumExp;
				gradients[0] -= YMinusP[j];
				for (int feat : feats) {
					gradients[feat] -= YMinusP[j];
				}
			}
		}		
		double lambda = mLambda / numDataPoints;
		for(int i = 0; i < sumDers.length; i ++) {
			sumDers[i] += (gradients[i] + 2*lambda*W[i]);
			value += lambda * W[i] * W[i];
		}
		return new Pair<Double, double[]>(value, sumDers);
	}


	public void processBatch(int taskID, int start, int end) {
		int threadID = taskID % mNumThreads;
		//System.out.println("Processing batch:" + taskID + " thread ID:" + threadID);
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
	
	
	public Runnable createTask(final int count, final int start, final int end) {
		return new Runnable() {
		      public void run() {
		        //System.out.println("Task " + count + " : start");
		        processBatch(count, start, end);
		        //System.out.println("Task " + count + " : end");
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
			//System.out.println("Starting iteration:" + iteration);
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

	public void writeModel(String modelFile) {
		PrintStream ps = FileUtil.openOutFile(modelFile);
		System.out.println("Writing Model... ...");
		for (double aW : W) {
			ps.println(aW);
		}
		System.out.println("Finished Writing Model");
		ps.close();
	}
}
