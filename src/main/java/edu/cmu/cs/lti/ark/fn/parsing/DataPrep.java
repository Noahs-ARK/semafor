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

import com.google.common.base.Optional;
import com.google.common.collect.*;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.FileUtil;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;
import edu.cmu.cs.lti.ark.util.ds.Range1Based;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParses;

import java.io.*;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;
import static java.lang.Integer.parseInt;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.readLines;

public class DataPrep {
	/**
	 * an array list containing candidate spans for each sentence
	 * the m'th span in the n'th sentence is 
	 * candidateLines.get(n)[m][0] and candidateLines.get(n)[m][1]
	 */
	public ArrayList<int[][]> candidateLines;
	/**
	 * a hash map from a frame name to its
	 * frame elements.
	 */
	public static FEDict frameElementsForFrame;
	public static WordNetRelations wordNetRelations;
	/**
	 * contains lines in frame element file
	 */
	public static List<String> feLines;
	/**
	 * lines in tags file
	 */
	public static List<String> tagLines;
	/**
	 * index of the current line in feLines being processed
	 */
	public int feIndex;
	/**
	 * total number of features
	 */
	public static int numFeatures = 0;
	/**
	 * a map from feature name to its index
	 */
	public static HashMap<String, Integer> featureIndex;
	/**
	 * prints .spans file , which is later used to recover 
	 * frame parse after prediction
	 */
	private PrintWriter ps;

	public static final ImmutableSet<String> NON_BREAKING_LEFT_CONSTITUENT_POS = ImmutableSet.of("DT", "JJ");

	/**
	 * is it generating an alphabet or using an alphabet
	 */
	public static boolean genAlpha = true;

	public static boolean useOracleSpans = false;
	
	public DataPrep() throws IOException {
		new File(FEFileName.spanfilename).delete(); // this is gross
		tagLines = readLines(new FileInputStream(FEFileName.tagFilename));
		load(tagLines, null, null);
	}

	public DataPrep(List<String> tagLines,
					List<String> frameElementLines,
					WordNetRelations lwnr) throws IOException {
		new File(FEFileName.spanfilename).delete(); // this is gross
		load(tagLines, frameElementLines, lwnr);
	}

	public DataPrep(Sentence sentence, String frameElements, WordNetRelations lwnr) throws IOException {
		new File(FEFileName.spanfilename).delete(); // this is gross
		getDataPoints(sentence, frameElements, lwnr);
	}

	/**
	 * Finds a set of candidate spans based on a dependency parse
	 *
	 * @param dataPoint
	 * @param useOracleSpans
	 * @param kBestParses
	 * @return
	 */
	public static ArrayList<int[]> findSpans(DataPointWithFrameElements dataPoint,
											 boolean useOracleSpans,
											 int kBestParses) {
		final DependencyParse bestParse = dataPoint.getParses().getBestParse();
		final DependencyParse[] nodes = bestParse.getIndexSortedListOfNodes();
		// nodes includes a dummy head node
		final int length = nodes.length - 1;
		// indexes a set of spans by [start][end]
		boolean[][] spanMatrix = new boolean[length][length];
		// index from [start][end] to which parse the span came from
		final int[][] depParses = new int[length][length];
		// index from [start][end] to the index of the head of the span
		final int[][] heads = new int[length][length];
		for(int j : xrange(length)) {
			for(int k : xrange(length)) {
				heads[j][k] = -1;
			}
		}
		spanMatrix = addGoldSpan(dataPoint, spanMatrix);
		if(kBestParses > 1) {
			addKBestParses(dataPoint, depParses);
		}
		if(!useOracleSpans) {
			addConstituents(spanMatrix, heads, nodes);
		}
		ArrayList<int[]> spanList = new ArrayList<int[]>();
		for (int i : xrange(length)) {
			for (int j : xrange(length)) {
				if (spanMatrix[i][j]) {
					spanList.add(new int[] {i, j, depParses[i][j]});
				}
			}
		}
		// null span is always a candidate
		spanList.add(new int[]{-1, -1, 0});
		return spanList;
	}

