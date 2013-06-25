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
package edu.cmu.cs.lti.ark.fn.identification.latentmodel;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags;
import edu.cmu.cs.lti.ark.fn.identification.RequiredDataForFrameIdentification;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.fn.wordnet.CachedRelations;
import edu.cmu.cs.lti.ark.fn.wordnet.Relations;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.nlp.CachedLemmatizer;
import edu.cmu.cs.lti.ark.util.nlp.Lemmatizer;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.logging.*;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.apache.commons.io.IOUtils.closeQuietly;

public class LatentAlphabetCreationThreaded {
	private static final Logger logger = Logger.getLogger(LatentAlphabetCreationThreaded.class.getCanonicalName());

	private static final int BATCH_SIZE = 100;
	private static int EXPECTED_NUM_FEATURES = 3000000;
	public static final String ALPHABET_FILENAME = "alphabet.dat";
	final static FilenameFilter LOCAL_ALPHABET_FILENAME_FILTER = new FilenameFilter() {
		public boolean accept(File dir, String filename) {
			return filename.startsWith(ALPHABET_FILENAME + "_");
		}
	};
	private final THashMap<String, THashSet<String>> frameMap;
	private final String parseFile;
	private final File alphabetDir;
	private final String frameElementsFile;
	private final int startIndex;
	private final int endIndex;
	private final int numThreads;
	private LatentFeatureExtractor featureExtractor;

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
		final Relations wnRelations =
				new CachedRelations(r.getRevisedRelMap(), r.getRelatedWordsForWord());
		final Lemmatizer lemmatizer =
				new CachedLemmatizer(r.getHvLemmaCache());
		final int numThreads = options.numThreads.present() ?
								options.numThreads.get() :
								Runtime.getRuntime().availableProcessors();
		final File alphabetDir = new File(options.modelFile.get());
		final LatentAlphabetCreationThreaded events =
				new LatentAlphabetCreationThreaded(alphabetDir,
						options.trainFrameElementFile.get(),
						options.trainParseFile.get(),
						r.getFrameMap(),
						new LatentFeatureExtractor(wnRelations, lemmatizer),
						startIndex,
						endIndex,
						numThreads);
		events.createLocalAlphabets();
		combineAlphabets(alphabetDir);
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
	 * @param startIndex          the line of the frameElementsFile to start at
	 * @param endIndex            the line of frameElementsFile to end at
	 * @param numThreads          the number of threads to run
	 */
	public LatentAlphabetCreationThreaded(File alphabetDir,
										  String frameElementsFile,
										  String parseFile,
										  THashMap<String, THashSet<String>> frameMap,
										  LatentFeatureExtractor featureExtractor,
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
	 * @throws java.io.IOException
	 */
	public void createLocalAlphabets() throws IOException {
		final List<String> frameLines =
				Files.readLines(new File(frameElementsFile), Charsets.UTF_8)
						.subList(startIndex, endIndex);
		final List<String> parseLines = Files.readLines(new File(parseFile), Charsets.UTF_8);

		final ExecutorService threadPool = newFixedThreadPool(numThreads);
		int i = 0;
		for (int start = startIndex; start < endIndex; start += BATCH_SIZE) {
			final int threadId = i;
			final List<String> frameLineBatch =
					frameLines.subList(start, Math.min(frameLines.size(), start + BATCH_SIZE));
			threadPool.execute(new Runnable() {
				public void run() {
					logger.info("Thread " + threadId + " : start");
					processBatch(threadId, frameLineBatch, parseLines);
					logger.info("Thread " + threadId + " : end");
				}
			});
			i++;
		}
		threadPool.shutdown();
	}

	private void processBatch(int threadId, List<String> frameLines, List<String> parseLines) {
		final Set<String> alphabet = Sets.newHashSet();
		for (int i = 0; i < frameLines.size(); i++) {
			processLine(frameLines.get(i), parseLines, alphabet);
			if (i % 10 == 0) {
				logger.info("Thread " + threadId + "\n" +
							"Processed index:" + i + " of " + frameLines.size() + "\n" +
							"Alphabet size:" + alphabet.size());
			}
		}
		try {
			writeAlphabetFile(alphabet, alphabetDir + "_" + threadId);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void processLine(String frameLine, List<String> parseLines, Set<String> alphabet) {
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

	private void extractFeaturesAndAddToAlphabet(String frame,
												 int[] targetTokenIdxs,
												 String[][] allLemmaTags,
												 Set<String> alphabet) {
		final THashSet<String> hiddenUnits = frameMap.get(frame);
		final DependencyParse parse = DependencyParse.processFN(allLemmaTags, 0.0);
		for (String unit : hiddenUnits) {
			final IntCounter<String> valMap = featureExtractor.extractFeatures(
					frame,
					targetTokenIdxs,
					unit,
					allLemmaTags,
					parse,
					true);
			alphabet.addAll(valMap.keySet());
		}
	}

	/**
	 * Combines the multiple alphabet files created by createLocalAlphabets into one
	 * alphabet file
	 */
	public static void combineAlphabets(File alphabetDir) throws IOException {
		final String[] files = alphabetDir.list(LOCAL_ALPHABET_FILENAME_FILTER);
		final Set<String> alphabet = Sets.newHashSet();
		for (String file: files) {
			final String path = alphabetDir.getAbsolutePath() + "/" + file;
			if (logger.isLoggable(Level.INFO)) logger.info("reading path: " + path);
			final Map<String, Integer> localAlphabet = readAlphabetFile(path);
			alphabet.addAll(localAlphabet.keySet());
		}
		writeAlphabetFile(alphabet, alphabetDir.getAbsolutePath() + "/" + ALPHABET_FILENAME);
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

	public static void writeAlphabetFile(Set<String> alphabet, String filename) throws IOException {
		FileUtils.writeLines(new File(filename), alphabet);
	}
}
