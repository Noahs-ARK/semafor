/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 *
 * AlphabetCreationThreaded.java is part of SEMAFOR 2.0.
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
import com.google.common.collect.*;
import com.google.common.io.Files;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.identification.IdFeatureExtractor;
import edu.cmu.cs.lti.ark.fn.identification.RequiredDataForFrameIdentification;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.util.FileUtil;
import edu.cmu.cs.lti.ark.util.SerializedObjects;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;
import static org.apache.commons.io.IOUtils.closeQuietly;

public class AlphabetCreationThreaded {
	private static final Logger logger = Logger.getLogger(AlphabetCreationThreaded.class.getCanonicalName());
	private static final int DEFAULT_MINIMUM_FEATURE_COUNT = 2;
	public static final String ALPHABET_FILENAME = "alphabet.dat";

	private final Set<String> allFrames;
	private final String parseFile;
	private final String frameElementsFile;
	private final int startIndex;
	private final int endIndex;
	private final int numThreads;
	private final IdFeatureExtractor featureExtractor;

	/**
	 * Parses commandline args, then creates a new {@link #AlphabetCreationThreaded} with them
	 * and calls {@link #createAlphabet}
	 *
	 * @param args commandline arguments. see {@link #AlphabetCreationThreaded}
	 *             for details.
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException, ExecutionException, InterruptedException {
		final FNModelOptions options = new FNModelOptions(args);

		LogManager.getLogManager().reset();
		final FileHandler fileHandler = new FileHandler(options.logOutputFile.get(), true);
		fileHandler.setFormatter(new SimpleFormatter());
		logger.addHandler(fileHandler);

		final int startIndex = options.startIndex.get();
		final int endIndex = options.endIndex.get();
		logger.info("Start:" + startIndex + " end:" + endIndex);
		final RequiredDataForFrameIdentification r =
				SerializedObjects.readObject(options.fnIdReqDataFile.get());

		final int minimumCount = options.minimumCount.present() ?
									options.minimumCount.get() :
									DEFAULT_MINIMUM_FEATURE_COUNT;
		final int numThreads = options.numThreads.present() ?
								options.numThreads.get() :
								Runtime.getRuntime().availableProcessors();
		final File alphabetDir = new File(options.modelFile.get());
		final String featureExtractorType =
				options.idFeatureExtractorType.present() ?
						options.idFeatureExtractorType.get() :
						"basic";
		final IdFeatureExtractor featureExtractor = IdFeatureExtractor.fromName(featureExtractorType);
		final AlphabetCreationThreaded events =
				new AlphabetCreationThreaded(
						options.trainFrameElementFile.get(),
						options.trainParseFile.get(),
						r.getFrameMap().keySet(),
						featureExtractor,
						startIndex,
						endIndex,
						numThreads);
		final Multiset<String> unconjoinedFeatures = events.createAlphabet();
		final File alphabetFile = new File(alphabetDir, ALPHABET_FILENAME);
		events.conjoinAndWriteAlphabet(unconjoinedFeatures, minimumCount, alphabetFile);
	}

	/**
	 * Creates a new AlphabetCreationThreaded with the given arguments
	 *
	 * @param frameElementsFile   path to file containing gold standard frame element
	 *                            annotations
	 * @param parseFile           path to file containing dependency parsed sentences (the same
*                            ones that are in frameElementsFile
	 * @param allFrames           set of all frame names
	 * @param featureExtractor    feature extractor
	 * @param startIndex          the line of the frameElementsFile to start at
	 * @param endIndex            the line of frameElementsFile to end at
	 * @param numThreads          the number of threads to run
	 */
	public AlphabetCreationThreaded(String frameElementsFile,
									String parseFile,
									Set<String> allFrames,
									IdFeatureExtractor featureExtractor,
									int startIndex,
									int endIndex,
									int numThreads) {
		this.frameElementsFile = frameElementsFile;
		this.parseFile = parseFile;
		this.allFrames = allFrames;
		this.featureExtractor = featureExtractor;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.numThreads = numThreads;
	}

