/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * FrameIdentificationGoldTargets.java is part of SEMAFOR 2.0.
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

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectDoubleHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class FrameIdentificationGoldTargets
{
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		FNModelOptions options = new FNModelOptions(args);
		ArrayList<String> parses = new ArrayList<String>();
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
		RequiredDataForFrameIdentification r = SerializedObjects.readObject(options.fnIdReqDataFile.get());
		THashMap<String,THashSet<String>> frameMap = r.getFrameMap();
		THashMap<String,THashSet<String>> cMap = r.getcMap();

		List<String> segs = ParsePreparation.readLines(options.testFrameFile.get());
		ArrayList<String> inputForFrameId = getGoldSeg(segs,start,end);	// Null\tTargetTokenNum(s)\tSentenceOffset
		ArrayList<String> idResult = new ArrayList<String>();
		final Pair<IdFeatureExtractor,TObjectDoubleHashMap<String>> extractorAndParams =
				FrameIdentificationRelease.parseParamFile(options.idParamFile.get());
		final IdFeatureExtractor featureExtractor = extractorAndParams.first;
		final TObjectDoubleHashMap<String> paramList = extractorAndParams.second;
		System.out.println("Size of originalSentences list:"+originalIndices.size());

		boolean usegraph = !options.useGraph.get().equals("null");
		SmoothedGraph sg = null;
		FastFrameIdentifier idModel;
		if (usegraph) {
			sg = SerializedObjects.readObject(options.useGraph.get());
			idModel = new GraphBasedFrameIdentifier(
					featureExtractor,
					frameMap.keySet(),
					cMap,
					paramList,
					sg);
		} else {
			idModel = new FastFrameIdentifier(
					featureExtractor,
					paramList,
					frameMap.keySet(),
					cMap
			);
		}
		System.out.println("Start Time:"+(new Date()));
		for(String input: inputForFrameId)
		{
			String[] toks = input.split("\t");
			int sentNum = Integer.parseInt(toks[2]);	// offset of the sentence within the loaded data (relative to options.startIndex)
			String bestFrame = idModel.getBestFrame(input, parses.get(sentNum));
			String tokenRepresentation = getTokenRepresentation(toks[1],parses.get(sentNum));  
			String[] split = tokenRepresentation.trim().split("\t");
			String sentCount = originalIndices.get(sentNum);
			idResult.add(1+"\t"+bestFrame+"\t"+split[0]+"\t"+toks[1]+"\t"+split[1]+"\t"+sentCount);	// BestFrame\tTargetTokenNum(s)\tSentenceOffset
			System.out.println("1"+"\t"+bestFrame+"\t"+split[0]+"\t"+toks[1]+"\t"+split[1]+"\t"+sentCount);
		}
		System.out.println("End Time:"+(new Date()));
		String feFile = options.frameElementsOutputFile.get();
		ParsePreparation.writeSentencesToFile(feFile, idResult);
		
	}

	public static ArrayList<String> getGoldSeg(List<String> segs,int start, int end)
	{
		ArrayList<String> result = new ArrayList<String>();
		int count = 0;
		for(String seg:segs)
		{
			String[] toks = seg.split("\t");
			int sentNum = Integer.parseInt(toks[5]);
			if(sentNum<start)
			{
				count++;
				continue;
			}
			if(sentNum>=end)
				break;
			String span = toks[3];
			String line = "Null\t"+span+"\t"+(sentNum-start);
			result.add(line);
			count++;
		}
		return result;
	}
	
	public static String getTokenRepresentation(String tokNum, String parse)
	{
		StringTokenizer st = new StringTokenizer(parse,"\t");
		int tokensInFirstSent = Integer.parseInt(st.nextToken());
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
			intTokNums[j] = Integer.parseInt(tokNums[j]);
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
}
