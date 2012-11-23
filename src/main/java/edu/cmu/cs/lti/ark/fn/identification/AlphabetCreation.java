/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * AlphabetCreation.java is part of SEMAFOR 2.0.
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
import gnu.trove.*;

public class AlphabetCreation	
{
	private THashMap<String, THashSet<String>> mFrameMap=null;
	private WordNetRelations mWnr = null;
	private ArrayList<String> mListOfParses = null;
	private String mAlphabetFile = null;
	private String mEventDir = null;
	private String mFrameElementsFile = null;
	private THashMap<String,Integer> alphabet = new THashMap<String,Integer>(new TObjectIdentityHashingStrategy<String>());
	private THashMap<String,THashSet<String>> relCache = null;
	private THashMap<String,String> lemmaCache = null;
	
	private THashMap<String, THashSet<String>> clusterMap = null;
	private int K = -1;
	
	public static void main(String[] args)
	{
		FNModelOptions options = new FNModelOptions(args);
		String alphabetFile=options.modelFile.get();
		String eventDir = options.eventsFile.get();
		String feFile = options.trainFrameElementFile.get();
		ArrayList<String> parses = ParsePreparation.readSentencesFromFile(options.trainParseFile.get());
		THashMap<String, THashSet<String>> frameMap = (THashMap<String, THashSet<String>>)SerializedObjects.readSerializedObject(options.frameNetMapFile.get());
		WordNetRelations wnr = null;
		THashMap<String,String> lemmaCache = (THashMap<String,String>)SerializedObjects.readSerializedObject(options.lemmaCacheFile.get());
		THashMap<String,THashSet<String>> relCache = (THashMap<String,THashSet<String>>)SerializedObjects.readSerializedObject(options.wnMapFile.get());
		boolean useClusters = options.clusterFeats.get().equals("true");
		THashMap<String, THashSet<String>> clusterMap=null;
		AlphabetCreation alph = new AlphabetCreation(alphabetFile,eventDir,feFile,parses,frameMap,wnr,lemmaCache,relCache);
		if(useClusters)
		{
			clusterMap= (THashMap<String, THashSet<String>>)SerializedObjects.readSerializedObject(options.synClusterMap.get());
			int K = options.clusterK.get();
			alph.runAlphabetCreation(clusterMap,K);
		}
		else
			alph.runAlphabetCreation();
	} 
	
	public AlphabetCreation(String alphabetFile,
							String eventDir,
							String frameElementsFile,
							ArrayList<String> parses, 
							THashMap<String, THashSet<String>> frameMap, 
							WordNetRelations wnr,
							THashMap<String,String> lemmaCache,
							THashMap<String,THashSet<String>> relCache)
	{
		mWnr = wnr;
		mFrameMap=frameMap;
		mListOfParses=parses;
		mFrameElementsFile=frameElementsFile;
		mEventDir=eventDir;
		mAlphabetFile=alphabetFile;	
		alphabet = new THashMap<String,Integer>();
		this.lemmaCache=lemmaCache;
		this.relCache=relCache;
	}
	
	public void runAlphabetCreation()
	{
		System.out.println("Creating alphabet....");
		try
		{
			BufferedReader bReader = new BufferedReader(new FileReader(mFrameElementsFile));
			String line = null;
			int count = 0;
			while((line=bReader.readLine())!=null)
			{
				line=line.trim();
				System.out.println("Processing line:"+line);
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
		writeAlphabetFile();
	}	
	
	public void runAlphabetCreation(THashMap<String, THashSet<String>> clusterMap, int K)
	{
		this.clusterMap=clusterMap;
		this.K=K;
		runAlphabetCreation();
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
			if(clusterMap!=null)
				valMap =  featex.extractFeaturesWithClusters(frame,
														  intTokNums, 
														  unit, 
														  data, 
														  mWnr, 
														  "test", 
														  relCache,
														  lemmaCache,
														  parse,clusterMap,K);
			else
				valMap =  featex.extractFeatures(frame,
						  intTokNums, 
						  unit, 
						  data, 
						  mWnr, 
						  "test", 
						  relCache,
						  lemmaCache,
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
