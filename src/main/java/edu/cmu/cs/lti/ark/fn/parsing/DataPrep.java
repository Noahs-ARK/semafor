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

import java.io.PrintStream;
import java.util.*;

import edu.cmu.cs.lti.ark.fn.utils.DataPointWithElements;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParses;
import edu.cmu.cs.lti.ark.util.FileUtil;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;
import edu.cmu.cs.lti.ark.util.ds.Range1Based;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.fn.parsing.CandidateFrameElementFilters;
import edu.cmu.cs.lti.ark.fn.parsing.FeatureExtractor;

public class DataPrep {
	/**
	 * @brief prefix for null features
	 */
	public static final String NULL_SPAN = "NULL_SPAN";
	/**
	 * @brief an array list containing candidate spans for each sentence
	 * the m'th span in the n'th sentence is 
	 * canLines.get(n)[m][0] and canLines.get(n)[m][1]
	 */
	public ArrayList<int[][]> canLines;
	/**
	 * a hash map from a frame name to its
	 * frame elements.
	 */
	public static FEDict fedict;
	public static WordNetRelations wnr;
	/**
	 * contains lines in frame element file
	 */
	public static ArrayList<String> feLines;
	/**
	 * lines in tags file
	 */
	public static ArrayList<String> tagLines;
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
	
	public DataPrep()
	{
		ps = FileUtil.openOutFile(FEFileName.spanfilename);
		load(null, null, null);
	}
	
	public DataPrep(String spansFile)
	{
		ps = FileUtil.openOutFile(FEFileName.spanfilename);
		load(null, null, null);
	}
	
	public DataPrep(ArrayList<String> tL, 
					ArrayList<String> fL,
					WordNetRelations lwnr) {
		// this model does not write span files
		ps = FileUtil.openOutFile(FEFileName.spanfilename);
		load(tL, fL, lwnr);		
	}	
	
	private ArrayList<String >readLinesInFile(String filename){
		ArrayList<String>lines=new ArrayList<String>();
		Scanner sc=FileUtil.openInFile(filename);
		while (sc.hasNextLine()) {
			lines.add(sc.nextLine());
		}
		sc.close();
		return lines;
	}
	private ArrayList<int[]>  addConstituent(DataPointWithElements dp){
		DependencyParse parse=dp.getParses().getBestParse();
		DependencyParse[] nodes=DependencyParse.getIndexSortedListOfNodes(parse);
		boolean[][] spanMat = new boolean[nodes.length - 1][nodes.length - 1];
		int[][] heads = new int[nodes.length-1][nodes.length-1];
		int[][] depParses = new int[nodes.length-1][nodes.length-1];
		for(int j = 0; j < nodes.length-1; j ++)
		{
			for(int k = 0; k < nodes.length-1; k ++)
			{
				spanMat[j][k]=false;
				heads[j][k]=-1;
				depParses[j][k]=0;
			}
		}
		ArrayList<int[]> spanList = new ArrayList<int[]>();
		addGoldSpan(spanMat,dp,depParses);
		if(!useOracleSpans)
		{
			parse = dp.getParses().get(0);
			nodes = DependencyParse.getIndexSortedListOfNodes(parse);
			findSpans(spanMat, heads, nodes);
			if(FEFileName.useUnlabeledSpans)
			{
				for(int j = 1; j < nodes.length; j ++)
				{
					for(int k = 1; k < nodes.length; k ++)
					{
						if(j==k)
							continue;
						if(k<j)
							continue;
						String span = "";
						for(int l = j; l <= k; l ++)
							span+= nodes[l].getWord()+" ";
						span=span.trim().toLowerCase();
						if(FEFileName.unlabeledSpans.contains(span))
						{
							if(!spanMat[j-1][k-1])
							{
								spanMat[j-1][k-1]=true;
								depParses[j-1][k-1]=findParse(dp.getParses(),j,k,FEFileName.unlabeledSpans.get(span));
							}
						}
					}
				}
			}
		}
		for (int i = 0; i < spanMat.length; i++) {
			for (int j = 0; j < spanMat.length; j++) {
				if (spanMat[i][j]) {
					int span[] = new int[3];
					span[0] = i;
					span[1] = j;
					span[2] = depParses[i][j];
					spanList.add(span);
				}
			}
		}
		return spanList;
	}
	
