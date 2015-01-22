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

import edu.cmu.cs.lti.ark.ml.optimization.Lbfgs;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.fn.utils.ThreadPool;
import edu.cmu.cs.lti.ark.util.FileUtil;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import riso.numerical.LBFGS;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static edu.cmu.cs.lti.ark.util.SerializedObjects.readObject;
import static java.lang.Math.log;
import static java.lang.Math.min;

public class Training {
	private final String modelFile;
	private final ArrayList<FrameFeatures> frameFeaturesList;
	// all of these arrays get reused to conserve memory
	private final double[] weights;
	private final double[] gradients;
	private final double lambda; // L2 regularization hyperparameter
	private final int numThreads;
	private final double[][] threadGradients; // per-thread gradients
	private final double[] threadObjectives; // per-thread objective values
	
	/**
	 * @param args command-line arguments as follows:
	 *   frameFeaturesCacheFile: path to file containing a serialized cache of all of the features
	 *       extracted from the training data
	 *   alphabetFile: path to file containing the alphabet
	 *   lambda: L2 regularization hyperparameter
	 *   numThreads: the number of parallel threads to run while optimizing
	 *   modelFile: path to output file to write resulting model to. intermediate models will be written to
	 *       modelFile + "_" + i
	 */
	public static void main(String[] args) throws Exception {
		final FNModelOptions opts = new FNModelOptions(args);
		final String modelFile = opts.modelFile.get();
		final String alphabetFile = opts.alphabetFile.get();
		final String frameFeaturesCacheFile = opts.frameFeaturesCacheFile.get();
		final double lambda = opts.lambda.get();
		final int numThreads = opts.numThreads.get();
		final ArrayList<FrameFeatures> frameFeaturesList = readObject(frameFeaturesCacheFile);
		final Training training = new Training(modelFile, alphabetFile, frameFeaturesList, lambda, numThreads);
		training.runCustomLBFGS();
	}

	public Training(String modelFile,
					String alphabetFile,
					ArrayList<FrameFeatures> frameFeaturesList,
					double lambda,
					int numThreads) {
		this.modelFile = modelFile;
		this.frameFeaturesList = frameFeaturesList;
		this.lambda =  lambda;
		this.numThreads = numThreads;
		int numFeatures = readNumFeatures(alphabetFile);
		weights = new double[numFeatures];
		gradients = new double[numFeatures];
		threadGradients =  new double[numThreads][numFeatures];
		threadObjectives = new double[numThreads];
	}
	
	private int readNumFeatures(String alphabetFile) {
		final Scanner scanner = FileUtil.openInFile(alphabetFile);
		final int numFeatures = scanner.nextInt() + 1;
		scanner.close();
		return numFeatures;
	}

	private Pair<Double, double[]> getObjectiveAndGradient(FrameFeatures ffs) {
		final int modelSize = weights.length;
		final List<SpanAndCorrespondingFeatures[]> featsList = ffs.fElementSpansAndFeatures;
		final List<Integer> goldSpans = ffs.goldSpanIdxs;
		final double[] gradients = new double[modelSize];
		double value = 0.0;
		for(int i = 0; i < featsList.size(); i ++) {
			SpanAndCorrespondingFeatures[] featureArray = featsList.get(i);
			int goldSpan = goldSpans.get(i);
			int featArrLen = featureArray.length;
			double weiFeatSum[] = new double[featArrLen];
			double exp[] = new double[featArrLen];
			double sumExp = 0.0;
			for(int j = 0; j < featArrLen; j ++) {
				weiFeatSum[j] = weights[0];
				int[] feats = featureArray[j].features;
				for (int feat : feats) {
					if (feat == 0) {
						continue;
					}
					double weight = weights[feat];
					weiFeatSum[j] += weight;
				}
				exp[j] = Math.exp(weiFeatSum[j]);
				sumExp += exp[j];
			}
			value -= log(exp[goldSpan] / sumExp);
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
		return Pair.of(value, gradients);
	}


	public void processBatch(int taskID, int start, int end) {
		final int threadID = taskID % numThreads;
		System.out.println("Processing batch:" + taskID + " thread ID:" + threadID);
		final int safeEnd = min(end, frameFeaturesList.size());
		for (int index = start; index < safeEnd; index ++) {
			final FrameFeatures ffs = frameFeaturesList.get(index);
			final Pair<Double, double[]> objAndGrad = getObjectiveAndGradient(ffs);
			final double objective = objAndGrad.first;
			final double[] gradient = objAndGrad.second;
			threadObjectives[threadID] += objective;
			for (int i = 0; i < weights.length; i++) {
				threadGradients[threadID][i] += gradient[i];
			}
		}
	}

	public Runnable createTask(final int count, final int start, final int end) {
		return new Runnable() {
		      public void run() {
		        System.out.println("Task " + count + " : start");
		        processBatch(count, start, end);
		        System.out.println("Task " + count + " : end");
		      }
		};
	}

	/**
	 * @return the value of the function. fills out gradients as a side-effect
	 */
	private double getValuesAndGradients() {
		double value = 0.0;
		Arrays.fill(gradients, 0.0);
		for (int i = 0; i < numThreads; i++) {
			Arrays.fill(threadGradients[i], 0.0);
		}
		Arrays.fill(threadObjectives, 0.0);
		ThreadPool threadPool = new ThreadPool(numThreads);
		int batchSize = 10;
		int count = 0;
		for (int i = 0; i < frameFeaturesList.size(); i = i + batchSize) {
			threadPool.runTask(createTask(count, i, i + batchSize));
			count++;
		}
		threadPool.join();
		// sum up results from all threads
		for (int i = 0; i < numThreads; i++) {
			value += threadObjectives[i];
			for (int j = 0; j < weights.length; j++) {
				gradients[j] += threadGradients[i][j];
			}
		}
		// L2 regularization
		for(int i = 0; i < weights.length; i ++) {
			gradients[i] += 2 * lambda * weights[i];
			value += lambda * weights[i] * weights[i];
		}
		System.out.println("Finished value and gradient computation.");
		return value;
	}
	
	public void runCustomLBFGS() throws Exception {
		int modelSize = weights.length;
		double[] diagco = new double[modelSize];
		int[] iprint = { Lbfgs.DEBUG ? 1 : -1,  0 };   //output the minimum level of info
		int[] iflag = { 0 };
		int iteration = 0;
		do {
			System.out.println("Starting iteration:" + iteration);
			double m_value = getValuesAndGradients(); // fills out gradients as a side-effect
			System.out.println("Function value:"+m_value);
			LBFGS.lbfgs(modelSize,
					Lbfgs.NUM_CORRECTIONS,
					weights,
					m_value,
					gradients,
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
				writeModel(modelFile +"_"+iteration);
		} while (iteration <= Lbfgs.MAX_ITERATIONS && iflag[0] != 0);
		writeModel(modelFile);
	}

	public void writeModel(String modelFile) {
		final PrintStream ps = FileUtil.openOutFile(modelFile);
		System.out.println("Writing Model... ...");
		for (double w : weights) {
			ps.println(w);
		}
		System.out.println("Finished Writing Model");
		ps.close();
	}
}
