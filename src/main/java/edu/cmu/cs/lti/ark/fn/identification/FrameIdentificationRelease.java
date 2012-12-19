/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * FrameIdentificationRelease.java is part of SEMAFOR 2.0.
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
import java.util.*;


import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Token;
import edu.cmu.cs.lti.ark.fn.evaluation.ParseUtils;
import edu.cmu.cs.lti.ark.fn.segmentation.MoreRelaxedSegmenter;
import edu.cmu.cs.lti.ark.fn.segmentation.RoteSegmenter;
import edu.cmu.cs.lti.ark.fn.segmentation.Segmenter;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.CommandLineOptions;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.optimization.LDouble;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectDoubleHashMap;

import static com.google.common.base.Strings.nullToEmpty;


public class FrameIdentificationRelease
{
	public static void main(String[] args)
	{
		FNModelOptions options = new FNModelOptions(args);
		options.ensurePresenceOrQuit(new CommandLineOptions.Option[]{options.startIndex, options.endIndex,
				options.testParseFile, options.testTokenizedFile, options.fnIdReqDataFile, 
				options.stopWordsFile, options.wnConfigFile,
				options.idParamFile, options.frameElementsOutputFile, options.printFNIDConfidence});
		boolean printconf = options.printFNIDConfidence.get().equals("yes");
		ArrayList<String> parses = new ArrayList<String>();
		System.out.println("Start Time:"+(new Date()));
		int start = options.startIndex.get();
		int end = options.endIndex.get();
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
		RequiredDataForFrameIdentification r = (RequiredDataForFrameIdentification)SerializedObjects.readSerializedObject(options.fnIdReqDataFile.get());
		THashSet<String> allRelatedWords = r.getAllRelatedWords();
		Map<String, Set<String>> relatedWordsForWord = r.getRelatedWordsForWord();
		Map<String, THashMap<String, Set<String>>> wordNetMap = r.getWordNetMap();
		THashMap<String,THashSet<String>> frameMap = r.getFrameMap();
		THashMap<String,THashSet<String>> cMap = r.getcMap();			
		Map<String, Map<String, Set<String>>> revisedRelationsMap = 
			r.getRevisedRelMap();
		WordNetRelations mWNR = new WordNetRelations(options.stopWordsFile.get(), options.wnConfigFile.get());
		mWNR.setRelatedWordsForWord(relatedWordsForWord);
		mWNR.setWordNetMap(wordNetMap);
		
		
		boolean useRelaxed = options.useRelaxedSegmentation.get().equals("yes");
		List<String> segs = null;
		if (!useRelaxed) {
			RoteSegmenter seg = new RoteSegmenter(allRelatedWords);
			segs = seg.getSegmentations(tokenNums, parses, allRelatedWords);
		} else {
			Segmenter seg = new MoreRelaxedSegmenter();
			segs = seg.getSegmentations(tokenNums, parses, allRelatedWords);
		}
		ArrayList<String> inputForFrameId = ParseUtils.getRightInputForFrameIdentification(segs);	// Null\tTargetTokenNum(s)\tSentenceOffset
		ArrayList<String> idResult = new ArrayList<String>();
		TObjectDoubleHashMap<String> paramList = parseParamFile(options.idParamFile.get());
		
		Map<String, String> hvLemmas = r.getHvLemmaCache();
		FastFrameIdentifier idModel = new FastFrameIdentifier(
				paramList, 
				"reg", 
				0.0, 
				frameMap,
				cMap,
				relatedWordsForWord,
				revisedRelationsMap,
				hvLemmas);
		System.out.println("Size of originalSentences list:"+originalIndices.size());
		
		boolean useClusters = options.clusterFeats.get().equals("true");
		if(useClusters)
		{
			THashMap<String, THashSet<String>> clusterMap= (THashMap<String, THashSet<String>>)SerializedObjects.readSerializedObject(options.synClusterMap.get());
			int K = options.clusterK.get();
			idModel.setClusterInfo(clusterMap,K);
		}
		
		for(String input: inputForFrameId)
		{
			String[] toks = input.split("\t");
			int sentNum = new Integer(toks[2]);	// offset of the sentence within the loaded data (relative to options.startIndex)
			if (printconf) {
				String[] bestFrames = idModel.getBestFrame(input,parses.get(sentNum), printconf);
				for (String bestFrame: bestFrames) {
					String tokenRepresentation = getTokenRepresentation(toks[1],parses.get(sentNum));  
					String[] split = tokenRepresentation.trim().split("\t");
					String sentCount = originalIndices.get(sentNum);
					idResult.add(1+"\t"+bestFrame+"\t"+split[0]+"\t"+toks[1]+"\t"+split[1]+"\t"+sentCount);	// BestFrame\tTargetTokenNum(s)\tSentenceOffset
					System.out.println("1"+"\t"+bestFrame+"\t"+split[0]+"\t"+toks[1]+"\t"+split[1]+"\t"+sentCount);
				}
			} else {
				String bestFrame = idModel.getBestFrame(input,parses.get(sentNum));
				String tokenRepresentation = getTokenRepresentation(toks[1],parses.get(sentNum));  
				String[] split = tokenRepresentation.trim().split("\t");
				String sentCount = originalIndices.get(sentNum);
				idResult.add(1+"\t"+bestFrame+"\t"+split[0]+"\t"+toks[1]+"\t"+split[1]+"\t"+sentCount);	// BestFrame\tTargetTokenNum(s)\tSentenceOffset
				System.out.println("1"+"\t"+bestFrame+"\t"+split[0]+"\t"+toks[1]+"\t"+split[1]+"\t"+sentCount);
			}
			
		}
		String feFile = options.frameElementsOutputFile.get();
		ParsePreparation.writeSentencesToFile(feFile, idResult);
		System.out.println("End Time:"+(new Date()));
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
	
	public static String getTokenRepresentation(String tokNum, String parse) {
		String[] tokNums = tokNum.split("_");
		List<Integer> indices = Lists.newArrayList();
		for (String tokNum1 : tokNums) {
			indices.add(Integer.parseInt(tokNum1));
		}
		return getTokenRepresentation(indices, Sentence.fromAllLemmaTagsArray(AllLemmaTags.readLine(parse)));
	}

	public static String getTokenRepresentation(List<Integer> indices, Sentence sentence) {
		final List<Token> tokens = sentence.getTokens();
		if(indices.isEmpty()) return "\t";
		final Token firstToken = tokens.get(indices.get(0));
		String firstTok = firstToken.getForm().toLowerCase() + "." +
				nullToEmpty(firstToken.getPostag()).substring(0, 1).toLowerCase();
		List<String> actualTokens = Lists.newArrayList();
		for (int i : indices) {
			actualTokens.add(tokens.get(i).getForm());
		}
		return firstTok + "\t" + Joiner.on(" ").join(actualTokens);
	}
}
