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
package edu.cmu.cs.lti.ark.fn.identification.latentmodel;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.primitives.Ints;
import edu.cmu.cs.lti.ark.fn.optimization.Lbfgs;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.fn.utils.ThreadPool;
import edu.cmu.cs.lti.ark.util.SerializedObjects;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static org.apache.commons.io.IOUtils.closeQuietly;


public class TrainBatchModelDerThreaded {
	private static final Logger logger = Logger.getLogger(TrainBatchModelDerThreaded.class.getCanonicalName());

	private static final int BATCH_SIZE = 10;
	private static final boolean CACHE_FEATURES = false;
	private static final String FEATURE_FILENAME_PREFIX = "feats_";
	private static final String FEATURE_FILENAME_SUFFIX = ".jobj.gz";
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
	private final double[] params;
	private final List<String> eventFiles;
	private final String modelFile;
	private final int modelSize;
	private final boolean useL2Regularization;
	private final int numThreads;
	private final double lambda;
	private double[] gradients;
	private double[][] tGradients;
	private double[] tValues;
	// only used if CACHE_FEATURES is true
	private final LoadingCache<Integer, int[][][]> featuresByTargetIdx =
			CacheBuilder.newBuilder()
					.initialCapacity(16000)   // > number of targets in training data
					.build(new CacheLoader<Integer, int[][][]>() {
						@Override
						public int[][][] load(Integer key) throws IOException, ClassNotFoundException {
							return loadFeaturesForTarget(key);
						}
					});

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
		final TrainBatchModelDerThreaded tbm = new TrainBatchModelDerThreaded(
				options.alphabetFile.get(),
				options.eventsFile.get(),
				options.modelFile.get(),
				options.reg.get(),
				options.lambda.get(),
				restartFile.equals("null") ? Optional.<String>absent() : Optional.of(restartFile),
				numThreads);
		tbm.trainModel();
	}

	public TrainBatchModelDerThreaded(String alphabetFile,
									  String eventsDir,
									  String modelFile,
									  String reg,
									  double lambda,
									  Optional<String> restartFile,
									  int numThreads) throws IOException {
		this.modelSize = getNumFeatures(alphabetFile);
		this.modelFile = modelFile;
		this.eventFiles = getEventFiles(new File(eventsDir));
		this.useL2Regularization = reg.equals("reg");
		this.lambda = lambda / (double) eventFiles.size();
		this.numThreads = numThreads;
		this.params = restartFile.isPresent() ? loadModel(restartFile.get()) : getInitialParams();
	}

	public void trainModel() throws Exception {
		// minimum verbosity
		int[] iprint = new int[] {Lbfgs.DEBUG ? 1 : -1, 0};
		// lbfgs sets this flag to zero when it has converged
		int[] iflag = new int[] { 0 };
		tGradients = new double[numThreads][modelSize];  // gradients for each batch/thread
		gradients = new double[modelSize];  // gradients of all threads added together
		tValues = new double[numThreads];
		int iteration = 0;
		do {
			logger.info("Starting iteration:" + iteration);
			Arrays.fill(gradients, 0.0);
			double m_value = getValuesAndGradients();
			logger.info("Function value:" + m_value);
			riso.numerical.LBFGS.lbfgs(modelSize,
					Lbfgs.NUM_CORRECTIONS,
					params,
					m_value,
					gradients,
					false,
					new double[modelSize],
					iprint,
					Lbfgs.STOPPING_THRESHOLD,
					Lbfgs.XTOL,
					iflag
			);
			logger.info("Finished iteration:" + iteration);
			iteration++;
			if (iteration % Lbfgs.SAVE_EVERY_K == 0) {
				saveModel(params, modelFile + "_" + iteration);
			}
		} while (iteration <= Lbfgs.MAX_ITERATIONS && iflag[0] != 0);
		saveModel(params, modelFile);
	}

	private double getValuesAndGradients() {
		double value = 0.0;
		for (double[] tGradient : tGradients) Arrays.fill(tGradient, 0.0);
		Arrays.fill(tValues, 0.0);

		final ThreadPool threadPool = new ThreadPool(numThreads);
		int task = 0;
		for (int i = 0; i < eventFiles.size(); i += BATCH_SIZE) {
			final int taskId = task;
			final int start = i;
			final int end = Math.min(i + BATCH_SIZE, eventFiles.size());
			threadPool.runTask(new Runnable() {
				public void run() {
					logger.info("Task " + taskId + " : start");
					try {
						processBatch(taskId, start, end);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					logger.info("Task " + taskId + " : end");
				}
			});
			task++;
		}
		threadPool.join();
		// merge the calculated tValues and gradients together
		Arrays.fill(gradients, 0.0);
		for (int i = 0; i < numThreads; i++) {
			value += tValues[i];
			for (int j = 0; j < modelSize; j++) {
				gradients[j] += tGradients[i][j];
			}
		}
		System.out.println("Finished value and gradient computation.");
		return value;
	}

	public void processBatch(int taskId, int start, int end)
			throws ExecutionException, IOException, ClassNotFoundException {
		int threadId = taskId % numThreads;
		logger.info("Processing batch:" + taskId + " thread ID:" + threadId);
		for (int targetIdx = start; targetIdx < end; targetIdx++) {
			int[][][] featureArray = getFeaturesForTarget(targetIdx);
			int featArrLen = featureArray.length;
			double exp[][] = new double[featArrLen][];
			double sumExp[] = new double[featArrLen];
			double totalExp = 0.0;
			for (int i = 0; i < featArrLen; i++) {
				exp[i] = new double[featureArray[i].length];
				sumExp[i] = 0.0;  // the partition function
				for (int j = 0; j < exp[i].length; j++) {
					double weiFeatSum = 0.0;
					int[] feats = featureArray[i][j];
					for (int feat : feats) {
						weiFeatSum += params[feat];
					}
					exp[i][j] = Math.exp(weiFeatSum);
					sumExp[i] += exp[i][j];
				}
				totalExp += sumExp[i];
			}
			tValues[threadId] -= Math.log(sumExp[0]) - Math.log(totalExp);
			for (int i = 0; i < featArrLen; i++) {
				for (int j = 0; j < exp[i].length; j++) {
					double Y = 0.0;
					if (i == 0) {
						// the correct frame is always first
						Y = exp[i][j] / sumExp[i];
					}
					double YMinusP = Y - (exp[i][j] / totalExp);
					int[] feats = featureArray[i][j];
					for (int feat : feats) {
						tGradients[threadId][feat] -= YMinusP;
					}
				}
			}
			if (targetIdx % 100 == 0) {
				System.out.print(".");
				logger.info("" + targetIdx);
			}
		}
		if (useL2Regularization) {
			for (int i = 0; i < params.length; ++i) {
				final double weight = Math.log(params[i]);
				tValues[threadId] += lambda * (weight * weight);
				tGradients[threadId][i] += 2 * lambda * weight;
			}
		}
	}

	private int[][][] getFeaturesForTarget(int targetIdx) throws ExecutionException, IOException, ClassNotFoundException {
		if (CACHE_FEATURES) {
			return featuresByTargetIdx.get(targetIdx);
		} else {
			return loadFeaturesForTarget(targetIdx);
		}
	}

	private int[][][] loadFeaturesForTarget(Integer key) throws IOException, ClassNotFoundException {
		return SerializedObjects.readObject(eventFiles.get(key));
	}

	private double[] getInitialParams() {
		final double[] params = new double[modelSize];
		Arrays.fill(params, 1.0);
		return params;
	}

	/**
	 * Reads parameters from modelFile, one param per line.
	 *
	 * @param modelFile the filename to read from
	 */
	private double[] loadModel(String modelFile) throws IOException {
		final List<String> lines = Files.readLines(new File(modelFile), Charsets.UTF_8);
		final double[] params = new double[lines.size()];
		int i = 0;
		for (String line : lines) {
			params[i] = Double.parseDouble(line.trim());
			i++;
		}
		return params;
	}

	/**
	 * Writes the current parameters to modelFile, one param per line.
	 *
	 * @param modelFile the filename to write to
	 */
	private static void saveModel(double[] params, String modelFile) throws IOException {
		final BufferedWriter bWriter = new BufferedWriter(new FileWriter(modelFile));
		try {
			for (double param : params) bWriter.write(param + "\n");
		} finally {
			closeQuietly(bWriter);
		}
	}

	/** Reads the number of features in the model from the first line of alphabetFile */
	private static int getNumFeatures(String alphabetFile) throws IOException {
		final String firstLine = Files.readFirstLine(new File(alphabetFile), Charsets.UTF_8);
		final int numFeatures = Integer.parseInt(firstLine.trim()) + 1;
		logger.info("Number of features: " + numFeatures);
		return numFeatures;
	}

	/** Gets the list of all feature files */
	private static List<String> getEventFiles(File eventsDir) {
		final String[] files = eventsDir.list(featureFilenameFilter);
		Arrays.sort(files, featureFilenameComparator);
		final List<String> eventFiles = Lists.newArrayListWithExpectedSize(files.length);
		for (String file : files) eventFiles.add(eventsDir.getAbsolutePath() + "/" + file);
		logger.info("Total number of datapoints:" + eventFiles.size());
		return eventFiles;
	}
}
