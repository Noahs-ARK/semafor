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
package edu.cmu.cs.lti.ark.fn.identification;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.apache.commons.io.IOUtils.closeQuietly;

public class AlphabetCreationThreaded {
	private static final Logger logger = Logger.getLogger(AlphabetCreationThreaded.class.getCanonicalName());

	private final THashMap<String, THashSet<String>> frameMap;
	private final String parseFile;
	private final String alphabetFile;
	private final String frameElementsFile;
	private final int startIndex;
	private final int endIndex;
	private final int numThreads;
	private final FeatureExtractor.Relations wnRelations;
	private final FeatureExtractor.Lemmatizer lemmatizer;

	/**
	 * Parses commandline args, then creates a new {@link #AlphabetCreationThreaded} with them
	 * and calls {@link #createEvents}.
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
		final FeatureExtractor.Relations wnRelations =
				new FeatureExtractor.CachedRelations(r.getRevisedRelMap(), r.getRelatedWordsForWord());
		final FeatureExtractor.Lemmatizer lemmatizer =
				new FeatureExtractor.CachedLemmatizer(r.getHvLemmaCache());
		final AlphabetCreationThreaded events =
				new AlphabetCreationThreaded(options.modelFile.get(),
						options.trainFrameElementFile.get(),
						options.trainParseFile.get(),
						r.getFrameMap(),
						wnRelations,
						lemmatizer,
						startIndex,
						endIndex,
						options.numThreads.get());
		events.createEvents();
	}

	/**
	 * Creates a new AlphabetCreationThreaded with the given arguments
	 *
	 * @param alphabetFile        the file prefix to which to write the resulting alphabet
	 * @param frameElementsFile   path to file containing gold standard frame element
	 *                            annotations
	 * @param parseFile           path to file containing dependency parsed sentences (the same
 *                            ones that are in frameElementsFile
	 * @param frameMap            a map from frames to a set of disambiguated predicates
*                            (words along with part of speech tags, but in the style of FrameNet)
	 * @param startIndex          the line of the frameElementsFile to start at
	 * @param endIndex            the line of frameElementsFile to end at
	 * @param numThreads          the number of threads to run
	 */
	public AlphabetCreationThreaded(String alphabetFile,
									String frameElementsFile,
									String parseFile,
									THashMap<String, THashSet<String>> frameMap,
									FeatureExtractor.Relations wnRelations,
									FeatureExtractor.Lemmatizer lemmatizer,
									int startIndex,
									int endIndex,
									int numThreads) {
		this.alphabetFile = alphabetFile;
		this.frameElementsFile = frameElementsFile;
		this.parseFile = parseFile;
		this.frameMap = frameMap;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.numThreads = numThreads;
		this.wnRelations = wnRelations;
		this.lemmatizer = lemmatizer;

	}

	/**
	 * Splits frameElementLines into numThreads equally-sized batches and creates an alphabet
	 * file for each one.
	 *
	 * @throws IOException
	 */
	public void createEvents() throws IOException {
		final List<String> frameLines =
				Files.readLines(new File(frameElementsFile), Charsets.UTF_8)
						.subList(startIndex, endIndex);
		final List<String> parseLines = Files.readLines(new File(parseFile), Charsets.UTF_8);
		final int dataCount = endIndex - startIndex;
		final int batchSize = (int) (Math.ceil((double) dataCount / (double) numThreads));

		final ExecutorService threadPool = newFixedThreadPool(numThreads);
		for (int i = 0; i < numThreads; i++) {
			final int threadId = i;
			final int start = threadId * batchSize;
			final int end = Math.min(frameLines.size(), start + batchSize);
			threadPool.execute(new Runnable() {
				public void run() {
					logger.info("Thread " + threadId + " : start");
					processBatch(threadId, frameLines.subList(start, end), parseLines);
					logger.info("Thread " + threadId + " : end");
				}
			});
		}
		threadPool.shutdown();
	}

	private void processBatch(int threadId, List<String> frameLines, List<String> parseLines) {
		final Map<String, Integer> alphabet = new THashMap<String, Integer>();
		for (int i = 0; i < frameLines.size(); i++) {
			logger.info("Thread " + threadId + " Processing:" + i + " of " + frameLines.size());
			processLine(frameLines.get(i), parseLines, alphabet);
			logger.info("Processed index:" + i + " alphsize:" + alphabet.size());
		}
		try {
			writeAlphabetFile(alphabet, alphabetFile + "_" + threadId);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void extractFeaturesAndAddToAlphabet(String frame,
												 int[] targetTokenIdxs,
												 String[][] allLemmaTags,
												 Map<String, Integer> alphabet) {
		final THashSet<String> hiddenUnits = frameMap.get(frame);
		final DependencyParse parse = DependencyParse.processFN(allLemmaTags, 0.0);
		for (String unit : hiddenUnits) {
			final IntCounter<String> valMap = FeatureExtractor.extractFeatures(
					frame,
					targetTokenIdxs,
					unit,
					allLemmaTags,
					parse,
					wnRelations,
					lemmatizer,
					true);
			for (String feat : valMap.keySet()) {
				if (!alphabet.containsKey(feat)) {
					alphabet.put(feat, alphabet.size() + 1);
				}
			}
		}
	}

	private void processLine(String frameLine, List<String> parseLines, Map<String, Integer> alphabet) {
		// Parse the frameLine
		final String[] toks = frameLine.split("\t");
		// throw out first two fields
		final List<String> tokens = Arrays.asList(toks).subList(2, toks.length);
		final String frameName = tokens.get(1);
		final String[] targetIdxsStr = tokens.get(3).split("_");
		final int sentNum = Integer.parseInt(tokens.get(5));

		final int[] targetTokenIdxs = new int[targetIdxsStr.length];
		for (int j = 0; j < targetIdxsStr.length; j++)
			targetTokenIdxs[j] = Integer.parseInt(targetIdxsStr[j]);
		Arrays.sort(targetTokenIdxs);

		// Parse the parse line
		final String parseLine = parseLines.get(sentNum);
		final String[][] data = AllLemmaTags.readLine(parseLine);

		// extract features for every frame
		for (String frame : frameMap.keySet()) {
			extractFeaturesAndAddToAlphabet(frame, targetTokenIdxs, data, alphabet);
		}
	}

	private void writeAlphabetFile(Map<String, Integer> alphabet, String filename) throws IOException {
		final BufferedWriter bWriter = new BufferedWriter(new FileWriter(filename));
		try {
			bWriter.write(alphabet.size() + "\n");
			final Set<String> set = alphabet.keySet();
			for (String key : set) {
				bWriter.write(key + "\t" + alphabet.get(key) + "\n");
			}
		} finally {
			closeQuietly(bWriter);
		}
	}
}