	public int findParse(DependencyParses parses, int j, int k, int hIndex)
	{
		hIndex=hIndex+j;
		int start=0;
		int ret = 0;
		Range1Based span = new Range1Based(j,k);
		while(true)
		{
			Pair<Integer,DependencyParse> p = parses.matchesSomeConstituent(span,start);
			if(p==null)
			{
				ret = 0;
				break;
			}
			int fIndex = p.getSecond().getIndex();
			if(fIndex==hIndex)
			{
				if(p.getFirst()!=0)
					System.out.println("Found correct parse:"+p.getFirst());
				return p.getFirst();				
			}
			else
			{
				start=p.getFirst()+1;
				if(start>=parses.size())
					break;
			}
		}
		return ret;
	}
	
	
	/**
	 * @brief load data needed for feature extraction
	 * 
	 */
	private void load(ArrayList<String> tL,
					  ArrayList<String> fL,
					  WordNetRelations lwnr) {
		if (fedict == null) {
			fedict = new FEDict(FEFileName.fedictFilename1);
		}
		// fedict.merge(FEFileName.fedictFilename2);
		canLines = new ArrayList<int[][]>();
		if (fL == null) {
			feLines = readLinesInFile(FEFileName.feFilename);
		} else {
			feLines = fL;
		}
		if (tL == null) {
			tagLines = readLinesInFile(FEFileName.tagFilename);
		} else {
			tagLines = tL;
		}		
		
		boolean hasCandidateFile = true;
		
		Scanner canScanner = FileUtil.openInFile(FEFileName.candidateFilename);
		if (canScanner == null) {
			hasCandidateFile = false;
		}
		if (wnr == null) {
			if (lwnr != null) {
				wnr = lwnr;
			} else {
				wnr = new WordNetRelations(FEFileName.stopwordFilename,
						FEFileName.wordnetFilename);
			}
		}
		int span[];
		System.out.println("Loading data....");
		int count = 0;
		for (String feline : feLines)
		{
			int sentNum = Integer.parseInt(feline.split("\t")[5]);
			DataPointWithElements dp = new DataPointWithElements(tagLines.get(sentNum), feline);
			int spans[][];
			ArrayList<int[]> spanList;
			if (hasCandidateFile) {
				spanList=new ArrayList<int[]>();
				String spanToks[] = canScanner.nextLine().trim().split("\t|:");
				for (int i = 0; i < spanToks.length; i += 2) {
					span = new int[2];
					span[0] = Integer.parseInt(spanToks[i]);
					span[1] = Integer.parseInt(spanToks[i + 1]);
					spanList.add(span);
				}
			} 
			else
			{
				spanList=addConstituent(dp);
			}
			//add null span to candidates
			span=new int[3];
			span[0]=-1;
			span[1]=-1;
			span[2]=0;
			spanList.add(span);
			spans = new int[spanList.size()][];
			spanList.toArray(spans);
			canLines.add(spans);
			count++;
		}
		reset();
	}

	public void addGoldSpan(boolean [][] spanMat,DataPointWithElements goldDP,int[][] depParses)
	{
		List<Range0Based> spans = goldDP.getOvertFrameElementFillerSpans();
		DependencyParses parses = goldDP.getParses();
		for(Range0Based span:spans)
		{
			spanMat[span.getStart()][span.getEnd()]=true;
			if(FEFileName.KBestParse>1)
			{
				int start=0;
				Range1Based span1 = new Range1Based(span);
				Pair<Integer,DependencyParse> p = parses.matchesSomeConstituent(span1,start);
				if(p==null)
					depParses[span.getStart()][span.getEnd()]=0;
				else
				{	int fIndex = p.getFirst();
					depParses[span.getStart()][span.getEnd()]=fIndex;
				}
			}
		}
	}
	
	public boolean hasNext() {
		if (feIndex >= feLines.size())
			return false;
		return true;
	}

	public void reset() {
		feIndex = 0;
	}

	public static void addFeature(String key, HashMap<String, Integer> freqmap) {
		Integer freq = freqmap.get(key);
		if (freq == null) {
			freqmap.put(key, numFeat + 1);
			numFeat++;
		}
	}

