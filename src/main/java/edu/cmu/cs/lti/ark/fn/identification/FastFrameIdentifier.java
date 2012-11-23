/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * FastFrameIdentifier.java is part of SEMAFOR 2.0.
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


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import edu.cmu.cs.lti.ark.util.optimization.LDouble;
import edu.cmu.cs.lti.ark.util.optimization.LogFormula;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;

/**
 * @author dipanjan
 * This class is for finding the best frame for a span of tokens, given 
 * the annotation for a sentence. It is just a single node implementation. 
 * Moreover, it looks only at a small set of frames for seen words
 */
public class FastFrameIdentifier extends LRIdentificationModelSingleNode
{
	private THashMap<String, THashSet<String>> mHvCorrespondenceMap;
	private Map<String, Set<String>> mRelatedWordsForWord;
	private THashMap<String,THashSet<String>> clusterMap;
	private Map<String, Map<String, Set<String>>> mRevisedRelationsMap;
	private Map<String, String> mHVLemmas;
	private int K;
	
	public FastFrameIdentifier(TObjectDoubleHashMap<String> paramList, 
									  String reg, 
									  double l, 
									  THashMap<String, THashSet<String>> frameMap, 
									  THashMap<String, THashSet<String>> wnRelationCache,
									  THashMap<String,THashSet<String>> hvCorrespondenceMap,
									  Map<String, Set<String>> relatedWordsForWord,
									  Map<String, Map<String, Set<String>>> revisedRelationsMap,
									  Map<String, String> hvLemmas)
	{
		super(paramList,reg,l,null,frameMap);
		initializeParameterIndexes();
		this.mParamList=paramList;
		mReg=reg;
		mLambda=l;
		mFrameMap=frameMap;
		totalNumberOfParams=paramList.size();
		initializeParameters();
		mLookupChart = new TIntObjectHashMap<LogFormula>();
		mHvCorrespondenceMap = hvCorrespondenceMap;
		mRelatedWordsForWord = relatedWordsForWord;
		mRevisedRelationsMap = revisedRelationsMap;
		mHVLemmas = hvLemmas;
	}
	
	private double getNumeratorValue(String frameName, int[] intTokNums, String[][] data)
	{
		m_current = 0;
		m_llcurrent = 0;		
		double idVal = getValueForFrame(frameName, intTokNums,data);
		return idVal;
	}	
	
	private double getValueForFrame(String frame, int[] intTokNums, String[][] data)	
	{
		THashSet<String> hiddenUnits = mFrameMap.get(frame);
		double result = 0.0;
		DependencyParse parse = DependencyParse.processFN(data, 0.0);
		for (String unit : hiddenUnits)
		{
			IntCounter<String> valMap = null;
			FeatureExtractor featex = new FeatureExtractor();
			if(clusterMap==null) {
				valMap =  
					featex.extractFeaturesLessMemory(frame, 
							intTokNums, 
							unit, 
							data,
							"test", 
							mRelatedWordsForWord, 
							mRevisedRelationsMap,
							mHVLemmas,
							parse);
			} else { // not supported
				valMap =  
					featex.extractFeaturesWithClusters(frame, intTokNums, unit, data, mWNR, "test", null, null,parse,clusterMap,K);
			}
			Set<String> features = valMap.keySet();
			double featSum = 0.0;
			for (String feat : features)
			{
				double val = valMap.getT(feat);
				int ind = localA.get(feat);
				double paramVal = V[ind].exponentiate();
				double prod = val*paramVal;
				featSum+=prod;
			}
			double expVal = Math.exp(featSum);
			result+=expVal;
		}
		return result;
	}
	
