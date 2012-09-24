/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * FrameIdentificationReleaseMulticore.java is part of SEMAFOR 2.0.
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.zip.GZIPInputStream;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.fn.evaluation.ParseUtils;
import edu.cmu.cs.lti.ark.fn.segmentation.MoreRelaxedSegmenter;
import edu.cmu.cs.lti.ark.fn.segmentation.RoteSegmenter;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.fn.utils.ThreadPool;
import edu.cmu.cs.lti.ark.util.CommandLineOptions;
import edu.cmu.cs.lti.ark.util.optimization.LDouble;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectDoubleHashMap;


public class FrameIdentificationReleaseMulticore
{
	public static Logger logger = null;
	
	public static void main(String[] args)
	{
		FNModelOptions options = new FNModelOptions(args);
		options.ensurePresenceOrQuit(new CommandLineOptions.Option[]{options.startIndex, options.endIndex,
				options.testParseFile, options.testTokenizedFile, options.fnIdReqDataFile, 
				options.idParamFile, options.frameElementsOutputFile, options.logOutputFile});
		LogManager logManager = LogManager.getLogManager();
		logManager.reset();
		FileHandler fh = null;
		String logoutputfile = options.logOutputFile.get();
		logger = null;
		try {
       	 fh = new FileHandler(logoutputfile, true);
       	 fh.setFormatter(new SimpleFormatter());
            logger = Logger.getLogger("FNIDOnUnlabeledData");
            logger.addHandler(fh);   
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		boolean printconf = options.printFNIDConfidence.get().equals("yes");
		int start = options.startIndex.get();
		int end = options.endIndex.get();
		logger.info("Start:" + start);
		logger.info("End:" + end);
		ArrayList<String> parses = new ArrayList<String>();
		int count = 0;
		ArrayList<String> tokenNums = new ArrayList<String>();
		ArrayList<String> orgSentenceLines = new ArrayList<String>();
		ArrayList<String> originalIndices = new ArrayList<String>();
		try
		{
			BufferedReader inParses = new BufferedReader(new FileReader(options.testParseFile.get()));
			BufferedReader inOrgSentences = new BufferedReader(new FileReader(options.testTokenizedFile.get()));
			String line = null;
			int dummy = 0;
			while((line=inParses.readLine())!=null)
			{
				String line2 = inOrgSentences.readLine().trim();
				if(count<start)	// skip sentences prior to the specified range
				{
					count++;
					continue;
				}
				parses.add(line.trim());
				orgSentenceLines.add(line2);
				tokenNums.add(""+dummy);	// ?? I think 'dummy' is just the offset of the sentence relative to options.startIndex
				originalIndices.add(""+count);
				if(count==(end-1))	// skip sentences after the specified range
					break;
				count++;
				dummy++;
			}				
			inParses.close();
			inOrgSentences.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}		
		RequiredDataForFrameIdentification r = 
			(RequiredDataForFrameIdentification)readSerializedObject(options.fnIdReqDataFile.get());
		THashSet<String> allRelatedWords = r.getAllRelatedWords();
		Map<String, Set<String>> relatedWordsForWord = r.getRelatedWordsForWord();
		THashMap<String,THashSet<String>> frameMap = r.getFrameMap();
		THashMap<String,THashSet<String>> cMap = r.getcMap();			
		Map<String, Map<String, Set<String>>> revisedRelationsMap = 
			r.getRevisedRelMap();
		boolean useRelaxed = options.useRelaxedSegmentation.get().equals("yes");
		ArrayList<String> segs = null;
		if (!useRelaxed) {
			RoteSegmenter seg = new RoteSegmenter();
			segs = seg.findSegmentationForTest(tokenNums, parses, allRelatedWords);
		} else {
			MoreRelaxedSegmenter seg = new MoreRelaxedSegmenter();
			segs = seg.findSegmentationForTest(tokenNums, parses, allRelatedWords);
		}
		ArrayList<String> inputForFrameId = ParseUtils.getRightInputForFrameIdentification(segs);	// Null\tTargetTokenNum(s)\tSentenceOffset
		TObjectDoubleHashMap<String> paramList = parseParamFile(options.idParamFile.get());
		Map<String, String> hvLemmas = r.getHvLemmaCache();
		FastFrameIdentifier idModel = new FastFrameIdentifier(
				paramList, 
				"reg", 
				0.0, 
				frameMap, 
				null, 
				cMap,
				relatedWordsForWord,
				revisedRelationsMap,
				hvLemmas);
		boolean useClusters = options.clusterFeats.get().equals("true");
		if(useClusters)
		{
			THashMap<String, THashSet<String>> clusterMap=
				(THashMap<String, THashSet<String>>)readSerializedObject(options.synClusterMap.get());
			int K = options.clusterK.get();
			idModel.setClusterInfo(clusterMap,K);
		}
		int numInputs = inputForFrameId.size();
		int threadBatchSize = 10;
		int mNumThreads = options.numThreads.get();
		ThreadPool threadPool = new ThreadPool(mNumThreads);
		for(int k = 0; k < numInputs; k = k + threadBatchSize)
		{
				int s = k;
				int e = k + threadBatchSize;
				if (e > numInputs) {
					e = numInputs;
				}
				threadPool.runTask(createTask(
									count, 
									s, e, 
									inputForFrameId, 
									parses,
									originalIndices,
									printconf,
									idModel,
									options.frameElementsOutputFile.get()));
				count++;
		}
		threadPool.join();
		logger.info("Done: " + start + " " + end);		
	}	
	
	public static void processBatch(int count, 
			   int start, 
			   int end,
			   ArrayList<String> inputForFrameId,
			   ArrayList<String> parses,
			   ArrayList<String> originalIndices,
			   boolean printconf,
			   FastFrameIdentifier idModel,
			   String outFile) {
		ArrayList<String> idResult = new ArrayList<String>();
		for (int i = start; i < end; i++) {
			String input = inputForFrameId.get(i);
			logger.info(input);
			String[] toks = input.split("\t");
			int sentNum = new Integer(toks[2]);	// offset of the sentence within the loaded data (relative to options.startIndex)
			if (printconf) {
				String[] bestFrames = idModel.getBestFrame(input,parses.get(sentNum), printconf);
				for (String bestFrame: bestFrames) {
					String tokenRepresentation = getTokenRepresentation(toks[1],parses.get(sentNum));  
					String[] split = tokenRepresentation.trim().split("\t");
					String sentCount = originalIndices.get(sentNum);
					idResult.add(1+"\t"+bestFrame+"\t"+split[0]+"\t"+toks[1]+"\t"+split[1]+"\t"+sentCount);	// BestFrame\tTargetTokenNum(s)\tSentenceOffset
				}
			} else {
				String bestFrame = idModel.getBestFrame(input,parses.get(sentNum));
				String tokenRepresentation = getTokenRepresentation(toks[1],parses.get(sentNum));  
				String[] split = tokenRepresentation.trim().split("\t");
				String sentCount = originalIndices.get(sentNum);
				idResult.add(1+"\t"+bestFrame+"\t"+split[0]+"\t"+toks[1]+"\t"+split[1]+"\t"+sentCount);	// BestFrame\tTargetTokenNum(s)\tSentenceOffset
			}
		}
		ParsePreparation.writeSentencesToTempFile(outFile + "_"+start+"_"+end, idResult);
	}
	
	
	public static Runnable createTask(final int count, 
							   final int start, 
							   final int end,
							   final ArrayList<String> inputForFrameId,
							   final ArrayList<String> parses,
							   final ArrayList<String> originalIndices,
							   final boolean printconf,
							   final FastFrameIdentifier idModel,
							   final String outFile)
	{
		return new Runnable() {
		      public void run() {
		        logger.info("Task " + count + " : start");
		        processBatch(count, start, end, inputForFrameId, 
						parses,
						originalIndices,
						printconf,
						idModel,
						outFile);
		        logger.info("Task " + count + " : end");
		      }
		};
	}	
	
	public static TObjectDoubleHashMap<String> parseParamFile(String paramsFile)
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
	
	private static String getTokenRepresentation(String tokNum, String parse)
	{
		StringTokenizer st = new StringTokenizer(parse,"\t");
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
		String[] tokNums = tokNum.split("_");
		int[] intTokNums = new int[tokNums.length];
		for(int j = 0; j < tokNums.length; j ++)
			intTokNums[j] = new Integer(tokNums[j]);
		Arrays.sort(intTokNums);
		
		String actualTokens = "";
		String firstTok = "";
		for(int i = 0; i < intTokNums.length; i ++)
		{
			String lexUnit = data[0][intTokNums[i]];
			String pos = data[1][intTokNums[i]];	
			actualTokens+=lexUnit+" ";
			if(i==0)
				firstTok =  lexUnit.toLowerCase()+"."+pos.substring(0,1).toLowerCase();
		}
		actualTokens=actualTokens.trim();
		firstTok=firstTok.trim();
		
		return firstTok+"\t"+actualTokens;
	}
	
	public static Object readSerializedObject(String inputFile)
	{
		ObjectInput input = null;
		Object recoveredObject=null;
		try{
			//use buffering
			InputStream file = null;
			if (inputFile.endsWith(".gz")) {
				file = new GZIPInputStream(new FileInputStream(inputFile));
			} else {
				file = new FileInputStream(inputFile);
			}
			InputStream buffer = new BufferedInputStream(file);
			input = new ObjectInputStream(buffer);
			//deserialize the List
			recoveredObject = input.readObject();
		}
		catch(IOException ex){
			ex.printStackTrace();
		}
		catch (ClassNotFoundException ex){
			ex.printStackTrace();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		finally{
			try {
				if ( input != null ) {
					//close "input" and its underlying streams
					input.close();
				}
			}
			catch (IOException ex){
				ex.printStackTrace();
			}
		}
		return recoveredObject;
	}
}
