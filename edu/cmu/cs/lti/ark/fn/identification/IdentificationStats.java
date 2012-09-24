/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * IdentificationStats.java is part of SEMAFOR 2.0.
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;

public class IdentificationStats
{
	public static void main(String[] args)
	{
		try
		{
			mapReduceDataCreationDev(args);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		//verification();
	}	
	
	public static void verification()
	{
		String parseFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/alltrain.m45.parsed";
		String frameFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/alltrain.m45.frames";
		ArrayList<String> frames = ParsePreparation.readSentencesFromFile(frameFile);
		ArrayList<String> parses = ParsePreparation.readSentencesFromFile(parseFile);
		int size = frames.size();
		for(int i = 139500; i < size; i ++)
		{
			String[] toks = frames.get(i).split("\t");
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
					data[k][j]=""+st.nextToken().trim()+"\t";
				}
			}
			String lexUnit = "";
			for(int j = 0; j < tokNums.length; j ++)
				lexUnit += data[0][intTokNums[j]]+" ";
			lexUnit = lexUnit.trim();
			System.out.println(frameName+"\t"+lexUnit);
		}		
	}		
	
	public static void mapReduceDataCreation(String[] args) throws Exception
	{
		FNModelOptions opts = new FNModelOptions(args);
		ArrayList<String> frameNetParseLines = ParsePreparation.readSentencesFromFile(opts.frameNetParseFile.get());
		int offset = frameNetParseLines.size();
		frameNetParseLines.addAll(ParsePreparation.readSentencesFromFile(opts.trainParseFile.get()));
		ParsePreparation.writeSentencesToTempFile("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/alltrain.m45.parsed", frameNetParseLines);				
		String frameFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/alltrain.m45.frames";
		BufferedWriter bWriter = new BufferedWriter(new FileWriter(frameFile));
		ArrayList<String> frameNetFrameLines = ParsePreparation.readSentencesFromFile(opts.frameNetFrameFile.get());
		int size = frameNetFrameLines.size();
		for(int i = 0; i < size; i ++)
		{
			String frameLine = frameNetFrameLines.get(i);
			String[] toks = frameLine.split("\t");
			bWriter.write(toks[0]+"\t"+toks[2]+"\t"+toks[4]+"\n");
			if(i%100==0)
				System.out.print(".");
			if(i%1000==0)
				System.out.println();
		}
		ArrayList<String> trainFrameLines = ParsePreparation.readSentencesFromFile(opts.trainFrameFile.get());
		int trainFrameLineSize = trainFrameLines.size();
		for(int i = 0; i < trainFrameLineSize; i ++)
		{
			String frameLine = trainFrameLines.get(i);
			String[] toks = frameLine.split("\t");
			int sentNum = new Integer(toks[4].trim());
			sentNum+=offset;
			bWriter.write(toks[0]+"\t"+toks[2]+"\t"+sentNum+"\n");
			if(i%100==0)
				System.out.print(".");
			if(i%1000==0)
				System.out.println();
		}		
		bWriter.close();
	}	
	
	
	public static void mapReduceDataCreationDev(String[] args) throws Exception
	{
		FNModelOptions opts = new FNModelOptions(args);
		ArrayList<String> frameNetParseLines = ParsePreparation.readSentencesFromFile(opts.devParseFile.get());
		ParsePreparation.writeSentencesToTempFile("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/alldev.m45.parsed", frameNetParseLines);				
		String frameFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/alldev.m45.frames";
		BufferedWriter bWriter = new BufferedWriter(new FileWriter(frameFile));
		ArrayList<String> frameNetFrameLines = ParsePreparation.readSentencesFromFile(opts.devFrameFile.get());
		int size = frameNetFrameLines.size();
		for(int i = 0; i < size; i ++)
		{
			String frameLine = frameNetFrameLines.get(i);
			String[] toks = frameLine.split("\t");
			bWriter.write(toks[0]+"\t"+toks[2]+"\t"+toks[4]+"\n");
			if(i%100==0)
				System.out.print(".");
			if(i%1000==0)
				System.out.println();
		}
		bWriter.close();
	}	
	
	public void frameNumber(String[] args)
	{
		FNModelOptions opts = new FNModelOptions(args);
		if(opts.train.get())
		{ 
			ArrayList<String> allFrames = new ArrayList<String>();
			ArrayList<String> allTokens = new ArrayList<String>();
			ArrayList<String> frameNetFrameLines = ParsePreparation.readSentencesFromFile(opts.frameNetFrameFile.get());
			int frameLineSize = frameNetFrameLines.size();
			System.out.println("Reading all data...");
			int count = 0;
			for(int i = 0; i < frameLineSize; i ++)
			{
				String frameLine = frameNetFrameLines.get(i);
				String[] toks = frameLine.split("\t");
				String frame = toks[0];
				String token = toks[3];
				if(!allFrames.contains(frame))
					allFrames.add(frame);
				if(!allTokens.contains(token))
					allTokens.add(token);
				if(count%100==0)
					System.out.print(".");
				count++;
			}
			ArrayList<String> trainFrameLines = ParsePreparation.readSentencesFromFile(opts.trainFrameFile.get());
			int trainFrameLineSize = trainFrameLines.size();
			for(int i = 0; i < trainFrameLineSize; i ++)
			{
				String frameLine = trainFrameLines.get(i);
				String[] toks = frameLine.split("\t");
				String frame = toks[0];
				String token = toks[3];
				if(!allFrames.contains(frame))
					allFrames.add(frame);
				if(!allTokens.contains(token))
					allTokens.add(token);
				if(count%100==0)
					System.out.print(".");
				count++;
			}	
			System.out.println();
			int frameSize = allFrames.size();
			int tokensSize = allTokens.size();
			System.out.println(allFrames.size());
			System.out.println(allTokens.size());
			int product = frameSize * tokensSize;
			System.out.println(product);
		}
		else if(opts.dev.get())
		{
			
		}
		else if(opts.test.get())
		{
			System.out.println("Not handling yet.");
		}	
	}
}
