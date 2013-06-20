/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 *
 * ExtractTrainingFeatures.java is part of SEMAFOR 2.0.
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

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static java.util.concurrent.Executors.newFixedThreadPool;

public class ExtractTrainingFeatures {
	private static final Logger logger = Logger.getLogger(ExtractTrainingFeatures.class.getCanonicalName());

	private final THashMap<String, THashSet<String>> frameMap;
	private final String parseFile;
	private final String eventDir;
	private final String frameElementsFile;
	private final Map<String, Integer> alphabet;
	private final int startIndex;
	private final int endIndex;
	private final int numThreads;
	private final FeatureExtractor.Relations wnRelations;
	private final FeatureExtractor.Lemmatizer lemmatizer;

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		final FNModelOptions options = new FNModelOptions(args);

		LogManager.getLogManager().reset();
		final FileHandler fh = new FileHandler(options.logOutputFile.get(), true);
		fh.setFormatter(new SimpleFormatter());
		logger.addHandler(fh);

		final int startIndex = options.startIndex.get();
		final int endIndex = options.endIndex.get();
		logger.info("Start:" + startIndex + " end:" + endIndex);
		final RequiredDataForFrameIdentification r = SerializedObjects.readObject(options.fnIdReqDataFile.get());
		final FeatureExtractor.Relations wnRelations =
				new FeatureExtractor.CachedRelations(r.getRevisedRelMap(), r.getRelatedWordsForWord());
		final FeatureExtractor.Lemmatizer lemmatizer =
				new FeatureExtractor.CachedLemmatizer(r.getHvLemmaCache());
		logger.info("Reading alphabet");
		final Map<String, Integer> alphabet = CombineAlphabets.readAlphabetFile(options.modelFile.get());
		logger.info("Done reading alphabet");
		final ExtractTrainingFeatures events =
				new ExtractTrainingFeatures(alphabet,
						options.eventsFile.get(),
						options.trainFrameElementFile.get(),
						options.trainParseFile.get(),
						r.getFrameMap(),
						startIndex,
						endIndex,
						options.numThreads.get(),
						wnRelations,
						lemmatizer);
		events.createEvents();
	}

	public ExtractTrainingFeatures(Map<String, Integer> alphabet,
								   String eventDir,
								   String frameElementsFile,
								   String parseFile,
								   THashMap<String, THashSet<String>> frameMap,
								   int startIndex,
								   int endIndex,
								   int numThreads,
								   FeatureExtractor.Relations wnRelations,
								   FeatureExtractor.Lemmatizer lemmatizer) {
		this.alphabet = alphabet;
		this.frameMap = frameMap;
		this.parseFile = parseFile;
		this.frameElementsFile = frameElementsFile;
		this.eventDir = eventDir;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.numThreads = numThreads;
		this.wnRelations = wnRelations;
		this.lemmatizer = lemmatizer;
	}

	public void createEvents() throws IOException {
		final List<String> frameLines =
				Files.readLines(new File(frameElementsFile), Charsets.UTF_8)
						.subList(startIndex, endIndex);
		final List<String> parseLines = Files.readLines(new File(parseFile), Charsets.UTF_8);
		final int dataCount = endIndex - startIndex;

		final ExecutorService threadPool = newFixedThreadPool(numThreads);
		for (int i = 0; i < dataCount; i++) {
			final int count = i;
			threadPool.execute(new Runnable() {
				public void run() {
					logger.info("Task " + count + " : start");
					int[][][] allFeatures = processLine(frameLines.get(count), parseLines);
					final String file = String.format("%s/feats_%d.jobj.gz", eventDir, count);
					SerializedObjects.writeSerializedObject(allFeatures, file); // auto-gzips
					logger.info("Task " + count + " : end" + " alphsize:" + alphabet.size());
				}
			});
		}
		threadPool.shutdown();
	}

	private int[][] getFeatures(String frame, int[] targetTokenIdxs, String[][] allLemmaTags) {
		final THashSet<String> hiddenUnits = frameMap.get(frame);
		final DependencyParse parse = DependencyParse.processFN(allLemmaTags, 0.0);
		final int hSize = hiddenUnits.size();
		final int[][] results = new int[hSize][];
		int hCount = 0;
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
			final List<Integer> feats = new ArrayList<Integer>();
			for (String feat : valMap.keySet()) {
				int val = valMap.get(feat);
				int featIndex;
				if (alphabet.containsKey(feat)) {
					featIndex = alphabet.get(feat);
					for (int i = 0; i < val; i++) {
						feats.add(featIndex);
					}
				}
			}
			int hFeatSize = feats.size();
			results[hCount] = new int[hFeatSize];
			for (int i = 0; i < hFeatSize; i++) {
				results[hCount][i] = feats.get(i);
			}
			hCount++;
		}
		return results;
	}

	private int[][][] processLine(String frameLine, List<String> parseLines) {
		// Parse the frameLine
		final String[] toks = frameLine.split("\t");
		// throw out first two fields
		final List<String> tokens = Arrays.asList(toks).subList(2, toks.length);
		final String goldFrame = tokens.get(1);
		final String[] targetIdxsStr = tokens.get(3).split("_");
		final int[] targetTokenIdxs = new int[targetIdxsStr.length];
		for (int j = 0; j < targetIdxsStr.length; j++)
			targetTokenIdxs[j] = Integer.parseInt(targetIdxsStr[j]);
		Arrays.sort(targetTokenIdxs);
		final int sentNum = Integer.parseInt(tokens.get(5));

		// Parse the parse line
		final String[][] allLemmaTags = AllLemmaTags.readLine(parseLines.get(sentNum));

		// extract features for every frame
		final Set<String> frames = frameMap.keySet();
		final int[][][] allFeatures = new int[frames.size()][][];
		// put the correct frame first
		allFeatures[0] = getFeatures(goldFrame, targetTokenIdxs, allLemmaTags);
		int count = 1;
		for (String wrongFrame : frames) {
			if (wrongFrame.equals(goldFrame)) continue;
			allFeatures[count] = getFeatures(wrongFrame, targetTokenIdxs, allLemmaTags);
			count++;
		}
		return allFeatures;
	}
}