	private String getMaxHiddenVariable(String frame, int[] intTokNums, String[][] data)
	{
		THashSet<String> hiddenUnits = mFrameMap.get(frame);
		String maxHV = null;
		double maxVal = -Double.MAX_VALUE;
		DependencyParse parse = DependencyParse.processFN(data, 0.0);
		for (String unit : hiddenUnits)
		{
			FeatureExtractor featex = new FeatureExtractor();
			IntCounter<String> valMap =
				featex.extractFeaturesLessMemory(frame, 
						intTokNums, 
						unit, 
						data,
						"test", 
						mRelatedWordsForWord, 
						mRevisedRelationsMap,
						mHVLemmas,
						parse);
			Set<String> features = valMap.keySet();
			double featSum = 0.0;
			for (String feat : features)
			{
				double val = valMap.getT(feat);
				int ind = localA.get(feat);
				double paramVal = V[ind].exponentiate();
				double prod = val*paramVal;
				featSum+=prod;
			}
			double expVal = Math.exp(featSum);
			if(expVal>maxVal)
			{
				maxVal=expVal;
				maxHV=unit;
			}
		}
		return maxHV;
	}
	
	
	
	public Set<String> checkPresenceOfTokensInMap(int[] intTokNums, String[][] data)
	{
		String lemmatizedTokens = "";
		for(int i = 0; i < intTokNums.length; i ++)
		{
			String lexUnit = data[0][intTokNums[i]];
			String pos = data[1][intTokNums[i]];
			//lemmatizedTokens+=mWNR.getLemmaForWord(lexUnit, pos).toLowerCase()+" ";
			lemmatizedTokens+=data[5][intTokNums[i]]+" ";
		}
		lemmatizedTokens=lemmatizedTokens.trim();
		return mHvCorrespondenceMap.get(lemmatizedTokens);
	}
	
	public String getHuStat(String bestFrame, String frameLine, String parseLine)
	{
		String[] toks = frameLine.split("\t");
		String[] tokNums = toks[1].split("_");
		int[] intTokNums = new int[tokNums.length];
		for(int j = 0; j < tokNums.length; j ++)
			intTokNums[j] = new Integer(tokNums[j]);
		Arrays.sort(intTokNums);
		StringTokenizer st = new StringTokenizer(parseLine,"\t");
		int tokensInFirstSent = new Integer(st.nextToken());
		String[][] data = new String[6][tokensInFirstSent];
		for(int k = 0; k < 6; k ++)
		{
			data[k]=new String[tokensInFirstSent];
			for(int j = 0; j < tokensInFirstSent; j ++)
			{
				data[k][j]=""+st.nextToken().trim();
			}
		}	
		Set<String> set = checkPresenceOfTokensInMap(intTokNums,data);
		if(set==null)
		{
			set = mFrameMap.keySet();
		}
//		else
//		{
//			System.out.println("Problem in finding the set. Exiting.");
//			System.exit(0);
//		}
		String hv =  getMaxHiddenVariable(bestFrame, intTokNums, data);
		return hv;
	}
	
	public void setClusterInfo(THashMap<String,THashSet<String>> clusterMap,int K)
	{
		this.clusterMap=clusterMap;
		this.K=K;
	}
	
	
	public String[] getBestFrame(String frameLine, String parseLine, boolean printConf)
	{
		if (!printConf) {
			return new String[] {getBestFrame(frameLine, parseLine)};
		}
		double maxVal = -Double.MIN_VALUE;
		String[] toks = frameLine.split("\t");
		String[] tokNums = toks[1].split("_");
		int[] intTokNums = new int[tokNums.length];
		for(int j = 0; j < tokNums.length; j ++)
			intTokNums[j] = new Integer(tokNums[j]);
		Arrays.sort(intTokNums);
		StringTokenizer st = new StringTokenizer(parseLine,"\t");
		int tokensInFirstSent = new Integer(st.nextToken());
		String[][] data = new String[6][tokensInFirstSent];
		for(int k = 0; k < 6; k ++)
		{
			data[k]=new String[tokensInFirstSent];
			for(int j = 0; j < tokensInFirstSent; j ++)
			{
				data[k][j]=""+st.nextToken().trim();
			}
		}	
		Set<String> set = checkPresenceOfTokensInMap(intTokNums,data);
		String option = null;
		if(set==null)
		{
			set = mFrameMap.keySet();
		}		
		option = "" + set.size();
		double sum = 0.0;
		int count = 0;
		Pair<String, Double>[] frames = new Pair[set.size()];
		for(String frame: set)
		{
			double val =  getNumeratorValue(frame, intTokNums, data);
			frames[count] = new Pair<String, Double>(frame, val);
			count++;
 			sum += val;
			//System.out.println("Considered "+frame+" for frameLine:"+frameLine);
		}
		Comparator<Pair<String, Double>> c = new Comparator<Pair<String, Double>>() {
			public int compare(Pair<String, Double> arg0,
					Pair<String, Double> arg1) {
				if (arg0.getSecond() > arg1.getSecond()) {
					return -1;
				} else if (arg0.getSecond() == arg1.getSecond()) {
					return 0;
				} else { 
					return 1;
				}
			}			
		};	
		Arrays.sort(frames, c);
		int K = frames.length < 10 ? frames.length : 10;
		String[] results = new String[K];
		for (int i = 0; i < K; i ++) {
			results[i] = frames[i].getFirst() + "\t" + (frames[i].getSecond() / sum);
  		}
		return results;
	}	
	
