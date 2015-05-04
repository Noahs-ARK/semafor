/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * DataPrep.java is part of SEMAFOR 2.0.
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

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements;
import edu.cmu.cs.lti.ark.util.FileUtil;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParses;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.*;
import java.util.*;

import static edu.cmu.cs.lti.ark.fn.parsing.CandidateFrameElementFilters.EMPTY_SPAN;
import static java.lang.Integer.parseInt;
import static org.apache.commons.io.IOUtils.closeQuietly;

@NotThreadSafe
public class DataPrep {
	/** a map from feature name to its index */
	public final Map<String, Integer> featureIndex;
	/**
	 * an array list containing candidate spans for each sentence
	 * the m'th span in the n'th sentence is
	 * candidateLines.get(n)[m][0] and candidateLines.get(n)[m][1]
	 */
	private List<List<SpanAndParseIdx>> candidateLines;
	/** contains lines in frame element file */
	public List<String> feLines;
	/** lines in tags file */
	public List<String> tagLines;
	/** index of the current line in feLines being processed */
	public int feIndex = 0;
	public final CandidateSpanPruner spanPruner;

	public static class SpanAndParseIdx {
		public final static SpanAndParseIdx EMPTY_SPAN_AND_PARSE_IDX = new SpanAndParseIdx(EMPTY_SPAN, 0);
		public final Range0Based span;
		public final int parseIdx;

		public SpanAndParseIdx(Range0Based span, int parseIdx) {
			this.span = span;
			this.parseIdx = parseIdx;
		}

		@Override
		public boolean equals(Object other) {
			if (other == null || !(other instanceof SpanAndParseIdx)) return false;
			SpanAndParseIdx o = (SpanAndParseIdx) other;
			return this.span.equals(o.span) && this.parseIdx == o.parseIdx;
		}
	}

	public DataPrep(List<String> tagLines,
					List<String> feLines,
					String spanFilename,
					Map<String, Integer> featureIndex) throws IOException {
		this.featureIndex = featureIndex;
		new FileOutputStream(new File(spanFilename), false).close(); // clobber spans file. this is gross
		this.feLines = feLines;
		this.tagLines = tagLines;
		candidateLines = load(tagLines, feLines);
		spanPruner = new CandidateSpanPruner();
	}

	/** loads data needed for feature extraction */
	private List<List<SpanAndParseIdx>> load(List<String> tagLines, List<String> frameElementLines) {
		final ArrayList<List<SpanAndParseIdx>> candidateLines = Lists.newArrayList();
		for (String feline : frameElementLines) {
			final int sentNum = parseInt(feline.split("\t")[7]);
			final String tagLine = tagLines.get(sentNum);
			final DataPointWithFrameElements dp = new DataPointWithFrameElements(tagLine, feline);
			final List<SpanAndParseIdx> spanList = spanPruner.candidateSpansAndGoldSpans(dp);
			candidateLines.add(spanList);
		}
		return candidateLines;
	}

	public boolean hasNext() {
		return feIndex < feLines.size();
	}

	public int[][][] getNextTrainData(String spanFilename) throws IOException {
		final String feline = feLines.get(feIndex);
		final List<SpanAndParseIdx> candidateTokens = candidateLines.get(feIndex);
		final int sentNum = parseInt(feline.split("\t")[7]);
		final String parseLine = tagLines.get(sentNum);
		final Sentence sentence = Sentence.fromAllLemmaTagsArray(AllLemmaTags.readLine(parseLine));
		final List<int[][]> allData = getTrainData(feline, candidateTokens, sentence, spanFilename);
		feIndex++;
		return allData.toArray(new int[allData.size()][][]);
	}

	public List<int[][]> getTrainData(String feline,
									  List<SpanAndParseIdx> candidateSpans,
									  Sentence sentence,
									  String spanFilename)
			throws IOException {
		final DataPointWithFrameElements dataPoint = new DataPointWithFrameElements(sentence, feline);
		final String frame = dataPoint.getFrameName();
		final String[] allFrameElements = FEDict.getInstance().lookupFrameElements(frame);
		final List<int[][]> featuresList = new ArrayList<int[][]>();

		final List<Pair<List<int[]>, List<String>>> allFeaturesAndSpanLines = Lists.newArrayList();
		//add realized frame elements
		final List<Range0Based> spans = dataPoint.getOvertFrameElementFillerSpans();
		final List<String> filledFrameElements = dataPoint.getOvertFilledFrameElementNames();
		for (int i = 0; i < dataPoint.getNumOvertFrameElementFillers(); i++) {
			final String frameElement = filledFrameElements.get(i);
			final Range0Based goldSpan = spans.get(i);
			allFeaturesAndSpanLines.add(
					getFeaturesForOneArgument(dataPoint, frame, frameElement, goldSpan, candidateSpans));
		}
		//add null frame elements
		final Set<String> realizedFes = Sets.newHashSet(filledFrameElements);
		for (String frameElement : allFrameElements) {
			if (!realizedFes.contains(frameElement)) {
				final Range0Based goldSpan = EMPTY_SPAN;
				allFeaturesAndSpanLines.add(
						getFeaturesForOneArgument(dataPoint, frame, frameElement, goldSpan, candidateSpans));
			}
		}
		//prints .spans file, which is later used to recover frame parse after prediction
		final PrintWriter ps = new PrintWriter(new FileWriter(spanFilename, true));
		try {
			for (Pair<List<int[]>, List<String>> featuresAndSpanLines : allFeaturesAndSpanLines) {
				final List<int[]> features = featuresAndSpanLines.first;
				final List<String> spanLines = featuresAndSpanLines.second;
				for (String spanLine : spanLines) ps.println(spanLine);
				featuresList.add(features.toArray(new int[features.size()][]));
			}
		} finally {
			closeQuietly(ps);
		}
		return featuresList;
	}

