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
import com.google.common.collect.*;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements;
import edu.cmu.cs.lti.ark.util.FileUtil;
import edu.cmu.cs.lti.ark.util.ds.*;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParses;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.*;
import java.util.*;

import static edu.cmu.cs.lti.ark.fn.parsing.CandidateFrameElementFilters.EMPTY_SPAN;
import static edu.cmu.cs.lti.ark.fn.parsing.CandidateFrameElementFilters.createSpanRange;
import static edu.cmu.cs.lti.ark.fn.parsing.DataPrep.SpanAndParseIdx.EMPTY_SPAN_AND_PARSE_IDX;
import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;
import static java.lang.Integer.parseInt;
import static org.apache.commons.io.IOUtils.closeQuietly;

@NotThreadSafe
public class DataPrep {
	public static final ImmutableSet<String> NON_BREAKING_LEFT_CONSTITUENT_POS = ImmutableSet.of("DT", "JJ");
	/** a map from feature name to its index */
	public static Map<String, Integer> featureIndex;
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
	/** is it generating an alphabet or using an alphabet */
	public static boolean genAlpha = true; // TODO: shouldn't be static

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

	public DataPrep(List<String> tagLines, List<String> feLines) throws IOException {
		new FileOutputStream(new File(FEFileName.spanfilename), false).close(); // clobber spans file. this is gross
		this.feLines = feLines;
		this.tagLines = tagLines;
		final File candidateFile = new File(FEFileName.candidateFilename);
		if (candidateFile.exists()) {
			candidateLines = loadFromCandidateFile(feLines, Files.newInputStreamSupplier(candidateFile));
		} else {
			candidateLines = load(tagLines, feLines);
		}
	}

	/** Finds a set of candidate spans based on a dependency parse */
	public static List<SpanAndParseIdx> findSpans(DataPointWithFrameElements dataPoint, int kBestParses) {
		final DependencyParses parses = dataPoint.getParses();
		final DependencyParse bestParse = parses.getBestParse();
		final DependencyParse[] nodes = bestParse.getIndexSortedListOfNodes();
		// nodes includes a dummy head node
		final int length = nodes.length - 1;
		// indexes a set of spans by [start][end]
		boolean[][] spanMatrix = new boolean[length][length];  // TODO(smt): why? we just want a set here, right?
		// index from [start][end] to which parse the span came from
		final int[][] depParses = new int[length][length];
		// index from [start][end] to the index of the head of the span
		final int[][] heads = new int[length][length];
		for(int j : xrange(length)) {
			for(int k : xrange(length)) {
				heads[j][k] = -1;
			}
		}
		for(Range0Based span : dataPoint.getOvertFrameElementFillerSpans()) {
			spanMatrix[span.start][span.end] = true;
		}
		if(kBestParses > 1) {
			addKBestParses(dataPoint, depParses);
		}
		addConstituents(spanMatrix, heads, nodes);
		final ArrayList<SpanAndParseIdx> spanList = Lists.newArrayList();
		for (int i : xrange(length)) {
			for (int j : xrange(length)) {
				if (spanMatrix[i][j]) {
					final int parseIdx = depParses[i][j];
					spanList.add(new SpanAndParseIdx(createSpanRange(i, j), parseIdx));
				}
			}
		}
		// null span is always a candidate
		spanList.add(EMPTY_SPAN_AND_PARSE_IDX);
		return spanList;
	}

	/** loads data needed for feature extraction */
	private List<List<SpanAndParseIdx>> load(List<String> tagLines, List<String> frameElementLines) {
		final ArrayList<List<SpanAndParseIdx>> candidateLines = Lists.newArrayList();
		for (String feline : frameElementLines) {
			final int sentNum = parseInt(feline.split("\t")[7]);
			final String tagLine = tagLines.get(sentNum);
			final DataPointWithFrameElements dp = new DataPointWithFrameElements(tagLine, feline);
			final List<SpanAndParseIdx> spanList = findSpans(dp, FEFileName.KBestParse);
			candidateLines.add(spanList);
		}
		return candidateLines;
	}

