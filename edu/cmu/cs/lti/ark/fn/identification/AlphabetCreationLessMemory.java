/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * AlphabetCreationLessMemory.java is part of SEMAFOR 2.0.
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
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import gnu.trove.*;

public class AlphabetCreationLessMemory	
{
	private THashMap<String, THashSet<String>> mFrameMap=null;
	private String mParseFile = null;
	private String mAlphabetFile = null;
	private String mEventDir = null;
	private String mFrameElementsFile = null;
	private THashMap<String,Integer> alphabet = null;
	private Map<String, Set<String>> mRelatedWordsForWord = null;
	private int mStartIndex = -1;
	private int mEndIndex = -1;
	private int mParseOffset = 0;
	private BufferedReader mParseReader = null;
	private String mParseLine = null;
	private Map<String, Map<String, Set<String>>> mRevisedRelationsMap;
	private Map<String, String> mHVLemmas;
	
	public static void main(String[] args)
	{
		FNModelOptions options = new FNModelOptions(args);
		String alphabetFile=options.modelFile.get();
		String eventDir = options.eventsFile.get();
		String feFile = options.trainFrameElementFile.get();
		RequiredDataForFrameIdentification r = (RequiredDataForFrameIdentification)SerializedObjects.readSerializedObject(options.fnIdReqDataFile.get());
		Map<String, Set<String>> relatedWordsForWord = r.getRelatedWordsForWord();
		THashMap<String,THashSet<String>> frameMap = r.getFrameMap();
		int startIndex = options.startIndex.get();
		int endIndex = options.endIndex.get();
		Map<String, Map<String, Set<String>>> revisedRelationsMap = 
			r.getRevisedRelMap();
		Map<String, String> hvLemmas = r.getHvLemmaCache();
		AlphabetCreationLessMemory alph = 
			new AlphabetCreationLessMemory(alphabetFile, 
					eventDir, 
					feFile, 
					options.trainParseFile.get(), 
					frameMap, 
					relatedWordsForWord, 
					startIndex,
					endIndex,
					revisedRelationsMap,
					hvLemmas);
		alph.runAlphabetCreation();
	} 
	
	public AlphabetCreationLessMemory(String alphabetFile,
							String eventDir,
							String frameElementsFile,
							String parseFile, 
							THashMap<String, THashSet<String>> frameMap, 
							Map<String, Set<String>> relatedWordsForWord,
							int startIndex, 
							int endIndex,
							Map<String, Map<String, Set<String>>> rMap,
							Map<String, String> lemmaCache)
	{
		mFrameMap=frameMap;
		mParseFile=parseFile;
		mFrameElementsFile=frameElementsFile;
		mEventDir=eventDir;
		mAlphabetFile=alphabetFile;	
		alphabet = new THashMap<String,Integer>();
		mRelatedWordsForWord = relatedWordsForWord;
		mStartIndex = startIndex;
		mEndIndex = endIndex;
		mHVLemmas = lemmaCache;
		mRevisedRelationsMap = rMap;
	}
	
	public void runAlphabetCreation()
	{
		System.out.println("Creating alphabet....");
		try
		{
			BufferedReader bReader = new BufferedReader(new FileReader(mFrameElementsFile));
			String line = null;
			int count = 0;
			mParseReader = new BufferedReader(new FileReader(mParseFile));
			mParseLine = mParseReader.readLine();
			mParseOffset = 0;
			while((line=bReader.readLine())!=null)
			{
				if (count < mStartIndex) {// skip frame elements prior to the specified range
					count++;
					continue;
				}
				line=line.trim();
				System.out.println("Processing line:"+line);
				processLine(line, count);
				count++;
				if (count == mEndIndex) {
					break;
				}
			}
			bReader.close();
			mParseReader.close();
		}
		catch(Exception e)
		{
			System.out.println("Problem in reading fe file. exiting..");
			System.exit(0);
		}
		writeAlphabetFile();
	}	
	
	
	private void writeAlphabetFile()
	{
		try
		{
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(mAlphabetFile));
			bWriter.write(alphabet.size()+"\n");
			Set<String> set = alphabet.keySet();
			for(String key:set)
			{
				bWriter.write(key+"\t"+alphabet.get(key)+"\n");
			}
			bWriter.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	private int[][] getFeatures(String frame,int[] intTokNums,String[][] data)
	{
		THashSet<String> hiddenUnits = mFrameMap.get(frame);
		DependencyParse parse = DependencyParse.processFN(data, 0.0);
		int hSize = hiddenUnits.size();
		int[][] res = new int[hSize][];
		int hCount = 0;
		for (String unit : hiddenUnits)
		{
			IntCounter<String> valMap = null;
			FeatureExtractor featex = new FeatureExtractor();
			valMap =  featex.extractFeaturesLessMemory(frame,
					intTokNums, 
					unit, 
					data, 
					"test", 
					mRelatedWordsForWord,
					mRevisedRelationsMap,
					mHVLemmas,
					parse);														
			Set<String> features = valMap.keySet();
			ArrayList<Integer> feats = new ArrayList<Integer>();
			for (String feat : features)
			{
				int val = valMap.get(feat);
				int featIndex=-1;
				if(alphabet.containsKey(feat))
				{
					featIndex=alphabet.get(feat);
				}
				else
				{
					featIndex=alphabet.size()+1;
					alphabet.put(feat, featIndex);
				}
				for(int i = 0; i < val; i ++)
				{
					feats.add(featIndex);
				}
			}
			int hFeatSize = feats.size();
			res[hCount]=new int[hFeatSize];
			for(int i = 0; i < hFeatSize; i ++)
			{
				res[hCount][i]=feats.get(i);
			}
			hCount++;
		}
		return res;
	}
	
	private void processLine(String line, int index) throws IOException {
		String[] toks = line.split("\t");
		int sentNum = new Integer(toks[5]);
		while (mParseOffset < sentNum) {
			mParseLine = mParseReader.readLine();
			mParseOffset++;
		}
		String frameName = toks[1];
		String[] tokNums = toks[3].split("_");
		int[] intTokNums = new int[tokNums.length];
		for(int j = 0; j < tokNums.length; j ++)
			intTokNums[j] = new Integer(tokNums[j]);
		Arrays.sort(intTokNums);
		StringTokenizer st = new StringTokenizer(mParseLine,"\t");
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
		allFeatures[0]=getFeatures(frameName,intTokNums,data);
		System.out.print(".");
		int count = 1;
		for(String f:set)
		{
			if(f.equals(frameName))
				continue;
			allFeatures[count]=getFeatures(f,intTokNums,data);
			System.out.print(".");
			count++;
		}
		System.out.println();
		String file = mEventDir+"/feats_"+index+".jobj";
		SerializedObjects.writeSerializedObject(allFeatures, file);
		System.out.println("Created feature object for index:"+index+" alphsize:"+alphabet.size());
		allFeatures=null;
		System.gc();
	}	
}
