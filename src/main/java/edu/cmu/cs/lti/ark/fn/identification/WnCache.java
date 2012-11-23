/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * WnCache.java is part of SEMAFOR 2.0.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.StringTokenizer;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectIdentityHashingStrategy;


public class WnCache
{
	private THashMap<String, THashSet<String>> mFrameMap=null;
	private WordNetRelations mWnr = null;
	private ArrayList<String> mListOfParses = null;
	private String mFrameElementsFile = null;
	private String mWnCacheFile=null;
	private String mLemmaCacheFile = null;
	private THashMap<String,THashSet<String>> cache = null;
	private THashMap<String,String> lemmaCache = null;
	private int mStart = 0;
	private int mEnd = 0;
	
	public static void main(String[] args)
	{
		FNModelOptions options = new FNModelOptions(args);
		String feFile = options.trainFrameElementFile.get();
		ArrayList<String> parses = ParsePreparation.readSentencesFromFile(options.trainParseFile.get());
		THashMap<String, THashSet<String>> frameMap = (THashMap<String, THashSet<String>>)SerializedObjects.readSerializedObject(options.frameNetMapFile.get());
		WordNetRelations wnr = new WordNetRelations(options.stopWordsFile.get(),options.wnConfigFile.get());
		String wnCacheFile = options.wnMapFile.get();
		String lemmaCacheFile = options.lemmaCacheFile.get();
		int start = options.startIndex.get();
		int end = options.endIndex.get();
		WnCache wnc = new WnCache(feFile,parses,frameMap,wnr,wnCacheFile,lemmaCacheFile,start,end);
		wnc.wncache();
	}	
	
	public WnCache(String frameElementsFile,ArrayList<String> parses,THashMap<String, THashSet<String>> frameMap,WordNetRelations wnr,String wnCacheFile, String lemmaCacheFile, int start, int end)
	{
		mWnr = wnr;
		mFrameMap=frameMap;
		mListOfParses=parses;
		mFrameElementsFile=frameElementsFile;
		mWnCacheFile=wnCacheFile;
		mLemmaCacheFile=lemmaCacheFile;
		mStart=start;
		mEnd=end;
	}
		
	public void wncache()
	{
		cache = new THashMap<String,THashSet<String>>();
		lemmaCache = new THashMap<String,String>();
		System.out.println("Caching WN relationships........");
		try
		{
			BufferedReader bReader = new BufferedReader(new FileReader(mFrameElementsFile));
			String line = null;
			int count = 0;
			while((line=bReader.readLine())!=null)
			{
				if(count<mStart)
				{
					count++;
					continue;
				}
				if(count>=mEnd)
				{
					break;
				}
				line=line.trim();
				System.out.println("Processing line number "+count+":"+line);
				processLine(line,count);
				count++;
			}
			bReader.close();
		}
		catch(Exception e)
		{
			System.out.println("Problem in reading fe file. exiting..");
			System.exit(0);
		}
		SerializedObjects.writeSerializedObject(lemmaCache, mLemmaCacheFile+"_"+mStart+"_"+mEnd);
		SerializedObjects.writeSerializedObject(cache, mWnCacheFile+"_"+mStart+"_"+mEnd);
	}
	
	private void updateCache(String frame,int[] intTokNums,String[][] data)
	{
		THashSet<String> hiddenUnits = mFrameMap.get(frame);
		DependencyParse parse = DependencyParse.processFN(data, 0.0);
		for (String unit : hiddenUnits)
		{
			updateCacheForOneUnit(frame,intTokNums,unit,data,mWnr,parse);
		}
	}	
	
	private void updateCacheForOneUnit(String mFrameName, 
			 int[] tokenNums, 
			 String hiddenWord, 
			 String[][] parseData, 
			 WordNetRelations wnr, 
			 DependencyParse parse)
	{
		int[] mTokenNums = tokenNums;
		Arrays.sort(mTokenNums);
		
		String hiddenUnitTokens = "";
		String hiddenUnitLemmas = "";
		
		String actualTokens = "";
		String actualLemmas = "";
		
		String[] hiddenToks = hiddenWord.split(" ");
		FeatureExtractor featex = new FeatureExtractor();
		for(int i = 0; i < hiddenToks.length; i ++)
		{
			String[] arr = hiddenToks[i].split("_");
			hiddenUnitTokens+=arr[0]+" ";
			hiddenUnitLemmas+=featex.getLowerCaseLemma(lemmaCache, arr[0], arr[1], wnr)+" ";
		}
		hiddenUnitTokens=hiddenUnitTokens.trim();
		for(int i = 0; i < mTokenNums.length; i ++)
		{
			String lexUnit = parseData[0][mTokenNums[i]];
			String pos = parseData[1][mTokenNums[i]];	
			actualTokens+=lexUnit+" ";
			actualLemmas+=featex.getLowerCaseLemma(lemmaCache,lexUnit, pos,wnr)+" ";
	
		}	
		actualTokens=actualTokens.trim();
		featex.getWNRelations(cache,hiddenUnitTokens, actualTokens,wnr);	
	}	
	
	
	private void processLine(String line, int index)
	{
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
		String[][] data = new String[5][tokensInFirstSent];
		for(int k = 0; k < 5; k ++)
		{
			data[k]=new String[tokensInFirstSent];
			for(int j = 0; j < tokensInFirstSent; j ++)
			{
				data[k][j]=""+st.nextToken().trim();
			}
		}
		Set<String> set = mFrameMap.keySet();
		for(String f:set)
		{
			updateCache(f,intTokNums,data);
			System.out.print(".");
		}
		System.out.println();
	}
	
}