	private List<List<SpanAndParseIdx>> loadFromCandidateFile(List<String> frameElementLines,
													 InputSupplier<? extends InputStream> candidateInputSupplier)
			throws IOException {
		final List<List<SpanAndParseIdx>> candidateLines = Lists.newArrayList();
		final Scanner canScanner = new Scanner(candidateInputSupplier.getInput());
		for (String ignored : frameElementLines) {
			final List<SpanAndParseIdx> spanList = Lists.newArrayList();
			final String spanTokens[] = canScanner.nextLine().trim().split("\t|:");
			for (int i = 0; i < spanTokens.length; i += 2) {
				final Range0Based span = createSpanRange(parseInt(spanTokens[i]), parseInt(spanTokens[i + 1]));
				spanList.add(new SpanAndParseIdx(span, 0));
			}
			//add null span to candidates
			spanList.add(EMPTY_SPAN_AND_PARSE_IDX);
			candidateLines.add(spanList);
		}
		return candidateLines;
	}

	/**
	 * Adds parses from goldDP to depParses
	 *
	 * @param goldDP the DataPointWithElements whose parses to add to depParses
	 * @param depParses the array to add parses to
	 */
	private static void addKBestParses(DataPointWithFrameElements goldDP, int[][] depParses) {
		final List<Range0Based> spans = goldDP.getOvertFrameElementFillerSpans();
		final DependencyParses parses = goldDP.getParses();
		for (Range0Based span : spans) {
			Range1Based span1 = new Range1Based(span);
			Optional<Pair<Integer, DependencyParse>> indexAndParse = parses.matchesSomeConstituent(span1);
			if (indexAndParse.isPresent()) {
				int fIndex = indexAndParse.get().first;
				depParses[span.start][span.end] = fIndex;
			} else {
				depParses[span.start][span.end] = 0;
			}
		}
	}

	public boolean hasNext() {
		return feIndex < feLines.size();
	}

	public static void addFeature(String key, Map<String, Integer> freqmap) {
		if(!freqmap.containsKey(key)) {
			final int numFeatures = freqmap.size();
			freqmap.put(key, numFeatures + 1);
		}
	}

	public int[][][] getNextTrainData() throws IOException {
		final String feline = feLines.get(feIndex);
		final List<SpanAndParseIdx> candidateTokens = candidateLines.get(feIndex);
		final int sentNum = parseInt(feline.split("\t")[7]);
		final String parseLine = tagLines.get(sentNum);
		final Sentence sentence = Sentence.fromAllLemmaTagsArray(AllLemmaTags.readLine(parseLine));
		final List<int[][]> allData = getTrainData(feline, candidateTokens, sentence);
		feIndex++;
		return allData.toArray(new int[allData.size()][][]);
	}

