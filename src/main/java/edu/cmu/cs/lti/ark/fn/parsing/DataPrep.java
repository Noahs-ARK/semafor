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
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithElements;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.FileUtil;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;
import edu.cmu.cs.lti.ark.util.ds.Range1Based;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParses;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

import static java.lang.Integer.parseInt;
import static org.apache.commons.io.IOUtils.closeQuietly;

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
	private static PrintStream ps;

	/**
	 * is it generating an alphabet or using an alphabet
	 */
	public static boolean genAlpha = true;

	
	public static boolean useOracleSpans = false;
	
	public DataPrep() throws FEDict.LoadingException {
		ps = FileUtil.openOutFile(FEFileName.spanfilename);
		load(null, null, null);
	}

	public DataPrep(List<String> tagLines,
					List<String> frameElementLines,
					WordNetRelations lwnr) throws FEDict.LoadingException {
		ps = FileUtil.openOutFile(FEFileName.spanfilename);
		load(tagLines, frameElementLines, lwnr);
	}	
	
	private ArrayList<String> readLinesInFile(String filename){
		ArrayList<String>lines=new ArrayList<String>();
		Scanner sc=FileUtil.openInFile(filename);
		while (sc.hasNextLine()) {
			lines.add(sc.nextLine());
		}
		sc.close();
		return lines;
	}
	private ArrayList<int[]> addConstituent(DataPointWithElements dp){
		DependencyParse parse = dp.getParses().getBestParse();
		DependencyParse[] nodes = parse.getIndexSortedListOfNodes();
		boolean[][] spanMat = new boolean[nodes.length - 1][nodes.length - 1];
		int[][] heads = new int[nodes.length-1][nodes.length-1];
		int[][] depParses = new int[nodes.length-1][nodes.length-1];
		for(int j = 0; j < nodes.length-1; j ++) {
			for(int k = 0; k < nodes.length-1; k ++) {
				spanMat[j][k] = false;
				heads[j][k] = -1;
				depParses[j][k] = 0;
			}
		}
		addGoldSpan(spanMat, dp);
		if(FEFileName.KBestParse > 1) {
			addKBestParses(dp, depParses);
		}
		if(!useOracleSpans) {
			parse = dp.getParses().get(0);
			nodes = parse.getIndexSortedListOfNodes();
			findSpans(spanMat, heads, nodes);
			if(FEFileName.useUnlabeledSpans) {
				for(int j = 1; j < nodes.length; j ++) {
					for(int k = 1; k < nodes.length; k ++) {
						if(j==k) continue;
						if(k<j) continue;
						String span = "";
						for(int l = j; l <= k; l++) {
							span += nodes[l].getWord() + " ";
						}
						span=span.trim().toLowerCase();
						if(FEFileName.unlabeledSpans.contains(span)) {
							if(!spanMat[j-1][k-1]) {
								spanMat[j-1][k-1] = true;
								depParses[j-1][k-1] = findParse(dp.getParses(), j, k, FEFileName.unlabeledSpans.get(span));
							}
						}
					}
				}
			}
		}
		ArrayList<int[]> spanList = new ArrayList<int[]>();
		for (int i = 0; i < spanMat.length; i++) {
			for (int j = 0; j < spanMat.length; j++) {
				if (spanMat[i][j]) {
					spanList.add(new int[] {i, j, depParses[i][j]});
				}
			}
		}
		return spanList;
	}
	
	public int findParse(DependencyParses parses, int j, int k, int hIndex) {
		hIndex=hIndex+j;
		int start=0;
		int ret = 0;
		Range1Based span = new Range1Based(j,k);
		while(true) {
			Pair<Integer,DependencyParse> p = parses.matchesSomeConstituent(span,start);
			if(p==null) {
				ret = 0;
				break;
			}
			int fIndex = p.getSecond().getIndex();
			if(fIndex==hIndex) {
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
	private void load(List<String> tagLines, List<String> frameElementLines, WordNetRelations lwnr)
			throws FEDict.LoadingException {
		if (fedict == null) {
			fedict = new FEDict(FEFileName.fedictFilename1);
		}
		candidateLines = new ArrayList<int[][]>();
		if (frameElementLines == null) {
			feLines = readLinesInFile(FEFileName.feFilename);
		} else {
			feLines = frameElementLines;
		}
		if (tagLines == null) {
			DataPrep.tagLines = readLinesInFile(FEFileName.tagFilename);
		} else {
			DataPrep.tagLines = tagLines;
		}

		Scanner canScanner = FileUtil.openInFile(FEFileName.candidateFilename);
		final boolean hasCandidateFile = (canScanner != null);

		if (wnr == null) {
			if (lwnr != null) {
				wnr = lwnr;
			} else {
				wnr = new WordNetRelations(FEFileName.stopwordFilename, FEFileName.wordnetFilename);
			}
		}
		System.out.println("Loading data....");
		for (String feline : feLines) {
			int sentNum = parseInt(feline.split("\t")[5]);
			DataPointWithElements dp = new DataPointWithElements(DataPrep.tagLines.get(sentNum), feline);
			ArrayList<int[]> spanList;
			if (hasCandidateFile) {
				spanList = Lists.newArrayList();
				String spanTokens[] = canScanner.nextLine().trim().split("\t|:");
				for (int i = 0; i < spanTokens.length; i += 2) {
					spanList.add(new int[]{parseInt(spanTokens[i]), parseInt(spanTokens[i + 1])});
				}
			}
			else {
				spanList = addConstituent(dp);
			}
			//add null span to candidates
			spanList.add(new int[]{-1, -1, 0});
			candidateLines.add(spanList.toArray(new int[spanList.size()][]));
		}
		reset();
	}

	public void addGoldSpan(boolean[][] spanMat, DataPointWithElements goldDP) {
		List<Range0Based> spans = goldDP.getOvertFrameElementFillerSpans();
		for(Range0Based span : spans) {
			spanMat[span.getStart()][span.getEnd()] = true;
		}
	}

	private void addKBestParses(DataPointWithElements goldDP, int[][] depParses) {
		List<Range0Based> spans = goldDP.getOvertFrameElementFillerSpans();
		DependencyParses parses = goldDP.getParses();
		for (Range0Based span : spans) {
			int start = 0;
			Range1Based span1 = new Range1Based(span);
			Pair<Integer, DependencyParse> p = parses.matchesSomeConstituent(span1, start);
			if (p == null) {
				depParses[span.getStart()][span.getEnd()] = 0;
			} else {
				int fIndex = p.getFirst();
				depParses[span.getStart()][span.getEnd()] = fIndex;
			}
		}
	}

	public boolean hasNext() {
		return feIndex < feLines.size();
	}

	public void reset() {
		feIndex = 0;
	}

	public static void addFeature(String key, HashMap<String, Integer> freqmap) {
		if(!freqmap.containsKey(key)) {
			freqmap.put(key, numFeat + 1);
			numFeat++;
		}
	}

	public int[][][] getNextTrainData() {
		int[][][] allData;
		String feline = feLines.get(feIndex);
		int sentNum = parseInt(feline.split("\t")[5]);
		final String parseLine = tagLines.get(sentNum);
		DataPointWithElements goldDP = new DataPointWithElements(parseLine, feline);
		String frame = goldDP.getFrameName();
		int candidateTokens[][] = candidateLines.get(feIndex);
		String[] canArgs = fedict.lookupFrameElements(frame);
		HashSet<String> realizedFes = new HashSet<String>();
		ArrayList<int[][]> dataPointList = new ArrayList<int[][]>();
		String frameElementNames[] = goldDP.getOvertFilledFrameElementNames();
		List<Range0Based> spans = goldDP.getOvertFrameElementFillerSpans();
		//add realized frame elements
		for (int i = 0; i < goldDP.getNumOvertFrameElementFillers(); i++) {
			if(realizedFes.contains(frameElementNames[i]))
				continue;
			realizedFes.add(frameElementNames[i]);
			addFeatureForOneArgument(goldDP, frame, frameElementNames[i], spans.get(i),dataPointList, candidateTokens);
		}
		//add null frame elements
		if (canArgs != null) {
			for (String fe : canArgs) {
				if (!realizedFes.contains(fe)) {
					addFeatureForOneArgument(goldDP, frame, fe,
							CandidateFrameElementFilters.EMPTY_SPAN,
							dataPointList, candidateTokens);
				}
			}
		}
		allData = dataPointList.toArray(new int[dataPointList.size()][][]);
		feIndex++;
		return allData;
	}
	
	private void addFeatureForOneArgument(DataPointWithElements goldDP,
			String frame, String fe, Range0Based span,
			ArrayList<int[][]> datapointlist, int cantoks[][]) {
		Set<String> featureSet;
		ArrayList<int[]> adatapoint = new ArrayList<int[]>();
		// add feature for gold standard
		int parseId = 0;
		for (int j = 0; j < cantoks.length; j++) {
			if (cantoks[j][0] == span.getStart() 
					&& cantoks[j][1] == span.getEnd()) {
				parseId = cantoks[j][2];
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
		for (int j = 0; j < cantoks.length; j++) {
			// make sure does not print gold span again
			if (cantoks[j][0] == span.getStart() 
					&& cantoks[j][1] == span.getEnd())
				continue;
			
			Range0Based canspan;
			canspan = CandidateFrameElementFilters.createSpanRange(cantoks[j][0], cantoks[j][1]);
			selectedParse = goldDP.getParses().get(cantoks[j][2]);
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
		datapointlist.add(datap);
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
		int[] parent = new int[nodes.length - 1];
		int left[] = new int[parent.length];
		int right[] = new int[parent.length];
		for (int i = 0; i < parent.length; i++) {
			parent[i] = (nodes[i + 1].getParentIndex() - 1);
			left[i] = i;
			right[i] = i;
		}
		for (int i = 0; i < parent.length; i++) {
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
		for (int i = 0; i < parent.length; i++) {
			spanMat[left[i]][right[i]] = true;
			heads[left[i]][right[i]] = i;
		}
		
		// single words
		for (int i = 0; i < parent.length; i++) {
			spanMat[i][i] = true;
			heads[i][i]=i;
		}
		
		for (int i = 0; i < parent.length; i++) {
			if(!(left[i]<i&&right[i]>i)) continue;
			//left
			int justLeft=i-1;
			if(i-1>=0) {
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
			int justRight=i+1;
			if(justRight<=parent.length-1) {
				if(spanMat[justRight][right[i]]) {
					spanMat[left[i]][i]=true;
					heads[left[i]][i]=i;
				}
			}
		}
		
	}

	/**
	 * Look up the index in of feature in in our map
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