	public int[][][] getTrainData() {
		int[][][] alldata = null;
		String feline = feLines.get(feIndex);
		int sentNum = Integer.parseInt(feline.split("\t")[5]);
		DataPointWithElements goldDP = new DataPointWithElements(tagLines.get(sentNum), feline);
		String frame = goldDP.getFrameName();
		int cantoks[][] = canLines.get(feIndex);
		String[] canArgs = fedict.lookupFes(frame);
		HashSet<String> realizedFes = new HashSet<String>();
		ArrayList<int[][]> datapointList = new ArrayList<int[][]>();
		String fename[] = goldDP.getOvertFilledFrameElementNames();
		List<Range0Based> spans = goldDP.getOvertFrameElementFillerSpans();
		//add realized frame elements
		for (int i = 0; i < goldDP.getNumOvertFrameElementFillers(); i++) {
			if(realizedFes.contains(fename[i]))
				continue;
			realizedFes.add(fename[i]);
			addFeatureForOneArgument(goldDP, frame, fename[i], spans.get(i),datapointList, cantoks);
		}
		//add null frame elements
		if (canArgs != null)
			for (String fe : canArgs) {
				if (!realizedFes.contains(fe)) {
					addFeatureForOneArgument(goldDP, frame, fe,
							CandidateFrameElementFilters.EMPTY_SPAN,
							datapointList, cantoks);
				}
			}
		alldata = new int[datapointList.size()][][];
		datapointList.toArray(alldata);
		feIndex++;
		return alldata;
	}

	public int[][][] getTrainDataOracleSpans() {
		int[][][] alldata = null;
		String feline = feLines.get(feIndex);
		int sentNum = Integer.parseInt(feline.split("\t")[5]);
		DataPointWithElements goldDP = new DataPointWithElements(tagLines
				.get(sentNum), feline);
		String frame = goldDP.getFrameName();
		int cantoks[][] = canLines.get(feIndex);
		String[] canArgs = fedict.lookupFes(frame);
		HashSet<String> realizedFes = new HashSet<String>();
		ArrayList<int[][]> datapointList = new ArrayList<int[][]>();
		String fename[] = goldDP.getOvertFilledFrameElementNames();
		List<Range0Based> spans = goldDP.getOvertFrameElementFillerSpans();
		//add realized frame elements
		for (int i = 0; i < goldDP.getNumOvertFrameElementFillers(); i++) {
			realizedFes.add(fename[i]);
			addFeatureForOneArgumentOracleSpans(goldDP, frame, fename[i], spans.get(i),
					datapointList, cantoks);	
		}
		//add null frame elements
		if (canArgs != null)
			for (String fe : canArgs) {
				if (!realizedFes.contains(fe)) {
					addFeatureForOneArgumentOracleSpans(goldDP, frame, fe,
							CandidateFrameElementFilters.EMPTY_SPAN,
							datapointList, cantoks);
				}
			}
		alldata = new int[datapointList.size()][][];
		datapointList.toArray(alldata);
		feIndex++;
		return alldata;
	}
	