	Pair<List<int[]>, List<String>> getFeaturesForOneArgument(DataPointWithFrameElements dp,
															  String frame,
															  String fe,
															  Range0Based goldSpan,
															  List<SpanAndParseIdx> candidateSpans) {
		final List<int[]> features = Lists.newArrayList();
		final List<String> spanLines = Lists.newArrayList();
		spanLines.add(Joiner.on("\t").join(
				dp.getSentenceNum(), fe, frame, dp.getTargetTokenIdxs()[0],
				dp.getTargetTokenIdxs()[dp.getTargetTokenIdxs().length-1], feIndex));
		// put gold span first
		final List<SpanAndParseIdx> goldFirst =
				Lists.newArrayListWithCapacity(candidateSpans.size());
		for (SpanAndParseIdx candidateSpanAndParseIdx : candidateSpans) {
			if (candidateSpanAndParseIdx.span.equals(goldSpan)) {
				goldFirst.add(0, candidateSpanAndParseIdx);
			} else {
				goldFirst.add(candidateSpanAndParseIdx);
			}
		}
		// add features for candidate spans
		final DependencyParses parses = dp.getParses();
		for (SpanAndParseIdx candidateSpanAndParseIdx : goldFirst) {
			final Range0Based candidateSpan = candidateSpanAndParseIdx.span;
			final DependencyParse parse = parses.get(candidateSpanAndParseIdx.parseIdx);
			features.add(getFeatureIndexes(dp, frame, fe, candidateSpan, parse));
			spanLines.add(candidateSpan.start + "\t" + candidateSpan.end);
		}
		spanLines.add("");
		return Pair.of(features, spanLines);
	}

	/** Gets the indexes of all features that fire */
	int[] getFeatureIndexes(DataPointWithFrameElements dataPoint,
							String frame,
							String fe,
							Range0Based span,
							DependencyParse parse) {
		final Set<String> featureSet =
				new FeatureExtractor().extractFeatures(dataPoint, frame, fe, span, parse).elementSet();
		final int[] featArray = new int[featureSet.size()];
		int i = 0;
		for (String feature : featureSet) {
			featArray[i] = getIndexOfFeature(feature);
			i++;
		}
		return featArray;
	}

	public static Map<String, Integer> readFeatureIndex(File alphabetFile) throws FileNotFoundException {
                System.out.println("alphabet file " + alphabetFile.getAbsolutePath());
		HashMap<String, Integer> featureIndex = Maps.newHashMap();
		final FileInputStream inputStream = new FileInputStream(alphabetFile);
		Scanner scanner = new Scanner(inputStream);
		try {
			// skip the first line
			scanner.nextLine();
			int count = 0;
			while (scanner.hasNextLine()) {
				addFeature(scanner.nextLine(), featureIndex);
				if (count % 100000 == 0) {
					System.err.print(count + " ");
				}
				count++;
			}
			System.err.println();
		} finally {
			closeQuietly(inputStream);
		}
		return featureIndex;
	}

	// writes a features to file
	public void writeFeatureIndex(String alphabetFilename) {
		int numFeatures = featureIndex.size();
		PrintStream printStream = FileUtil.openOutFile(alphabetFilename);
		printStream.println(numFeatures);
		String buf[] = new String[numFeatures + 1];
		for (String feature : featureIndex.keySet()) {
			buf[featureIndex.get(feature)] = feature;
		}
		for (int i = 1; i <= numFeatures; i++) {
			printStream.println(buf[i]);
		}
		printStream.close();
	}

	/**
	 * Look up the index of feature in our map
	 * If it doesn't exist, add it to our map
	 *
	 * @param feature the feature to look up
	 * @return the index of feature
	 */
	public int getIndexOfFeature(String feature) {
		Optional<Integer> idx = Optional.fromNullable(featureIndex.get(feature));
		if (idx.isPresent()) return idx.get();
		addFeature(feature, featureIndex);
		return featureIndex.size();
	}

	public static void addFeature(String key, Map<String, Integer> freqmap) {
		if(!freqmap.containsKey(key)) {
			final int numFeatures = freqmap.size();
			freqmap.put(key, numFeatures + 1);
		}
	}
}
