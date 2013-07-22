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
package edu.cmu.cs.lti.ark.fn.identification.training;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.primitives.Ints;
import edu.cmu.cs.lti.ark.fn.optimization.Lbfgs;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.fn.utils.ThreadPool;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import gnu.trove.TIntDoubleHashMap;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;
import static edu.cmu.cs.lti.ark.util.Math2.sum;


public class TrainBatch {
	private static final Logger logger = Logger.getLogger(TrainBatch.class.getCanonicalName());

	public static final String FEATURE_FILENAME_PREFIX = "feats_";
	public static final String FEATURE_FILENAME_SUFFIX = ".jobj.gz";
	private static final FilenameFilter featureFilenameFilter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return name.startsWith(FEATURE_FILENAME_PREFIX) && name.endsWith(FEATURE_FILENAME_SUFFIX);
		}
	};
	private static final Comparator<String> featureFilenameComparator = new Comparator<String>() {
		private final int PREFIX_LEN = FEATURE_FILENAME_PREFIX.length();
		private final int SUFFIX_LEN = FEATURE_FILENAME_SUFFIX.length();
		public int compare(String a, String b) {
			final int aNum = Integer.parseInt(a.substring(PREFIX_LEN, a.length() - SUFFIX_LEN));
			final int bNum = Integer.parseInt(b.substring(PREFIX_LEN, b.length() - SUFFIX_LEN));
			return Ints.compare(aNum, bNum);
		}
	};
	private static float DEFAULT_COST_MULTIPLE = 5f;

	private final double[] params;
	private final List<String> eventFiles;
	private final String modelFile;
	private final boolean useL1Regularization;
	private final boolean useL2Regularization;
	private final int numThreads;
	private final double lambda;
	final double[] gradients;
	final double[][] tGradients;
	final double[] tValues;
	private final boolean usePartialCreditCosts;
	private float costMultiple;

	public static void main(String[] args) throws Exception {
		final FNModelOptions options = new FNModelOptions(args);
		LogManager.getLogManager().reset();
		final FileHandler fh = new FileHandler(options.logOutputFile.get(), true);
		fh.setFormatter(new SimpleFormatter());
		logger.addHandler(fh);

		final String restartFile = options.restartFile.get();
		final int numThreads = options.numThreads.present() ?
									options.numThreads.get() :
									Runtime.getRuntime().availableProcessors();
		final TrainBatch tbm = new TrainBatch(
				options.alphabetFile.get(),
				options.eventsFile.get(),
				options.modelFile.get(),
				options.reg.get(),
				options.lambda.get(),
				restartFile.equals("null") ? Optional.<String>absent() : Optional.of(restartFile),
				numThreads,
				options.usePartialCredit.get(),
				options.costMultiple.present() ? (float) options.costMultiple.get() : DEFAULT_COST_MULTIPLE);
		tbm.trainModel();
	}

	public TrainBatch(String alphabetFile,
					  String eventsDir,
					  String modelFile,
					  String reg,
					  double lambda,
					  Optional<String> restartFile,
					  int numThreads,
					  boolean usePartialCreditCosts,
					  float costMultiple) throws IOException {
		final int modelSize = AlphabetCreationThreaded.getAlphabetSize(alphabetFile);
		logger.info(String.format("Number of features: %d", modelSize));
		this.modelFile = modelFile;
		this.eventFiles = getEventFiles(new File(eventsDir));
		this.useL1Regularization = reg.toLowerCase().equals("l1");
		this.useL2Regularization = reg.toLowerCase().equals("l2");
		this.lambda = lambda;
		this.numThreads = numThreads;
		this.usePartialCreditCosts = usePartialCreditCosts;
		this.costMultiple = costMultiple;
		this.params = restartFile.isPresent() ? loadModel(restartFile.get()) : new double[modelSize];

		gradients = new double[modelSize];
		tGradients = new double[numThreads][modelSize];
		tValues = new double[numThreads];
	}

	public double[] trainModel() throws Exception {
		final Function<double[], Pair<Double, double[]>> valueAndGradientFunction =
				new Function<double[], Pair<Double, double[]>>() {
					@Override public Pair<Double, double[]> apply(double[] currentParams) {
						return getValuesAndGradientsThreaded(currentParams);
					}
				};
		return Lbfgs.trainAndSaveModel(params, valueAndGradientFunction, modelFile);
	}

	private Pair<Double, double[]> getValuesAndGradientsThreaded(final double[] currentParams) {
		final long startTime = System.currentTimeMillis();
		final int batchSize = (int) Math.ceil(eventFiles.size() / (double) numThreads);
		final ThreadPool threadPool = new ThreadPool(numThreads);
		int task = 0;
		for (int start = 0; start < eventFiles.size(); start += batchSize) {
			final int taskId = task;
			final Iterable<Integer> targetIdxs = xrange(start, Math.min(start + batchSize, eventFiles.size()));
			threadPool.runTask(new Runnable() {
				public void run() {
					logger.info(String.format("Task %d : start", taskId));
					try {
						tValues[taskId] = processBatch(targetIdxs, currentParams, tGradients[taskId]);
					} catch (Exception e) { throw new RuntimeException(e); }
					logger.info(String.format("Task %d : end", taskId));
				}
			});
			task++;
		}
		threadPool.join();
		// merge the calculated tValues and gradients together
		double logLikelihood = sum(tValues);
		Arrays.fill(gradients, 0.0);
		for (double[] tGrad : tGradients) {
			plusEquals(gradients, tGrad);
		}
		// LBGFS always minimizes, so objective is -log(likelihood)
		double negLogLikelihood = -logLikelihood; // * oneOverN;
		timesEquals(gradients, -1.0); //oneOverN);
		// add the regularization penalty
		if (useL1Regularization) {
			double penalty = 0.0;
			for (double param : currentParams) penalty += Math.abs(param);
			negLogLikelihood += lambda * penalty;
			for (int i = 0; i < currentParams.length; ++i) {
				gradients[i] += lambda * currentParams[i];
			}
		}
		if (useL2Regularization) {
			negLogLikelihood += lambda * dotProduct(currentParams, currentParams);
			for (int i = 0; i < currentParams.length; ++i) {
				gradients[i] += 2 * lambda * currentParams[i];
			}
		}
		final long endTime = System.currentTimeMillis();
		logger.info(String.format("Finished logLikelihood and gradient computation. Took %s seconds.", (endTime - startTime) / 1000.0));
		return Pair.of(negLogLikelihood, gradients);
	}

	/** Writes gradients to tGradients as a side-effect */
	private double processBatch(Iterable<Integer> targetIdxs,
								double[] currentParams,
								double[] tGradients) throws Exception {
		Arrays.fill(tGradients, 0.0);
		double logLikelihood = 0.0;
		for (int targetIdx : targetIdxs) {
			if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
			logLikelihood +=
					addLogLossAndGradientForExample(getFeaturesForTarget(targetIdx), currentParams, tGradients);
			if (targetIdx % 100 == 0) logger.info(String.format("target idx: %d", targetIdx));
		}
		return logLikelihood;
	}

	/** Writes to gradients as a side-effect */
	private double addLogLossAndGradientForExample(FeaturesAndCost[] featuresByFrame,
												   double[] currentParams,
												   double[] gradient) {
		int numFrames = featuresByFrame.length;
		double frameScore[] = new double[numFrames];
		double expdFrameScore[] = new double[numFrames];
		for (int frameIdx = 0; frameIdx < numFrames; frameIdx++) {
			final TIntDoubleHashMap frameFeatures = featuresByFrame[frameIdx].features;
			frameScore[frameIdx] = dotProduct(currentParams, frameFeatures);
			if (usePartialCreditCosts) {
				// softmax-margin
				frameScore[frameIdx] += costMultiple * featuresByFrame[frameIdx].cost;
			}
			expdFrameScore[frameIdx] = Math.exp(frameScore[frameIdx]);
		}
		final double logPartitionFn = Math.log(sum(expdFrameScore));

		// the correct frame is always first
		final int correctFrameIdx = 0;
		plusEquals(gradient, featuresByFrame[correctFrameIdx].features);
		for (int frameIdx = 0; frameIdx < numFrames; frameIdx++) {
			// estimate of P(y | x) * cost(y, y*) under the current parameters
			double prob = Math.exp(frameScore[frameIdx] - logPartitionFn);
			for (int featIdx : featuresByFrame[frameIdx].features.keys()) {
				gradient[featIdx] -= prob * featuresByFrame[frameIdx].features.get(featIdx);
			}
		}
		return frameScore[correctFrameIdx] - logPartitionFn;
	}

	/** Performs a dot product of the dense vector a and the sparse vector b (encoded as a map from index to value). */
	private static double dotProduct(double[] a, TIntDoubleHashMap b) {
		double result = 0.0;
		for (int featureIdx : b.keys()) {
			result += a[featureIdx] * b.get(featureIdx);
		}
		return result;
	}

	/** Performs a dot product of the two dense vectors a and b. */
	private static double dotProduct(double[] a, double[] b) {
		double result = 0.0;
		for (int featureIdx : xrange(a.length)) {
			result += a[featureIdx] * b[featureIdx];
		}
		return result;
	}

	/** Adds the sparse vector rhs to the dense vector lhs **/
	private static void plusEquals(double[] lhs, TIntDoubleHashMap rhs) {
		for (int i : rhs.keys()) {
			lhs[i] += rhs.get(i);
		}
	}

	/** Adds the dense vector rhs to the dense vector lhs **/
	private static void plusEquals(double[] lhs, double[] rhs) {
		for (int i = 0; i < lhs.length; i++) {
			lhs[i] += rhs[i];
		}
	}

	/** Multiplies the dense vector <code>vector</code> by the scalar <code>scalar</code> **/
	private static void timesEquals(double[] vector, double scalar) {
		for (int i = 0; i < vector.length; i++) {
			vector[i] *= scalar;
		}
	}

	/**
	 * Get features for every frame for given target
	 *
	 * @param targetIdx the target to get features for
	 * @return map from feature index to feature value (for each frame)
	 */
	private FeaturesAndCost[] getFeaturesForTarget(int targetIdx) throws Exception {
		return SerializedObjects.readObject(eventFiles.get(targetIdx));
	}

	/**
	 * Reads parameters from modelFile, one param per line.
	 *
	 * @param modelFile the filename to read from
	 */
	public static double[] loadModel(String modelFile) throws IOException {
		final List<String> lines = Files.readLines(new File(modelFile), Charsets.UTF_8);
		final double[] params = new double[lines.size()];
		for (int i : xrange(lines.size())) {
			params[i] = Double.parseDouble(lines.get(i).trim());
		}
		return params;
	}

	/** Gets the list of all feature files */
	private static List<String> getEventFiles(File eventsDir) {
		final String[] files = eventsDir.list(featureFilenameFilter);
		Arrays.sort(files, featureFilenameComparator);
		final List<String> eventFiles = Lists.newArrayListWithExpectedSize(files.length);
		for (String file : files) eventFiles.add(eventsDir.getAbsolutePath() + "/" + file);
		logger.info(String.format("Total number of datapoints: %d", eventFiles.size()));
		return eventFiles;
	}
}
