/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * WordNetRelationsCache.java is part of SEMAFOR 2.0.
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
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.SerializedObjects;

import gnu.trove.THashMap;
import gnu.trove.THashSet;
	
public class WordNetRelationsCache
{
	public static void main(String[] args)
	{
		//verifyMap();
		//createMap();
		combineMaps(args);
	}	
	
	public static void combineMaps(String[] args)
	{
		String directory = args[0];
		/*
		 * unifying the lemma cache
		 */
		THashMap<String,String> lemmaCache = new THashMap<String,String>();
		File f = new File(directory);
		FilenameFilter lFilter = new FilenameFilter(){
			public boolean accept(File arg0, String arg1) {
				return arg1.startsWith("lemmacache.jobj_");
			}
		};
		String[] files = f.list(lFilter);
		for(String file:files)
		{
			file=directory+"/"+file;
			THashMap<String,String> lemmaCache1 = (THashMap<String,String>)SerializedObjects.readSerializedObject(file);
			Set<String> keySet = lemmaCache1.keySet();
			System.out.println("Size of keyset:"+keySet.size());
			for(String key:keySet)
			{
				if(lemmaCache.contains(key))
					continue;
				lemmaCache.put(key, lemmaCache1.get(key));
			}
			System.out.println("Done with:"+file);
		}
		SerializedObjects.writeSerializedObject(lemmaCache, directory+"/lemmaCache.jobj");
		/*
		 * unifying the relations cache
		 */
		THashMap<String,THashSet<String>> relCache = new THashMap<String,THashSet<String>>();
		FilenameFilter cFilter = new FilenameFilter(){
			public boolean accept(File arg0, String arg1) {
				return arg1.startsWith("wnrelmap.jobj_");
			}
		};
		files = f.list(cFilter);
		for(String file:files)
		{
			file=directory+"/"+file;
			THashMap<String,THashSet<String>> relCache1 = (THashMap<String,THashSet<String>>)SerializedObjects.readSerializedObject(file);
			Set<String> keySet = relCache1.keySet();
			System.out.println("Size of keyset:"+keySet.size());
			for(String key:keySet)
			{
				if(relCache.contains(key))
					continue;
				relCache.put(key, relCache1.get(key));
			}
			System.out.println("Done with:"+file);
		}
		SerializedObjects.writeSerializedObject(relCache, directory+"/relCache.jobj");
	}
	
	
	public static void verifyMap()
	{
		String stopWordsFile = "/usr0/dipanjan/work/fall2008/SentenceSentence/QG4Entailment/data/stopwords.txt";
		String wordNetConfigFile = "file_properties.xml";
		WordNetRelations wnr = new WordNetRelations(stopWordsFile, wordNetConfigFile);
		String relationsMapFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/hierWnDevCacheRelations.ser";
		THashMap<String,THashSet<String>> map = (THashMap<String,THashSet<String>>)SerializedObjects.readSerializedObject(relationsMapFile);
		String frameFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/alldev.m45.frames";
		String parsesFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/alldev.m45.parsed";
		String frameMapFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.map";
		ArrayList<String> frames = ParsePreparation.readSentencesFromFile(frameFile);
		ArrayList<String> parses = ParsePreparation.readSentencesFromFile(parsesFile);
		THashMap<String,THashSet<String>> frameMap = (THashMap<String,THashSet<String>>)SerializedObjects.readSerializedObject(frameMapFile); 
		int size = frames.size();
		for(int i = 0; i < size; i ++)
		{
			String frameLine = frames.get(i);
			String[] toks = frameLine.split("\t");
			String frameName = toks[0];
			int sentNum = new Integer(toks[2]);
			String[] tokNums = toks[1].split("_");
			int[] intTokNums = new int[tokNums.length];
			for(int j = 0; j < tokNums.length; j ++)
				intTokNums[j] = new Integer(tokNums[j]);
			Arrays.sort(intTokNums);
			StringTokenizer st = new StringTokenizer(parses.get(sentNum),"\t");
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
			Set<String> frameSet = frameMap.keySet();
			Iterator<String> frameItr = frameSet.iterator();
			while(frameItr.hasNext())
			{
				String frameDashed = frameItr.next();
				findRelationsForFrame(frameMap, frameName, intTokNums, data, wnr, map);
			}
			
		}
	}
	
	private static void findRelationsForFrame(THashMap<String,THashSet<String>> mFrameMap, String frame, int[] tokenNums, String[][] pData, WordNetRelations wnr, THashMap<String,THashSet<String>> wnCacheMap)	
	{
		THashSet<String> hiddenUnits = mFrameMap.get(frame);
		Iterator<String> itr = hiddenUnits.iterator();
		while(itr.hasNext())
		{
			String unit = itr.next();
			String[] hiddenToks = unit.split(" ");
			String hiddenUnitTokens = "";
			for(int i = 0; i < hiddenToks.length; i ++)
			{
				String[] arr = hiddenToks[i].split("_");
				hiddenUnitTokens+=arr[0]+" ";
			}
			hiddenUnitTokens=hiddenUnitTokens.trim().toLowerCase();
			String actualTokens = "";
			for(int i = 0; i < tokenNums.length; i ++)
			{
				String lexUnit = pData[0][tokenNums[i]];
				actualTokens+=lexUnit+" ";	
			}
			actualTokens = actualTokens.trim().toLowerCase();
			String pair = hiddenUnitTokens+"\t"+actualTokens;
			
			if(!wnCacheMap.contains(pair))			
			{
				Set<String> relations = wnr.getRelations(hiddenUnitTokens, actualTokens);
				if(!relations.contains(WordNetRelations.NO_RELATION))
				{
					System.out.println("Problem with pair:"+pair);
					System.exit(0);
				}
			}
			else
			{
				System.out.println("Found relation");
				Set<String> set1 = wnCacheMap.get(pair);
				Set<String> set2 = wnr.getRelations(hiddenUnitTokens, actualTokens);
				if(!set1.equals(set2))
				{
					System.out.println("Problem with pair 2:"+pair);
					System.exit(0);
				}
				System.out.println(pair);
				for(String string:set2)
				{
					System.out.print(string+" ");
				}
				System.out.println();
			}
		}
	}	
	
	
	public static void createMap()
	{
		String textFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/hierWnCache";
		String wnRelationsMapFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/hierWnCacheRelations.ser";
		
		THashMap<String,THashSet<String>> map = new THashMap<String,THashSet<String>>();
		try
		{
			BufferedReader bReader = new BufferedReader(new FileReader(textFile));
			String line = null;
			int count = 0;
			while((line=bReader.readLine())!=null)
			{
				String[] toks = line.split("\t");
				String hiddenWord = toks[0].trim();
				String actualWord = toks[1].trim();
				String relations = toks[2].trim();
				String key = hiddenWord+"\t"+actualWord;
				String[] relationToks = relations.split(" ");
				THashSet<String> relSet = new THashSet<String>();
				for(int i = 0; i < relationToks.length; i ++)
				{
					relSet.add(relationToks[i]);
				}
				map.put(key, relSet);
				if(count%1000==0)
					System.out.println(count);
				count++;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		SerializedObjects.writeSerializedObject(map, wnRelationsMapFile);
	}	
	
}