	public List<int[][]> getTrainData(String feline, List<SpanAndParseIdx> candidateTokens, Sentence sentence)
			throws IOException {
		final DataPointWithFrameElements dataPoint = new DataPointWithFrameElements(sentence, feline);
		final String frame = dataPoint.getFrameName();
		final String[] frameElements = FEDict.getInstance().lookupFrameElements(frame);
		final List<int[][]> featuresList = new ArrayList<int[][]>();

		final List<Pair<List<int[]>, List<String>>> allFeaturesAndSpanLines = Lists.newArrayList();
		//add realized frame elements
		final List<Range0Based> spans = dataPoint.getOvertFrameElementFillerSpans();
		final List<String> frameElementNames = dataPoint.getOvertFilledFrameElementNames();
		final Set<String> realizedFes = Sets.newHashSet();
		for (int i = 0; i < dataPoint.getNumOvertFrameElementFillers(); i++) {
			final String frameElement = frameElementNames.get(i);
			if (!realizedFes.contains(frameElement)) {
				realizedFes.add(frameElement);
				final Range0Based span = spans.get(i);
				allFeaturesAndSpanLines.add(
						getFeaturesForOneArgument(dataPoint, frame, frameElement, span, candidateTokens));
			}
		}
		//add null frame elements
		for (String frameElement : frameElements) {
			if (!realizedFes.contains(frameElement)) {
				final Range0Based span = EMPTY_SPAN;
				allFeaturesAndSpanLines.add(
						getFeaturesForOneArgument(dataPoint, frame, frameElement, span, candidateTokens));
			}
		}
		//prints .spans file, which is later used to recover frame parse after prediction
		final PrintWriter ps = new PrintWriter(new FileWriter(FEFileName.spanfilename, true));
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

	/**
	 * @param candidateSpanAndParseIdxs are of the form [start, end, dependencyParseIdx]
	 */
	Pair<List<int[]>, List<String>> getFeaturesForOneArgument(DataPointWithFrameElements dp,
															  String frame,
															  String fe,
															  Range0Based goldSpan,
															  List<SpanAndParseIdx> candidateSpanAndParseIdxs) {
		final List<int[]> features = Lists.newArrayList();
		final List<String> spanLines = Lists.newArrayList();
		spanLines.add(Joiner.on("\t").join(
				dp.getSentenceNum(), fe, frame, dp.getTargetTokenIdxs()[0],
				dp.getTargetTokenIdxs()[dp.getTargetTokenIdxs().length-1], feIndex));
		// put gold span first
		final List<SpanAndParseIdx> goldFirst = Lists.newArrayList();
		for (SpanAndParseIdx candidateSpanAndParseIdx : candidateSpanAndParseIdxs) {
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
			features.add(getFeaturesByIndex(dp, frame, fe, candidateSpan, parse));
			spanLines.add(candidateSpan.start + "\t" + candidateSpan.end);
		}
		spanLines.add("");
		return Pair.of(features, spanLines);
	}

	int[] getFeaturesByIndex(DataPointWithFrameElements dataPoint,
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
	public static void writeFeatureIndex(String alphabetFilename) {
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
	 * Calculates an array of constituent spans based on the given dependency parse.
	 * A constituent span is a token and all of its descendants.
	 * Adds the constituents to spanMatrix and heads
	 *
	 * @param spanMatrix an index from [start][end] -> isConstituent(start, end)
	 * @param heads an index from [start][end] -> headOfSpan(start, end)
	 * @param nodes the dependency parse
	 */
	private static void addConstituents(boolean[][] spanMatrix, int[][] heads, DependencyParse[] nodes) {
		final int length = nodes.length - 1;
		// left[i] is the index of the left-most descendant of i
		int left[] = new int[length];
		// right[i] is the index of the right-most descendant of i
		int right[] = new int[length];
		// translate parent indices from 1-based to 0-based
		int[] parent = new int[length];
		for (int i : xrange(length)) {
			parent[i] = (nodes[i+1].getParentIndex() - 1);
			left[i] = i;
			right[i] = i;
		}

		// for each node i, expand i's ancestors' spans to include i
		// NB: assumes children are contiguous (always true of projective parses)
		for (int i : xrange(length)) {
			int parentIndex = parent[i];
			while (parentIndex >= 0) {
				if (left[parentIndex] > i) {
					left[parentIndex] = i;
				} else if (right[parentIndex] < i) {
					right[parentIndex] = i;
				}
				parentIndex = parent[parentIndex];
			}
		}
		for (int i : xrange(length)) {
			spanMatrix[left[i]][right[i]] = true;
			heads[left[i]][right[i]] = i;
		}
		
		// single words are always constituents
		for (int i : xrange(length)) {
			spanMatrix[i][i] = true;
			heads[i][i] = i;
		}

		// heuristics to try to recover finer-grained constituents when a node has multiple descendants
		for (int i : xrange(length)) {
			if(!(left[i] < i && right[i] > i)) continue;
			//left
			int justLeft = i - 1;
			if(justLeft >= 0) {
				if(spanMatrix[left[i]][justLeft]) {
					if (!(justLeft == left[i] && NON_BREAKING_LEFT_CONSTITUENT_POS.contains(nodes[justLeft+1].getPOS()))) {
						spanMatrix[i][right[i]] = true;
						heads[i][right[i]] = i;
					}
				}
			}
			//right
			int justRight = i + 1;
			if(justRight <= length - 1) {
				if(spanMatrix[justRight][right[i]]) {
					spanMatrix[left[i]][i] = true;
					heads[left[i]][i] = i;
				}
			}
		}
	}

	/**
	 * Look up the index of feature in our map
	 * If it doesn't exist and genAlpha is true, add it to our map
	 *
	 * @param feature the feature to look up
	 * @return the index of feature
	 */
	public int getIndexOfFeature(String feature) {
		Integer idx = featureIndex.get(feature);
		if (idx != null) return idx;
		if (genAlpha) {
			addFeature(feature, featureIndex);
			return featureIndex.size();
		}
		return 0;
	}
}