	/**
	 * loads data needed for feature extraction
	 */
	private void load(List<String> tagLines, List<String> frameElementLines, WordNetRelations lwnr) throws IOException {
		// null values fall back to defaults
		if (tagLines == null) tagLines = readLines(new FileInputStream(FEFileName.tagFilename));
		if (frameElementLines == null) frameElementLines = readLines(new FileInputStream(FEFileName.feFilename));

		if (frameElementsForFrame == null) frameElementsForFrame = new FEDict(FEFileName.feDictFilename);
		if (wordNetRelations == null) {
			if (lwnr == null) lwnr = new WordNetRelations(FEFileName.stopwordFilename, FEFileName.wordnetFilename);
			wordNetRelations = lwnr;
		}

		candidateLines = new ArrayList<int[][]>();
		feLines = frameElementLines;
		DataPrep.tagLines = tagLines;

		Scanner canScanner = FileUtil.openInFile(FEFileName.candidateFilename);
		final boolean hasCandidateFile = (canScanner != null);

		System.err.println("Loading data....");
		for (String feline : feLines) {
			final int sentNum = parseInt(feline.split("\t")[7]);
			DataPointWithFrameElements dp = new DataPointWithFrameElements(tagLines.get(sentNum), feline);
			ArrayList<int[]> spanList;
			if (hasCandidateFile) {
				spanList = Lists.newArrayList();
				String spanTokens[] = canScanner.nextLine().trim().split("\t|:");
				for (int i = 0; i < spanTokens.length; i += 2) {
					spanList.add(new int[]{parseInt(spanTokens[i]), parseInt(spanTokens[i+1])});
				}
				//add null span to candidates
				spanList.add(new int[]{-1, -1, 0});
			}
			else {
				spanList = findSpans(dp, useOracleSpans, FEFileName.KBestParse);
			}
			candidateLines.add(spanList.toArray(new int[spanList.size()][]));
		}
		feIndex = 0;
	}

	public ArrayList<int[][]> getDataPoints(Sentence sentence, String feline, WordNetRelations lwnr) throws IOException {
		checkNotNull(sentence);
		checkNotNull(feline);
		checkNotNull(lwnr);
		if (frameElementsForFrame == null) frameElementsForFrame = new FEDict(FEFileName.feDictFilename);
		if (wordNetRelations == null) wordNetRelations = lwnr;

		System.err.println("Loading data....");
		final DataPointWithFrameElements dataPointWithElements = new DataPointWithFrameElements(sentence, feline);
		final ArrayList<int[]> spanList = findSpans(dataPointWithElements, useOracleSpans, FEFileName.KBestParse);
		return getTrainData(feline, spanList.toArray(new int[spanList.size()][]), sentence);
	}

	/**
	 * Adds frame element filler spans from goldDP to spanMatrix.
	 * Modifies spanMatrix in place, and also returns it.
	 *
	 * @param goldDP the data point whose spans to add
	 * @param spanMatrix the 2d boolean matrix to add spans to
	 */
	private static boolean[][] addGoldSpan(DataPointWithFrameElements goldDP, boolean[][] spanMatrix) {
		List<Range0Based> spans = goldDP.getOvertFrameElementFillerSpans();
		for(Range0Based span : spans) {
			spanMatrix[span.getStart()][span.getEnd()] = true;
		}
		return spanMatrix;
	}


	/**
	 * Adds parses from goldDP to depParses
	 *
	 * @param goldDP the DataPointWithElements whose parses to add to depParses
	 * @param depParses the array to add parses to
	 */
	private static void addKBestParses(DataPointWithFrameElements goldDP, int[][] depParses) {
		List<Range0Based> spans = goldDP.getOvertFrameElementFillerSpans();
		DependencyParses parses = goldDP.getParses();
		for (Range0Based span : spans) {
			Range1Based span1 = new Range1Based(span);
			Optional<Pair<Integer, DependencyParse>> indexAndParse = parses.matchesSomeConstituent(span1);
			if (!indexAndParse.isPresent()) {
				depParses[span.getStart()][span.getEnd()] = 0;
			} else {
				int fIndex = indexAndParse.get().getFirst();
				depParses[span.getStart()][span.getEnd()] = fIndex;
			}
		}
	}

	public boolean hasNext() {
		return feIndex < feLines.size();
	}

	public static void addFeature(String key, HashMap<String, Integer> freqmap) {
		if(!freqmap.containsKey(key)) {
			freqmap.put(key, numFeatures + 1);
			numFeatures++;
		}
	}

	public int[][][] getNextTrainData() throws IOException {
		final String feline = feLines.get(feIndex);
		final int candidateTokens[][] = candidateLines.get(feIndex);
		final int sentNum = parseInt(feline.split("\t")[7]);
		final String parseLine = tagLines.get(sentNum);
		final Sentence sentence = Sentence.fromAllLemmaTagsArray(AllLemmaTags.readLine(parseLine));
		final ArrayList<int[][]> allData = getTrainData(feline, candidateTokens, sentence);
		feIndex++;
		return allData.toArray(new int[allData.size()][][]);
	}

