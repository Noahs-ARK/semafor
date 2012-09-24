/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * CreateEventsUnsupported.java is part of SEMAFOR 2.0.
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
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.fn.utils.ThreadPool;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.BasicFileIO;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import gnu.trove.*;

public class CreateEventsUnsupported	
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
	private Logger mLogger = null; 
	private int mNumThreads = 0;
	private Map<String, Map<String, Set<String>>> mRevisedRelationsMap;
	private Map<String, String> mHVLemmas;

	public static void main(String[] args)
	{
		FNModelOptions options = new FNModelOptions(args);
		boolean append = true;
		String logoutputfile = options.logOutputFile.get();
		FileHandler fh = null;
		LogManager logManager = LogManager.getLogManager();
		logManager.reset();
		Logger logger = null;
		try {
			fh = new FileHandler(logoutputfile, append);
			fh.setFormatter(new SimpleFormatter());
			logger = Logger.getLogger("CreateEvents");
			logger.addHandler(fh);   
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		String alphabetFile=options.modelFile.get();
		String eventDir = options.eventsFile.get();
		String feFile = options.trainFrameElementFile.get();
		RequiredDataForFrameIdentification r = (RequiredDataForFrameIdentification)SerializedObjects.readSerializedObject(options.fnIdReqDataFile.get());
		Map<String, Set<String>> relatedWordsForWord = r.getRelatedWordsForWord();
		THashMap<String,THashSet<String>> frameMap = r.getFrameMap();
		int startIndex = options.startIndex.get();
		int endIndex = options.endIndex.get();
		int revisedStart = startIndex;
		int revisedEnd = endIndex;
		Map<String, Map<String, Set<String>>> revisedRelationsMap = 
			r.getRevisedRelMap();
		Map<String, String> hvLemmas = r.getHvLemmaCache();
		logger.info("Start:"+ revisedStart + " end:" + revisedEnd);
			CreateEventsUnsupported events = 
				new CreateEventsUnsupported(alphabetFile, 
						eventDir, 
						feFile, 
						options.trainParseFile.get(), 
						frameMap, 
						relatedWordsForWord,
						revisedStart,
						revisedEnd,
						logger,
						options.numThreads.get(),
						revisedRelationsMap,
						hvLemmas);
			events.createEvents();
	} 

	public CreateEventsUnsupported(String alphabetFile,
			String eventDir,
			String frameElementsFile,
			String parseFile, 
			THashMap<String, THashSet<String>> frameMap, 
			Map<String, Set<String>> relatedWordsForWord,
			int startIndex, 
			int endIndex,
			Logger logger,
			int numThreads,
			Map<String, Map<String, Set<String>>> rMap,
			Map<String, String> lemmaCache
			)
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
		mLogger = logger;
		mNumThreads = numThreads; 
		mHVLemmas = lemmaCache;
		mRevisedRelationsMap = rMap;
	}
	
	public void createEvents() {
		try {
			readAlphabetFile();
			ThreadPool threadPool = new ThreadPool(mNumThreads);
			int batchSize = 10;
			int count = 0;
			for (int i = mStartIndex; i < mEndIndex; i = i + batchSize) {
				threadPool.runTask(createTask(count, i, i + batchSize));
				count++;
			}
			threadPool.join(); 
		} catch (Exception e) {
			e.printStackTrace();
			mLogger.severe("Problem in running thread. Exiting.");
			System.exit(-1);
		}
	}
	
	public void processBatch(int i, int start, int end)
	{
		mLogger.info("Thread " + i +": Creating events....");
		if (end > mEndIndex) {
			end = mEndIndex;
		}
		mLogger.info("Thread " + i + ": start:" + start +" end:" + end);
		try
		{
			BufferedReader bReader = 
				new BufferedReader(new FileReader(mFrameElementsFile));
			String line = null;
			int count = 0;
			BufferedReader parseReader = 
				new BufferedReader(new FileReader(mParseFile));
			String parseLine = parseReader.readLine();
			int parseOffset = 0;
			while((line=bReader.readLine())!=null)
			{
				if (count < start) {// skip frame elements prior to the specified range
					count++;
					continue;
				}
				line=line.trim();
				mLogger.info("Thread + " + i + ": Processing:"+count);
				Pair<String, Integer> pair = 
					processLine(line, count, parseLine, parseOffset, parseReader);
				count++;
				if (count == end) {
					break;
				}
				parseLine = pair.getFirst();
				parseOffset = pair.getSecond();
			}
			bReader.close();
			parseReader.close();
		}
		catch(Exception e)
		{
			System.out.println("Problem in reading fe file. exiting..");
			System.exit(0);
		}
	}

	public Runnable createTask(final int count, final int start, final int end)
	{
		return new Runnable() {
		      public void run() {
		        mLogger.info("Task " + count + " : start");
		        processBatch(count, start, end);
		        mLogger.info("Task " + count + " : end");
		      }
		    };
	}
	
	private void readAlphabetFile()
	{
		BufferedReader bReader = BasicFileIO.openFileToRead(mAlphabetFile);
		alphabet = new THashMap<String,Integer>();
		int num = new Integer(BasicFileIO.getLine(bReader));
		for (int i = 0; i < num; i ++) {
			String line = BasicFileIO.getLine(bReader);
			line = line.trim();
			String[] toks = line.split("\t");
			alphabet.put(toks[0], new Integer(toks[1]));
		}
		BasicFileIO.closeFileAlreadyRead(bReader);
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
					for(int i = 0; i < val; i ++)
					{
						feats.add(featIndex);
					}
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

	private Pair<String, Integer> processLine(String line, int index, 
			String parseLine, int parseOffset, BufferedReader parseReader ) {
		String[] toks = line.split("\t");
		int sentNum = new Integer(toks[5]);
		while (parseOffset < sentNum) {
			parseLine = BasicFileIO.getLine(parseReader);
			parseOffset++;
		}
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
		allFeatures[0]=getFeatures(frameName,intTokNums,data);
		int count = 1;
		for(String f:set)
		{
			if(f.equals(frameName))
				continue;
			allFeatures[count]=getFeatures(f,intTokNums,data);
			count++;
		}
		String file = mEventDir+"/feats_"+index+".jobj.gz";
		SerializedObjects.writeSerializedObject(allFeatures, file);
		mLogger.info("Processed index:"+index+" alphsize:"+alphabet.size());
		return new Pair<String, Integer>(parseLine, parseOffset);
	}	
}