	/**
	 * Splits frameElementLines into numThreads equally-sized batches and creates an alphabet
	 * file for each one.
	 *
	 * @throws IOException
	 */
	public Multiset<String> createAlphabet() throws IOException, ExecutionException, InterruptedException {
		final List<String> frameLines =
				Files.readLines(new File(frameElementsFile), Charsets.UTF_8)
						.subList(startIndex, endIndex);
		final int batchSize = (int) Math.ceil(frameLines.size() / (double) numThreads);
		final List<List<String>> frameLinesPartition = Lists.partition(frameLines, batchSize);
		final List<String> parseLines = Files.readLines(new File(parseFile), Charsets.UTF_8);
		final Multiset<String> alphabet = ConcurrentHashMultiset.create();
		final List<Callable<Integer>> jobs = Lists.newArrayListWithExpectedSize(numThreads);
		for (final int i : xrange(numThreads)) {
			jobs.add(newJob(i, frameLinesPartition.get(i), parseLines, alphabet));
		}
		final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
		final List<Future<Integer>> results = threadPool.invokeAll(jobs);
		threadPool.shutdown();
		try {
			for (Integer i : xrange(results.size())) {
				logger.info(String.format("Thread %d successfully processed %d lines", i, results.get(i).get()));
			}
		} finally {
			threadPool.shutdownNow();
		}
		return alphabet;
	}

	private Callable<Integer> newJob(final int threadId,
									 final List<String> frameLineBatch,
									 final List<String> parseLines,
									 final Multiset<String> alphabet) {
		return new Callable<Integer>() {
			public Integer call() {
				logger.info("Thread " + threadId + " : start");
				for (int i = 0; i < frameLineBatch.size() && !Thread.currentThread().isInterrupted(); i++) {
					processLine(frameLineBatch.get(i), parseLines, alphabet);
					if (i % 50 == 0) {
						logger.info("Thread " + i + "\n" +
								"Processed index:" + i + " of " + frameLineBatch.size() + "\n" +
								"Alphabet size:" + alphabet.elementSet().size());
					}
				}
				logger.info("Thread " + threadId + " : end");
				return frameLineBatch.size();
			}
		};
	}

	private void processLine(String frameLine, List<String> parseLines, Multiset<String> alphabet) {
		// Parse the frameLine
		final String[] toks = frameLine.split("\t");
		// throw out first two fields
		final List<String> tokens = Arrays.asList(toks).subList(2, toks.length);
		//final String frameName = tokens.get(1);
		final String[] targetIdxsStr = tokens.get(3).split("_");
		final int sentNum = Integer.parseInt(tokens.get(5));

		final int[] targetTokenIdxs = new int[targetIdxsStr.length];
		for (int j = 0; j < targetIdxsStr.length; j++)
			targetTokenIdxs[j] = Integer.parseInt(targetIdxsStr[j]);
		Arrays.sort(targetTokenIdxs);

		// Parse the parse line
		final String parseLine = parseLines.get(sentNum);
		final Sentence sentence = Sentence.fromAllLemmaTagsArray(AllLemmaTags.readLine(parseLine));

		// extract base features (not conjoined with frame names) for every frame
		alphabet.addAll(featureExtractor.getBaseFeatures(targetTokenIdxs, sentence).keySet());
	}

	public static BiMap<String, Integer> readAlphabetFile(File file) throws IOException {
		final BufferedReader bReader = Files.newReader(file, Charsets.UTF_8);
		try {
			final BiMap<String, Integer> alphabet = HashBiMap.create();
			String line;
			int i = 0;
			while ((line = bReader.readLine()) != null) {
				final String[] fields = line.trim().split("\t");
				alphabet.put(fields[0], i);
				i++;
			}
			return alphabet;
		} finally {
			closeQuietly(bReader);
		}
	}

	/** Gets the number of features in the model stored in alphabetFile */
	public static int getAlphabetSize(String alphabetFile) throws IOException {
		return FileUtil.countLines(alphabetFile);
	}

	private void conjoinAndWriteAlphabet(final Multiset<String> unconjoinedFeatures,
										 final int minimumCount,
										 File alphabetFile) throws IOException {
		final BufferedWriter output = Files.newWriter(alphabetFile, Charsets.UTF_8);
		final int unconjoinedSize = unconjoinedFeatures.elementSet().size();
		try {
			logger.info("Writing alphabet.");
			int numUnconjoined = 0;
			int numConjoined = 0;
			for (String unconjoinedFeature : unconjoinedFeatures.elementSet()) {
				if (unconjoinedFeatures.count(unconjoinedFeature) >= minimumCount) {
					final Set<String> conjoinedFeatureNames =
							featureExtractor.getConjoinedFeatureNames(allFrames, unconjoinedFeature);
					numConjoined += conjoinedFeatureNames.size();
					for (String feature : conjoinedFeatureNames) {
						output.write(String.format("%s\n", feature));
					}
				}
				numUnconjoined++;
				if (numUnconjoined % 50 == 0) {
					logger.info("Unconjoined: " + numUnconjoined + " of " + unconjoinedSize);
					logger.info("Conjoined: " + numConjoined );
				}
			}
			logger.info("Done writing alphabet.");
		} finally {
			closeQuietly(output);
		}
	}
}