	public String getBestFrame(String frameLine, String parseLine, SmoothedGraph sg)
	{
		String result = null;
		double maxVal = -Double.MIN_VALUE;
		String[] toks = frameLine.split("\t");
		String[] tokNums = toks[1].split("_");
		int[] intTokNums = new int[tokNums.length];
		for(int j = 0; j < tokNums.length; j ++)
			intTokNums[j] = new Integer(tokNums[j]);
		Arrays.sort(intTokNums);
		StringTokenizer st = new StringTokenizer(parseLine,"\t");
		int tokensInFirstSent = new Integer(st.nextToken());
		String[][] data = new String[6][tokensInFirstSent];
		for(int k = 0; k < 6; k ++)
		{
			data[k]=new String[tokensInFirstSent];
			for(int j = 0; j < tokensInFirstSent; j ++)
			{
				data[k][j]=""+st.nextToken().trim();
			}
		}	
		String finetoken = null;
		String coarsetoken = null;
		Set<String> set = checkPresenceOfTokensInMap(intTokNums,data);
		if (set == null) {
			if (intTokNums.length > 1) {
				coarsetoken = "";
				for (int j = 0; j < intTokNums.length; j++) {
					coarsetoken += data[0][intTokNums[j]].toLowerCase() + " ";
				}
				coarsetoken = coarsetoken.trim();
				coarsetoken = 
					ScanAdverbsAndAdjectives.getCanonicalForm(coarsetoken);
				if (sg.getCoarseMap().containsKey(coarsetoken)) {
					set = sg.getCoarseMap().get(coarsetoken);
				} else {
					set = null;
				}
				
			} else {
				String lemma = data[5][intTokNums[0]];
				String pos = data[1][intTokNums[0]];
				if (pos.startsWith("N")) {
					pos = "n";
				} else if (pos.startsWith("V")) {
					pos = "v";
				} else if (pos.startsWith("J")) {
					pos = "a";
				} else if (pos.startsWith("RB")) {
					pos = "adv";
				} else if (pos.startsWith("I") || pos.startsWith("TO")) {
					pos = "prep";
				} else {
					pos = null;
				}
				if (pos != null) {
					finetoken = 
						ScanAdverbsAndAdjectives.getCanonicalForm(lemma + "." + pos);
					coarsetoken = 
						ScanAdverbsAndAdjectives.getCanonicalForm(lemma);
					if (sg.getFineMap().containsKey(finetoken)) {
						set = sg.getFineMap().get(finetoken);
					} else if (sg.getCoarseMap().containsKey(coarsetoken)){
						set = sg.getCoarseMap().get(coarsetoken);
					} else {
						set = null;
					}
				} else {
					coarsetoken = 
						ScanAdverbsAndAdjectives.getCanonicalForm(lemma);
					if (sg.getCoarseMap().containsKey(coarsetoken)){
						set = sg.getCoarseMap().get(coarsetoken);
					} else {
						set = null;
					}
				}
			}
		}
		if(set==null)
		{
			set = mFrameMap.keySet();
		}
		for(String frame: set)
		{
			double val =  getNumeratorValue(frame, intTokNums, data);
			if(val>maxVal)
			{
				maxVal = val;
				result=""+frame;
			}
			//System.out.println("Considered "+frame+" for frameLine:"+frameLine);
		}
		return result;
	}	
	