	private void addFeatureForOneArgumentOracleSpans(DataPointWithElements goldDP,
			String frame, String fe, Range0Based span,
			ArrayList<int[][]> datapointlist, int cantoks[][]) {
		Set<String> featureSet;
		ArrayList<int[]> adatapoint = new ArrayList<int[]>();
		// add feature for gold standard
		featureSet = FeatureExtractor.extractFeatures(goldDP, frame, fe, span,
				wnr,goldDP.getParses().getBestParse()).keySet();
		/*
		System.out.println(goldDP.getSentenceNum() + "\t" + fe + "\t" + frame + "\t"
				+ goldDP.getTokenNums()[0] + "\t"
				+ goldDP.getTokenNums()[goldDP.getTokenNums().length - 1]
				+ "\t" + feIndex);
		
		*/
		ps.println(goldDP.getSentenceNum() + "\t" + fe + "\t" + frame + "\t"
				+ goldDP.getTokenNums()[0] + "\t"
				+ goldDP.getTokenNums()[goldDP.getTokenNums().length - 1]
				+ "\t" + feIndex);
		ps.println(span.getStart() + "\t" + span.getEnd());
		int[] featArray = new int[featureSet.size()];
		int featCount = 0;
		for (String feature : featureSet) {
			featArray[featCount] = featidx(feature);
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
			ps.println(canspan.getStart() + "\t" + canspan.getEnd());
			featureSet = FeatureExtractor.extractFeatures(goldDP, frame, fe,
					canspan, wnr,goldDP.getParses().getBestParse()).keySet();
			featArray = new int[featureSet.size()];
			featCount = 0;
			for (String feature : featureSet) {
				featArray[featCount] = featidx(feature);
				featCount++;
			}
			adatapoint.add(featArray);
		}

		ps.println();
		int[][] datap = new int[adatapoint.size()][];
		adatapoint.toArray(datap);
		datapointlist.add(datap);
	}
	
	
	private void addFeatureForOneArgument(DataPointWithElements goldDP,
			String frame, String fe, Range0Based span,
			ArrayList<int[][]> datapointlist, int cantoks[][]) {
		Set<String> featureSet;
		ArrayList<int[]> adatapoint = new ArrayList<int[]>();
		// add feature for gold standard
		int parseId = 0;
		for (int j = 0; j < cantoks.length; j++)
		{
			if (cantoks[j][0] == span.getStart() 
					&& cantoks[j][1] == span.getEnd())
			{
				parseId = cantoks[j][2];
			}
		}
		DependencyParse selectedParse = goldDP.getParses().get(parseId);
		featureSet = FeatureExtractor.extractFeatures(goldDP, frame, fe, span, wnr, selectedParse).keySet();
		/*
		System.out.println(goldDP.getSentenceNum() + "\t" + fe + "\t" + frame + "\t"
				+ goldDP.getTokenNums()[0] + "\t"
				+ goldDP.getTokenNums()[goldDP.getTokenNums().length - 1]
				+ "\t" + feIndex);
		
		*/
		ps.println(goldDP.getSentenceNum() + "\t" + fe + "\t" + frame + "\t"
				+ goldDP.getTokenNums()[0] + "\t"
				+ goldDP.getTokenNums()[goldDP.getTokenNums().length - 1]
				+ "\t" + feIndex);
		ps.println(span.getStart() + "\t" + span.getEnd());
		int[] featArray = new int[featureSet.size()];
		int featCount = 0;
		for (String feature : featureSet) {
			featArray[featCount] = featidx(feature);
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
				featArray[featCount] = featidx(feature);
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
	public static void readFeatureIndex(String alphabetFilename) {
		Scanner localsc = FileUtil.openInFile(alphabetFilename);
		featIndex = new HashMap<String, Integer>();
		localsc.nextLine();
		int count = 0;
		while (localsc.hasNextLine()) {
			addFeature(localsc.nextLine(), featIndex);
			if (count % 100000 == 0) {
				System.out.print(count + " ");
			}
			count++;
		}
		System.out.println();
		localsc.close();
		genAlpha = false;
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

	public static void findSpans(boolean[][] spanMat, DependencyParse[] nodes) {
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
		}
		// single words
		for (int i = 0; i < parent.length; i++) {
			spanMat[i][i] = true;
		}
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
		for (int i = 0; i < parent.length; i++)
		{
			spanMat[left[i]][right[i]] = true;
			heads[left[i]][right[i]] = i;
		}
		
		// single words
		for (int i = 0; i < parent.length; i++) {
			spanMat[i][i] = true;
			heads[i][i]=i;
		}
		
		for (int i = 0; i < parent.length; i++)
		{
			if(!(left[i]<i&&right[i]>i))
				continue;
			//left
			int justLeft=i-1;
			if(i-1>=0)
			{
				if(spanMat[left[i]][justLeft])
				{
					if(justLeft-left[i]==0&&nodes[justLeft+1].getPOS().equals("DT"))
					{
						;
					}
					else if(justLeft-left[i]==0&&nodes[justLeft+1].getPOS().equals("JJ"))
					{
						;
					}
					else
					{	
						spanMat[i][right[i]]=true;
						heads[i][right[i]]=i;
					}
				}
			}
			
			//right
			int justRight=i+1;
			if(justRight<=parent.length-1)
			{
				if(spanMat[justRight][right[i]])
				{
					spanMat[left[i]][i]=true;
					heads[left[i]][i]=i;
				}
			}
		}
		
	}	
	
	
	public int featidx(String feature) {
		Integer fidx = featIndex.get(feature);
		if (fidx != null)
			return fidx;
		if (genAlpha) {
			addFeature(feature, featIndex);
			return numFeat;
		}
		return 0;
	}
}
