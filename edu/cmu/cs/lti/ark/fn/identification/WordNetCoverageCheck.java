/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * WordNetCoverageCheck.java is part of SEMAFOR 2.0.
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

import edu.cmu.cs.lti.ark.fn.identification.FeatureExtractor;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;


public class WordNetCoverageCheck {
	public static void main(String[] args) throws IOException {
		String datadir = "/Users/dipanjand/work/spring2010/FramenetParsing/CVSplits/0";
		// WN files
		String wnConfigFile = "/Users/dipanjand/work/spring2010/FramenetParsing/SSFrameStructureExtraction/file_properties.xml";
		String stopFile = "/Users/dipanjand/work/spring2010/FramenetParsing/SSFrameStructureExtraction/lrdata/stopwords.txt";
		WordNetRelations wnr = new WordNetRelations(stopFile, wnConfigFile);
		Map<String, Set<String>> relatedWordsForWord = 
			(Map<String, Set<String>>)SerializedObjects.readSerializedObject(datadir + "/wnallrelwords.ser");
		Map<String, THashMap<String, Set<String>>> wordNetMap = 
			(Map<String, THashMap<String, Set<String>>>)SerializedObjects.readSerializedObject(datadir + "/wnMap.ser");
		THashMap<String,THashSet<String>> mFrameMap = 
			(THashMap<String,THashSet<String>>)SerializedObjects.readSerializedObject(datadir + "/framenet.original.map");		
		String frameElementsFile = 
			"/Users/dipanjand/work/spring2010/FramenetParsing/uData/0/best.frame.elements";
		String parseFile = "/Users/dipanjand/work/spring2010/FramenetParsing/uData/0/AP_1m.all.lemma.tags";
		BufferedReader bReader = new BufferedReader(new FileReader(parseFile));
		ArrayList<String> mListOfParses = getThousandLines(bReader);
		bReader.close();
		BufferedReader feReader = new BufferedReader(new FileReader(frameElementsFile));
		ArrayList<String> fes = getThousandLines(feReader);
		feReader.close();
		
		for (String line: fes) {
			String[] toks = line.split("\t");
			int sentNum = new Integer(toks[5]);
			String parseLine = mListOfParses.get(sentNum);
			String frameName = toks[1];
			String[] tokNums = toks[3].split("_");
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
			Set<String> set = mFrameMap.keySet();
			int size = set.size();
			int[][][] allFeatures = new int[size][][];
			allFeatures[0]=getFeatures(frameName, 
					intTokNums, 
					data, 
					mFrameMap, 
					wnr,
					relatedWordsForWord, 
					wordNetMap);
			System.out.print(".");
			int count = 1;
			for(String f:set)
			{
				if(f.equals(frameName))
					continue;
				allFeatures[count]=getFeatures(f, 
						intTokNums, 
						data, 
						mFrameMap, 
						wnr,
						relatedWordsForWord,
						wordNetMap);
				System.out.print(".");
				count++;
			}
			System.out.println();	
		}
	}
	
	private static int[][] getFeatures(String frame,
								int[] intTokNums,
								String[][] data,
								THashMap<String,THashSet<String>> mFrameMap,
								WordNetRelations mWnr,
								Map<String, Set<String>> relatedWordsForWord,
								Map<String, THashMap<String, Set<String>>> wordNetMap
								)
	{
		THashSet<String> hiddenUnits = mFrameMap.get(frame);
		DependencyParse parse = DependencyParse.processFN(data, 0.0);
		int hSize = hiddenUnits.size();
		int[][] res = new int[hSize][];
		for (String unit : hiddenUnits)
		{
//			IntCounter<String> valMap1 = null;
//			valMap1 =  FeatureExtractor.extractFeatures(frame,
//						  intTokNums, 
//						  unit, 
//						  data, 
//						  mWnr, 
//						  "test", 
//						  null,
//						  null,
//						  parse);
//			Set<String> features1 = valMap1.keySet();
			IntCounter<String> valMap2 = null;
			FeatureExtractor featex = new FeatureExtractor();
			valMap2 = featex.extractFeaturesLessMemory(
					frame,
					  intTokNums, 
					  unit, 
					  data,  
					  "test", 
					  relatedWordsForWord,
					  null,
					  null,
					  parse);
//			Set<String> features2 = valMap2.keySet();
//			if (features1.size() != features2.size()) {
//				System.out.println("Problem with the two feature sets.");
//				String[] arr1 = new String[features1.size()];
//				features1.toArray(arr1);
//				Arrays.sort(arr1);
//				System.out.println("Feature set 1:");
//				for (String feat:arr1) System.out.println(feat);
//				String[] arr2 = new String[features2.size()];
//				features2.toArray(arr2);
//				Arrays.sort(arr2);
//				System.out.println("\nFeature set 2:");
//				for (String feat:arr2) System.out.println(feat);
//				System.exit(-1);
//			}
//			features2.removeAll(features1);
//			if (features2.size() > 0) {
//				System.out.println("Problem with second features set:");
//				for (String feat: features2) {
//					System.out.println(feat);
//				}
//				System.exit(-1);
//			}
		}
		return res;
	}
	
	public static <T> Set<T> intersection(Set<T> setA, Set<T> setB) {
	    Set<T> tmp = new HashSet<T>();
	    for (T x : setA)
	      if (setB.contains(x))
	        tmp.add(x);
	    return tmp;
	  }
	
	
	public static ArrayList<String> getThousandLines(BufferedReader bReader) throws IOException {
		ArrayList<String> list = new ArrayList<String>();
		String line = null;
		for (int i = 0; i < 1000; i ++) {
			line = bReader.readLine();
			if (line == null) {
				break;
			}
			list.add(line.trim());
		}
 		return list;
	}
}