	public String getBestFrame(String orgFELine, String frameLine, String parseLine)
	{
		String result = null;
		double maxVal = -Double.MIN_VALUE;
		String[] toks = frameLine.split("\t");
		String[] tokNums = toks[1].split("_");
		int[] intTokNums = new int[tokNums.length];
		for(int j = 0; j < tokNums.length; j ++)
			intTokNums[j] = new Integer(tokNums[j]);
		Arrays.sort(intTokNums);
		StringTokenizer st = new StringTokenizer(parseLine,"\t");
		int tokensInFirstSent = new Integer(st.nextToken());
		String[][] data = new String[6][tokensInFirstSent];
		for(int k = 0; k < 6; k ++)
		{
			data[k]=new String[tokensInFirstSent];
			for(int j = 0; j < tokensInFirstSent; j ++)
			{
				data[k][j]=""+st.nextToken().trim();
			}
		}	
		Set<String> set = checkPresenceOfTokensInMap(intTokNums,data);
		if(set==null)
		{
			set = mFrameMap.keySet();
			System.out.println("Notfound:\t"+frameLine);
		}
		for(String frame: set)
		{
			double val =  getNumeratorValue(frame, intTokNums, data);
			if(val>maxVal)
			{
				maxVal = val;
				result=""+frame;
			}
			//System.out.println("Considered "+frame+" for frameLine:"+frameLine);
		}
		return result;
	}
	
	public String getBestFrame(String frameLine, String parseLine)
	{
		String result = null;
		double maxVal = -Double.MIN_VALUE;
		String[] toks = frameLine.split("\t");
		String[] tokNums = toks[1].split("_");
		int[] intTokNums = new int[tokNums.length];
		for(int j = 0; j < tokNums.length; j ++)
			intTokNums[j] = new Integer(tokNums[j]);
		Arrays.sort(intTokNums);
		StringTokenizer st = new StringTokenizer(parseLine,"\t");
		int tokensInFirstSent = new Integer(st.nextToken());
		String[][] data = new String[6][tokensInFirstSent];
		for(int k = 0; k < 6; k ++)
		{
			data[k]=new String[tokensInFirstSent];
			for(int j = 0; j < tokensInFirstSent; j ++)
			{
				data[k][j]=""+st.nextToken().trim();
			}
		}	
		Set<String> set = checkPresenceOfTokensInMap(intTokNums,data);
		if(set==null)
		{
			set = mFrameMap.keySet();
			System.out.println("Notfound:\t"+frameLine);
		}
		for(String frame: set)
		{
			double val =  getNumeratorValue(frame, intTokNums, data);
			if(val>maxVal)
			{
				maxVal = val;
				result=""+frame;
			}
			//System.out.println("Considered "+frame+" for frameLine:"+frameLine);
		}
		return result;
	}	
	
	public void getAccuracyOnTestSet(ArrayList<String> parses, ArrayList<String> frameLines)
	{
		int total=0;
		int correct=0;
		
		for(String frameLine: frameLines)
		{
			String[] toks = frameLine.split("\t");
			String frameName = toks[0];
			int sentNum = new Integer(toks[2]);
			String parseLine = parses.get(sentNum);
			String foundFrame = getBestFrame(frameLine,parseLine);
			System.out.println("FoundFrame:"+foundFrame+"\tTrue Frame:"+frameName);
			if(frameName.equals(foundFrame))
				correct++;
			total++;
		}
		double accuracy = (double)correct/(double)total;
		System.out.println("Accuracy:"+accuracy);
	}
	
	private static TObjectDoubleHashMap<String> parseParamFile(String paramsFile)
	{
		TObjectDoubleHashMap<String> startParamList = new TObjectDoubleHashMap<String>(); 
		try {
			BufferedReader fis = new BufferedReader(new FileReader(paramsFile));
			String pattern = null;
			int count = 0;
			while ((pattern = fis.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(pattern.trim(),"\t");
				String paramName = st.nextToken().trim();
				String rest = st.nextToken().trim();
				String[] arr = rest.split(",");
				double value = new Double(arr[0].trim());
				boolean sign = new Boolean(arr[1].trim());
				LDouble val = new LDouble(value,sign);
				startParamList.put(paramName, val.exponentiate());
				if(count%100000==0)
					System.out.println("Processed param number:"+count);
				count++;
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return startParamList;
	}	

}

