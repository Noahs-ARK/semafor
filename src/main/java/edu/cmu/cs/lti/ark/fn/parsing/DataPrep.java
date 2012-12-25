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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithElements;
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
	public static FEDict fedict;
	public static WordNetRelations wnr;
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
	public static int numFeat = 0;
	/**
	 * a map from feature name to its index
	 */
	public static HashMap<String, Integer> featIndex;
	/**
	 * prints .spans file , which is later used to recover 
	 * frame parse after prediction
	 */
	private final PrintStream ps;

	/**
	 * is it generating an alphabet or using an alphabet
	 */
	public static boolean genAlpha = true;

	
	public static boolean useOracleSpans = false;
	
	public DataPrep() throws IOException {
		ps = FileUtil.openOutFile(FEFileName.spanfilename);
		tagLines = readLines(new FileInputStream(FEFileName.tagFilename));
		load(tagLines, null, null);
	}

	public DataPrep(List<String> tagLines,
					List<String> frameElementLines,
					WordNetRelations lwnr) throws IOException {
		ps = FileUtil.openOutFile(FEFileName.spanfilename);
		load(tagLines, frameElementLines, lwnr);
	}

	public DataPrep(Sentence sentence, String frameElements, WordNetRelations lwnr) throws IOException {
		ps = FileUtil.openOutFile(FEFileName.spanfilename);
		getDataPoints(sentence, frameElements, lwnr);
	}

	private ArrayList<int[]> addConstituent(DataPointWithElements dataPoint){
		final DependencyParse parse = dataPoint.getParses().getBestParse();
		final DependencyParse[] nodes = parse.getIndexSortedListOfNodes();
		final int length = nodes.length - 1;
		// initialize the matrices
		boolean[][] spanMat = new boolean[length][length];
		final int[][] heads = new int[length][length];
		final int[][] depParses = new int[length][length];
		for(int j : xrange(length)) {
			for(int k : xrange(length)) {
				heads[j][k] = -1;
			}
		}
		spanMat = addGoldSpan(dataPoint, spanMat);
		if(FEFileName.KBestParse > 1) {
			addKBestParses(dataPoint, depParses);
		}
		if(!useOracleSpans) {
			findSpans(spanMat, heads, nodes);
		}
		ArrayList<int[]> spanList = new ArrayList<int[]>();
		for (int i : xrange(length)) {
			for (int j : xrange(length)) {
				if (spanMat[i][j]) {
					spanList.add(new int[] {i, j, depParses[i][j]});
				}
			}
		}
		return spanList;
	}
	
	public int findParse(DependencyParses parses, int j, int k, int hIndex) {
		hIndex = hIndex + j;
		int start = 0;
		int ret = 0;
		Range1Based span = new Range1Based(j, k);
		while(true) {
			Pair<Integer,DependencyParse> p = parses.matchesSomeConstituent(span, start);
			if(p == null) {
				ret = 0;
				break;
			}
			int fIndex = p.getSecond().getIndex();
			if(fIndex == hIndex) {
				if(p.getFirst()!=0)
					System.out.println("Found correct parse:"+p.getFirst());
				return p.getFirst();				
			}
			else {
				start=p.getFirst()+1;
				if(start>=parses.size())
					break;
			}
		}
		return ret;
	}


	/**
	 * loads data needed for feature extraction
	 */
	private void load(List<String> tagLines, List<String> frameElementLines, WordNetRelations lwnr) throws IOException {
		// null values fall back to defaults
		if (tagLines == null) tagLines = readLines(new FileInputStream(FEFileName.tagFilename));
		if (frameElementLines == null) frameElementLines = readLines(new FileInputStream(FEFileName.feFilename));

		if (fedict == null) fedict = new FEDict(FEFileName.feDictFilename);
		if (wnr == null) {
			if (lwnr == null) lwnr = new WordNetRelations(FEFileName.stopwordFilename, FEFileName.wordnetFilename);
			wnr = lwnr;
		}

		candidateLines = new ArrayList<int[][]>();
		feLines = frameElementLines;
		DataPrep.tagLines = tagLines;

		Scanner canScanner = FileUtil.openInFile(FEFileName.candidateFilename);
		final boolean hasCandidateFile = (canScanner != null);

		System.err.println("Loading data....");
		for (String feline : feLines) {
			final int sentNum = parseInt(feline.split("\t")[5]);
			DataPointWithElements dp = new DataPointWithElements(tagLines.get(sentNum), feline);
			ArrayList<int[]> spanList;
			if (hasCandidateFile) {
				spanList = Lists.newArrayList();
				String spanTokens[] = canScanner.nextLine().trim().split("\t|:");
				for (int i = 0; i < spanTokens.length; i += 2) {
					spanList.add(new int[]{parseInt(spanTokens[i]), parseInt(spanTokens[i+1])});
				}
			}
			else {
				spanList = addConstituent(dp);
			}
			//add null span to candidates
			spanList.add(new int[]{-1, -1, 0});
			candidateLines.add(spanList.toArray(new int[spanList.size()][]));
		}
		feIndex = 0;
	}

	public int[][][] getDataPoints(Sentence sentence, String feline, WordNetRelations lwnr) throws IOException {
		checkNotNull(sentence);
		checkNotNull(feline);
		checkNotNull(lwnr);
		if (fedict == null) fedict = new FEDict(FEFileName.feDictFilename);
		if (wnr == null) wnr = lwnr;

		System.err.println("Loading data....");
		final DataPointWithElements dataPointWithElements = new DataPointWithElements(sentence, feline);
		final ArrayList<int[]> spanList = addConstituent(dataPointWithElements);
		//add null span to candidates
		spanList.add(new int[]{-1, -1, 0});
		final int[][] spanArray = spanList.toArray(new int[spanList.size()][]);
		return getTrainData(feline, spanArray, sentence);
	}

	/**
	 * Adds frame element filler spans from goldDP to spanMat.
	 * Modifies spanMat in place, and also returns it.
	 *
	 * @param goldDP the data point whose spans to add
	 * @param spanMat the 2d boolean matrix to add spans to
	 */
	private boolean[][] addGoldSpan(DataPointWithElements goldDP, boolean[][] spanMat) {
		List<Range0Based> spans = goldDP.getOvertFrameElementFillerSpans();
		for(Range0Based span : spans) {
			spanMat[span.getStart()][span.getEnd()] = true;
		}
		return spanMat;
	}

	/**
	 * Adds parses from goldDP to depParses
	 *
	 * @param goldDP the DataPointWithElements whose parses to add to depParses
	 * @param depParses the array to add parses to
	 */
	private void addKBestParses(DataPointWithElements goldDP, int[][] depParses) {
		List<Range0Based> spans = goldDP.getOvertFrameElementFillerSpans();
		DependencyParses parses = goldDP.getParses();
		for (Range0Based span : spans) {
			Range1Based span1 = new Range1Based(span);
			Pair<Integer, DependencyParse> indexAndParse = parses.matchesSomeConstituent(span1);
			if (indexAndParse == null) {
				depParses[span.getStart()][span.getEnd()] = 0;
			} else {
				int fIndex = indexAndParse.getFirst();
				depParses[span.getStart()][span.getEnd()] = fIndex;
			}
		}
	}

	public boolean hasNext() {
		return feIndex < feLines.size();
	}

	public static void addFeature(String key, HashMap<String, Integer> freqmap) {
		if(!freqmap.containsKey(key)) {
			freqmap.put(key, numFeat + 1);
			numFeat++;
		}
	}

	public int[][][] getNextTrainData() {
		final String feline = feLines.get(feIndex);
		final int candidateTokens[][] = candidateLines.get(feIndex);
		final int sentNum = parseInt(feline.split("\t")[5]);
		final String parseLine = tagLines.get(sentNum);
		final Sentence sentence = Sentence.fromAllLemmaTagsArray(AllLemmaTags.readLine(parseLine));
		final int[][][] allData = getTrainData(feline, candidateTokens, sentence);
		feIndex++;
		return allData;
	}

	private int[][][] getTrainData(String feline, int[][] candidateTokens, Sentence sentence) {
		final DataPointWithElements goldDP = new DataPointWithElements(sentence, feline);
		final String frame = goldDP.getFrameName();
		final String[] canArgs = fedict.lookupFrameElements(frame);
		final HashSet<String> realizedFes = new HashSet<String>();
		final ArrayList<int[][]> dataPointList = new ArrayList<int[][]>();
		final String frameElementNames[] = goldDP.getOvertFilledFrameElementNames();
		final List<Range0Based> spans = goldDP.getOvertFrameElementFillerSpans();
		//add realized frame elements
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
		return dataPointList.toArray(new int[dataPointList.size()][][]);
	}

	private void addFeatureForOneArgument(DataPointWithElements goldDP,
			String frame, String fe, Range0Based span,
			ArrayList<int[][]> dataPoints, int candidateTokens[][]) {
		Set<String> featureSet;
		ArrayList<int[]> adatapoint = new ArrayList<int[]>();
		// add feature for gold standard
		int parseId = 0;
		for (int j = 0; j < candidateTokens.length; j++) {
			if (candidateTokens[j][0] == span.getStart()
					&& candidateTokens[j][1] == span.getEnd()) {
				parseId = candidateTokens[j][2];
			}
		}
		DependencyParse selectedParse = goldDP.getParses().get(parseId);
		featureSet = FeatureExtractor.extractFeatures(goldDP, frame, fe, span, wnr, selectedParse).keySet();
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
		adatapoint.add(featArray);
		// add features for candidate spans
		for (int j = 0; j < candidateTokens.length; j++) {
			// make sure does not print gold span again
			if (candidateTokens[j][0] == span.getStart()
					&& candidateTokens[j][1] == span.getEnd())
				continue;
			
			Range0Based canspan;
			canspan = CandidateFrameElementFilters.createSpanRange(candidateTokens[j][0], candidateTokens[j][1]);
			selectedParse = goldDP.getParses().get(candidateTokens[j][2]);
			ps.println(canspan.getStart() + "\t" + canspan.getEnd());
			featureSet = FeatureExtractor.extractFeatures(goldDP, frame, fe,
					canspan, wnr, selectedParse).keySet();
			featArray = new int[featureSet.size()];
			featCount = 0;
			for (String feature : featureSet) {
				featArray[featCount] = getIndexOfFeature(feature);
				featCount++;
			}
			adatapoint.add(featArray);
		}

		ps.println();
		int[][] datap = new int[adatapoint.size()][];
		adatapoint.toArray(datap);
		dataPoints.add(datap);
	}

	// loads a list of features into a hash
	public static void loadFeatureIndex(String alphabetFilename) throws FileNotFoundException {
		featIndex = readFeatureIndex(new File(alphabetFilename));
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
		PrintStream localps = FileUtil.openOutFile(alphabetFilename);
		localps.println(numFeat);
		String buf[] = new String[numFeat + 1];
		for (String feature : featIndex.keySet()) {
			buf[featIndex.get(feature)] = feature;
		}
		for (int i = 1; i <= numFeat; i++) {
			localps.println(buf[i]);
		}
		localps.close();
	}

	public static void findSpans(boolean[][] spanMat, int[][] heads, DependencyParse[] nodes) {
		final int length = nodes.length - 1;
		int[] parent = new int[length];
		int left[] = new int[length];
		int right[] = new int[length];
		for (int i : xrange(length)) {
			parent[i] = (nodes[i + 1].getParentIndex() - 1);
			left[i] = i;
			right[i] = i;
		}
		for (int i : xrange(length)) {
			int index = parent[i];
			while (index >= 0) {
				if (left[index] > i) {
					left[index] = i;
				}
				if (right[index] < i) {
					right[index] = i;
				}
				index = parent[index];
			}
		}
		for (int i : xrange(length)) {
			spanMat[left[i]][right[i]] = true;
			heads[left[i]][right[i]] = i;
		}
		
		// single words
		for (int i : xrange(length)) {
			spanMat[i][i] = true;
			heads[i][i]=i;
		}
		
		for (int i : xrange(length)) {
			if(!(left[i] < i && right[i] > i)) continue;
			//left
			int justLeft = i - 1;
			if(i - 1 >= 0) {
				if(spanMat[left[i]][justLeft]) {
					if (justLeft-left[i] != 0 || !nodes[justLeft + 1].getPOS().equals("DT")) {
						if (justLeft - left[i] != 0 || !nodes[justLeft + 1].getPOS().equals("JJ")) {
							spanMat[i][right[i]] = true;
							heads[i][right[i]] = i;
						}
					}
				}
			}
			
			//right
			int justRight = i + 1;
			if(justRight <= length -1) {
				if(spanMat[justRight][right[i]]) {
					spanMat[left[i]][i]=true;
					heads[left[i]][i]=i;
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
		Integer idx = featIndex.get(feature);
		if (idx != null) return idx;
		if (genAlpha) {
			addFeature(feature, featIndex);
			return numFeat;
		}
		return 0;
	}
}
