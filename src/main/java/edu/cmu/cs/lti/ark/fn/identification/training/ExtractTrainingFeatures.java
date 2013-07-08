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
package edu.cmu.cs.lti.ark.fn.identification.training;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.identification.FrameCosts;
import edu.cmu.cs.lti.ark.fn.identification.IdFeatureExtractor;
import edu.cmu.cs.lti.ark.fn.identification.RequiredDataForFrameIdentification;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntDoubleHashMap;

import java.io.File;
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

import static edu.cmu.cs.lti.ark.fn.identification.training.AlphabetCreationThreaded.readAlphabetFile;
import static edu.cmu.cs.lti.ark.fn.identification.training.TrainBatch.FEATURE_FILENAME_PREFIX;
import static edu.cmu.cs.lti.ark.fn.identification.training.TrainBatch.FEATURE_FILENAME_SUFFIX;
import static edu.cmu.cs.lti.ark.util.SerializedObjects.writeSerializedObject;
import static java.util.concurrent.Executors.newFixedThreadPool;

public class ExtractTrainingFeatures {
	private static final Logger logger = Logger.getLogger(ExtractTrainingFeatures.class.getCanonicalName());

	private final THashMap<String, THashSet<String>> frameMap;
	private final String parseFile;
	private final File eventDir;
	private final String frameElementsFile;
	private final Map<String, Integer> alphabet;
	private final FrameCosts costs;
	private final int startIndex;
	private final int endIndex;
	private final int numThreads;
	private final IdFeatureExtractor featureExtractor;

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
		final String featureExtractorType =
				options.idFeatureExtractorType.present() ?
						options.idFeatureExtractorType.get() :
						"basic";
		final IdFeatureExtractor featureExtractor = IdFeatureExtractor.fromName(featureExtractorType);
		logger.info("Reading alphabet");
		final Map<String, Integer> alphabet = readAlphabetFile(new File(options.modelFile.get()));
		logger.info("Done reading alphabet");
		final ExtractTrainingFeatures events =
				new ExtractTrainingFeatures(alphabet,
						new File(options.eventsFile.get()),
						options.trainFrameElementFile.get(),
						options.trainParseFile.get(),
						r.getFrameMap(),
						FrameCosts.load(),
						startIndex,
						endIndex,
						options.numThreads.get(),
						featureExtractor);
		events.createEvents();
	}

	public ExtractTrainingFeatures(Map<String, Integer> alphabet,
								   File eventDir,
								   String frameElementsFile,
								   String parseFile,
								   THashMap<String, THashSet<String>> frameMap,
								   FrameCosts costs, int startIndex,
								   int endIndex,
								   int numThreads,
								   IdFeatureExtractor featureExtractor) {
		this.alphabet = alphabet;
		this.frameMap = frameMap;
		this.parseFile = parseFile;
		this.frameElementsFile = frameElementsFile;
		this.eventDir = eventDir;
		this.costs = costs;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.numThreads = numThreads;
		this.featureExtractor = featureExtractor;
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
					logger.info(String.format("Task %d : start", count));
					FeaturesAndCost[] allFeatures = processLine(frameLines.get(count), parseLines);
					final String filename =
							String.format("%s%06d%s", FEATURE_FILENAME_PREFIX, count, FEATURE_FILENAME_SUFFIX);
					writeSerializedObject(allFeatures, new File(eventDir, filename).getAbsolutePath()); // auto-gzips
					logger.info(String.format("Task %d : end", count));
				}
			});
		}
		threadPool.shutdown();
	}

	private FeaturesAndCost[] processLine(String frameLine, List<String> parseLines) {
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

		final Sentence sentence = Sentence.fromAllLemmaTagsArray(AllLemmaTags.readLine(parseLines.get(sentNum)));

		// extract features for every frame
		final Set<String> frames = frameMap.keySet();
		final FeaturesAndCost[] allFeatures = new FeaturesAndCost[frames.size()];
		final Map<String, TIntDoubleHashMap> allFeaturesMap = featureExtractor.extractFeaturesByIndex(
				frames,
				targetTokenIdxs,
				sentence,
				alphabet
		);
		// put the correct frame first
		allFeatures[0] = new FeaturesAndCost(allFeaturesMap.get(goldFrame), 0f);
		int count = 1;
		for (String frame : frames) {
			if (frame.equals(goldFrame)) continue;
			allFeatures[count] = new FeaturesAndCost(allFeaturesMap.get(frame), costs.getCost(goldFrame, frame));
			count++;
		}
		return allFeatures;
	}
}
