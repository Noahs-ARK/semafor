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
import com.google.common.base.Predicate;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.OutputSupplier;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.identification.FeatureExtractor;
import edu.cmu.cs.lti.ark.fn.identification.RequiredDataForFrameIdentification;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.fn.utils.ThreadPool;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static org.apache.commons.io.IOUtils.closeQuietly;

public class AlphabetCreationThreaded {
	private static final Logger logger = Logger.getLogger(AlphabetCreationThreaded.class.getCanonicalName());

	private static final int BATCH_SIZE = 100;
	private static final int MINIMUM_FEATURE_COUNT = 2;
	private static int EXPECTED_NUM_FEATURES = 6000000;
	public static final String ALPHABET_FILENAME = "alphabet.dat";
	private final THashMap<String, THashSet<String>> frameMap;
	private final String parseFile;
	private final File alphabetDir;
	private final String frameElementsFile;
	private final int startIndex;
	private final int endIndex;
	private final int numThreads;
	private final FeatureExtractor featureExtractor;

	/**
	 * Parses commandline args, then creates a new {@link #AlphabetCreationThreaded} with them
	 * and calls {@link #createAlphabet}
	 *
	 * @param args commandline arguments. see {@link #AlphabetCreationThreaded}
	 *             for details.
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
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

		final int numThreads = options.numThreads.present() ?
								options.numThreads.get() :
								Runtime.getRuntime().availableProcessors();
		final File alphabetDir = new File(options.modelFile.get());
		final AlphabetCreationThreaded events =
				new AlphabetCreationThreaded(alphabetDir,
						options.trainFrameElementFile.get(),
						options.trainParseFile.get(),
						r.getFrameMap(),
						new FeatureExtractor(),
						startIndex,
						endIndex,
						numThreads);
		events.createAlphabet();
		//combineAlphabets(alphabetDir);
	}

	/**
	 * Creates a new AlphabetCreationThreaded with the given arguments
	 *
	 * @param alphabetDir        the file prefix to which to write the resulting alphabet
	 * @param frameElementsFile   path to file containing gold standard frame element
	 *                            annotations
	 * @param parseFile           path to file containing dependency parsed sentences (the same
*                            ones that are in frameElementsFile
	 * @param frameMap            a map from frames to a set of disambiguated predicates
*                            (words along with part of speech tags, but in the style of FrameNet)
	 * @param featureExtractor    feature extractor
	 * @param startIndex          the line of the frameElementsFile to start at
	 * @param endIndex            the line of frameElementsFile to end at
	 * @param numThreads          the number of threads to run
	 */
	public AlphabetCreationThreaded(File alphabetDir,
									String frameElementsFile,
									String parseFile,
									THashMap<String, THashSet<String>> frameMap,
									FeatureExtractor featureExtractor,
									int startIndex,
									int endIndex,
									int numThreads) {
		this.alphabetDir = alphabetDir;
		this.frameElementsFile = frameElementsFile;
		this.parseFile = parseFile;
		this.frameMap = frameMap;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.numThreads = numThreads;
		this.featureExtractor = featureExtractor;
	}

	/**
	 * Splits frameElementLines into numThreads equally-sized batches and creates an alphabet
	 * file for each one.
	 *
	 * @throws IOException
	 */
	public void createAlphabet() throws IOException {
		final List<String> frameLines =
				Files.readLines(new File(frameElementsFile), Charsets.UTF_8)
						.subList(startIndex, endIndex);
		final List<String> parseLines = Files.readLines(new File(parseFile), Charsets.UTF_8);
		final Multiset<String> alphabet = ConcurrentHashMultiset.create();
		final ThreadPool threadPool = new ThreadPool(numThreads);
		int i = 0;
		for (int start = startIndex; start < endIndex; start += BATCH_SIZE) {
			final int threadId = i;
			final List<String> frameLineBatch =
					frameLines.subList(start, Math.min(frameLines.size(), start + BATCH_SIZE));
			threadPool.runTask(new Runnable() {
				public void run() {
					logger.info("Thread " + threadId + " : start");
					processBatch(threadId, frameLineBatch, parseLines, alphabet);
					logger.info("Thread " + threadId + " : end");
				}
			});
			i++;
		}
		threadPool.join();
		final OutputSupplier<OutputStreamWriter> outputSupplier =
				Files.newWriterSupplier(new File(alphabetDir, ALPHABET_FILENAME), Charsets.UTF_8);

		final Set<String> commonFeatures = Sets.filter(alphabet.elementSet(), new Predicate<String>() {
			@Override public boolean apply(String input) {
				return alphabet.count(input) >= MINIMUM_FEATURE_COUNT;
			} });
		writeAlphabet(commonFeatures, outputSupplier);
	}

	private void processBatch(int threadId, List<String> frameLines, List<String> parseLines, Multiset<String> alphabet) {
		for (int i = 0; i < frameLines.size(); i++) {
			processLine(frameLines.get(i), parseLines, alphabet);
			if (i % 50 == 0) {
				logger.info("Thread " + threadId + "\n" +
						"Processed index:" + i + " of " + frameLines.size() + "\n" +
						"Alphabet size:" + alphabet.elementSet().size());
			}
		}
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

		// extract features for every frame
		final Map<String, Map<String, Double>> featuresByFrame =
				featureExtractor.extractFeaturesByName(
					frameMap.keySet(),
					targetTokenIdxs,
					sentence
				);
		for (String frame : featuresByFrame.keySet()) {
			alphabet.addAll(featuresByFrame.get(frame).keySet());
		}
	}

	public static Map<String, Integer> readAlphabetFile(String filename) throws IOException {
		final BufferedReader bReader = new BufferedReader(new FileReader(filename));
		try {
			final Map<String, Integer> alphabet = new THashMap<String, Integer>(EXPECTED_NUM_FEATURES);
			String line;
			int i = 0;
			while ((line = bReader.readLine()) != null) {
				alphabet.put(line.trim(), i);
				i++;
			}
			return alphabet;
		} finally {
			closeQuietly(bReader);
		}
	}

	private void writeAlphabet(Set<String> alphabet, OutputSupplier<OutputStreamWriter> outputSupplier)
			throws IOException {
		final OutputStreamWriter output = outputSupplier.getOutput();
		try {
			output.write(String.format("%d\n", alphabet.size()));
			for (String feature : alphabet) {
				output.write(feature);
				output.write("\n");
			}
		} finally {
			closeQuietly(output);
		}
	}
}