	private ArrayList<int[][]> getTrainData(String feline, int[][] candidateTokens, Sentence sentence) throws IOException {
		final DataPointWithFrameElements goldDP = new DataPointWithFrameElements(sentence, feline);
		final String frame = goldDP.getFrameName();
		final String[] canArgs = frameElementsForFrame.lookupFrameElements(frame);
		final ArrayList<int[][]> dataPointList = new ArrayList<int[][]>();
		final List<Range0Based> spans = goldDP.getOvertFrameElementFillerSpans();

		//add realized frame elements
		final String frameElementNames[] = goldDP.getOvertFilledFrameElementNames();
		final HashSet<String> realizedFes = Sets.newHashSet();
		ps = new PrintWriter(new FileWriter(FEFileName.spanfilename, true));
		try {
			for (int i = 0; i < goldDP.getNumOvertFrameElementFillers(); i++) {
				if(realizedFes.contains(frameElementNames[i]))
					continue;
				realizedFes.add(frameElementNames[i]);
				addFeatureForOneArgument(goldDP, frame, frameElementNames[i], spans.get(i), dataPointList, candidateTokens);
			}
			//add null frame elements
			if (canArgs != null) {
				for (String frameElements : canArgs) {
					if (!realizedFes.contains(frameElements)) {
						addFeatureForOneArgument(goldDP, frame, frameElements,
								CandidateFrameElementFilters.EMPTY_SPAN,
								dataPointList, candidateTokens);
					}
				}
			}
		} finally {
			closeQuietly(ps);
		}
		return dataPointList;
	}

	private void addFeatureForOneArgument(DataPointWithFrameElements goldDP,
										  String frame,
										  String fe,
										  Range0Based span,
										  ArrayList<int[][]> dataPoints,
										  int candidateTokens[][]) {
		Set<String> featureSet;
		ArrayList<int[]> dataPoint = new ArrayList<int[]>();
		// add feature for gold standard
		int parseId = 0;
		for (int[] candidateToken : candidateTokens) {
			if (candidateToken[0] == span.getStart()
					&& candidateToken[1] == span.getEnd()) {
				parseId = candidateToken[2];
			}
		}
		DependencyParse selectedParse = goldDP.getParses().get(parseId);
		featureSet = FeatureExtractor.extractFeatures(goldDP, frame, fe, span, wordNetRelations, selectedParse).keySet();
		ps.println(goldDP.getSentenceNum() + "\t" + fe + "\t" + frame + "\t"
				+ goldDP.getTokenNums()[0] + "\t"
				+ goldDP.getTokenNums()[goldDP.getTokenNums().length - 1]
				+ "\t" + feIndex);
		ps.println(span.getStart() + "\t" + span.getEnd());
		int[] featArray = new int[featureSet.size()];
		int featCount = 0;
		for (String feature : featureSet) {
			featArray[featCount] = getIndexOfFeature(feature);
			featCount++;
		}
		dataPoint.add(featArray);
		// add features for candidate spans
		for (int[] candidateToken : candidateTokens) {
			// make sure does not print gold span again
			if (candidateToken[0] == span.getStart() && candidateToken[1] == span.getEnd())
				continue;

			final Range0Based candidateSpan =
					CandidateFrameElementFilters.createSpanRange(candidateToken[0], candidateToken[1]);
			selectedParse = goldDP.getParses().get(candidateToken[2]);
			ps.println(candidateSpan.getStart() + "\t" + candidateSpan.getEnd());
			featureSet = FeatureExtractor.extractFeatures(goldDP, frame, fe,
					candidateSpan, wordNetRelations, selectedParse).keySet();
			featArray = new int[featureSet.size()];
			featCount = 0;
			for (String feature : featureSet) {
				featArray[featCount] = getIndexOfFeature(feature);
				featCount++;
			}
			dataPoint.add(featArray);
		}
		ps.println();
		dataPoints.add(dataPoint.toArray(new int[dataPoint.size()][]));
	}

	// loads a list of features into a hash
	public static void loadFeatureIndex(String alphabetFilename) throws FileNotFoundException {
		featureIndex = readFeatureIndex(new File(alphabetFilename));
		genAlpha = false;
	}

	public static HashMap<String, Integer> readFeatureIndex(File alphabetFile) throws FileNotFoundException {
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

	// writes a list of features into a hash
	public static void writeFeatureIndex(String alphabetFilename) {
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
	 * Calculates the list of constituents spans based on the given dependency parse.
	 * A constituent span is a token and all of its descendants.
	 * Ranges are 0-based, and include both endpoints.
	 *
	 * @param parse the dependency parse
	 * @return the list of constituents in parse. results.get(i) is the span headed by token i.
	 */
	public static List<Range0Based> getConstituents(DependencyParse parse) {
		final DependencyParse[] nodes = parse.getIndexSortedListOfNodes();
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
		for (int i : xrange(length)) {
			int parentIndex = parent[i];
			while (parentIndex >= 0) {
				if (left[parentIndex] > i) {
					left[parentIndex] = i;
				}
				if (right[parentIndex] < i) {
					right[parentIndex] = i;
				}
				parentIndex = parent[parentIndex];
			}
		}
		ImmutableList.Builder<Range0Based> results = ImmutableList.builder();
		for(int i : xrange(length)) {
			results.add(new Range0Based(left[i], right[i]));
		}
		return results.build();
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
			return numFeatures;
		}
		return 0;
	}
}
