/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * LexicalUnitsFrameExtraction.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.data.prep;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.Date;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import edu.cmu.cs.lti.ark.fn.constants.FNConstants;
import edu.cmu.cs.lti.ark.fn.data.prep.Frames;
import edu.cmu.cs.lti.ark.fn.data.prep.FramesSemEval;
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithElements;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.XmlUtils;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectIdentityHashingStrategy;


public class LexicalUnitsFrameExtraction
{
	public static int sentenceNum = 0;
	
	public static void main(String[] args)
	{
		
		//matchNumberOfSentences();
		//semEval();
		/*
		try {
			semEvalTest();
		} catch(Exception ex) { ex.printStackTrace(); }
		
		writeMapOfFrameElements();
		*/
		//printMap();
		
		/*
		// Simulate calls to original(), semEvalDev(), and semEvalTrain(), but only output frame element information (not .frames files)
		try
		{
			
			{
				sentenceNum = 0;
				String directory = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/luXML";
				String outFileFEs = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.sentences.frame.elements";
				originalFNFrameElements(directory, outFileFEs);
			}
			
			
			
			{
				sentenceNum = 0;
				BufferedWriter bWriterFEs = new BufferedWriter(new FileWriter("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulldev.sentences.frame.elements"));
				for(int j = 0; j < FramesSemEval.devSet.length; j ++)
				{
					String fileName = FramesSemEval.devSet[j];
					System.out.println("\n\n"+fileName);
					getSemEvalFrameElements(fileName,bWriterFEs);
				}
				bWriterFEs.close();
			}
			
			{
				sentenceNum = 0;
				BufferedWriter bWriterFEs = new BufferedWriter(new FileWriter("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulltrain.sentences.frame.elements"));
				String[] sets = {"ANC","NTI","PropBank"};
				for(int i = 0; i < sets.length; i ++)
				{
					String directory = "/mal2/dipanjan/experiments/FramenetParsing/semeval-2007-task19/train/"+sets[i]; 
					File dir = new File(directory);
					FilenameFilter filter = new FilenameFilter(){
						public boolean accept(File dir, String name) {
							return name.endsWith(".xml");
						}
					};
					String[] fileNames = dir.list(filter);
					int len = fileNames.length;
					for(int j = 0; j < len; j ++)
					{
						String fileName = dir.getAbsolutePath()+"/"+fileNames[j];
						if(Arrays.binarySearch(FramesSemEval.devSet, fileName)>=0)
							continue;
						System.out.println("\n\n"+fileName);
						getSemEvalFrameElements(fileName,bWriterFEs);
					}
				}
				bWriterFEs.close();
			}
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		*/
		long longVal = (new Date()).getTime();
	}
	
	public static void printMap()
	{
		String mapFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.map";
		THashMap<String,THashSet<String>> map = (THashMap<String,THashSet<String>>)SerializedObjects.readSerializedObject(mapFile);
		Set<String> set = map.keySet();
		for(String frame: set)
		{
			THashSet<String> val = map.get(frame);
			System.out.print(frame+": ");
			for(String unit: val)
			{
				System.out.print(unit+" ");
			}
			System.out.println();
		}
	}	
	
	
	public static void semEval()
	{
		try
		{
			semEvalFNUnits();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Populates the given map object with frames (as keys) and sets of target words that evoke 
	 * those frames in the given corresponding sentences (as values)
	 * @param map
	 * @param frames
	 * @param sentences
	 * @author dipanjan
	 */
	public static void fillMap(THashMap<String,THashSet<String>> map, ArrayList<String> frames, ArrayList<String> sentences)
	{
		int size = frames.size();
		for(int i = 0; i < size; i ++)
		{
			String line = frames.get(i);
			String[] toks = line.split("\t");
			int sentenceNum = new Integer(toks[toks.length-1]);
			String storedSentence = sentences.get(sentenceNum);
			String frame = toks[0];
			String tokenNums = toks[2];
			String[] nums = tokenNums.split("_");
			String lexicalUnits = getTokens(storedSentence,nums);
			THashSet<String> set = map.get(frame);	
			if(set==null)
			{
				set = new THashSet<String>();
				set.add(lexicalUnits);
				map.put(frame, set);
			}
			else
			{
				if(!set.contains(lexicalUnits))
				{
					set.add(lexicalUnits);
				}
			}
		}
	}
	
	/**
	 * From flat-text files with sentences and frame occurrence information, 
	 * creates a map from frame names to target words observed as evoking that frame 
	 * and stores that map as a serialized object
	 * @author dipanjan
	 */
	public static void writeMapOfFrames()
	{
		String tokenizedFrameNetFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.sentences.all.tags";
		ArrayList<String> sentences = ParsePreparation.readSentencesFromFile(tokenizedFrameNetFile);
		String frameNetFrameFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.sentences.frames";
		ArrayList<String> frames = ParsePreparation.readSentencesFromFile(frameNetFrameFile);
		THashMap<String,THashSet<String>> map = new THashMap<String,THashSet<String>>();
		fillMap(map,frames,sentences);
				
		tokenizedFrameNetFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulltrain.sentences.all.tags";
		sentences = ParsePreparation.readSentencesFromFile(tokenizedFrameNetFile);
		frameNetFrameFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulltrain.sentences.frames";
		frames = ParsePreparation.readSentencesFromFile(frameNetFrameFile);
		fillMap(map,frames,sentences);		
		
		System.out.println(map.size());
		String mapFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.map";
		SerializedObjects.writeSerializedObject(map, mapFile);
	}
	
	/**
	 * From flat-text files with frame element occurrence information, 
	 * creates a map from frame names to a set of names of frame element 
	 * (argument) roles observed for that frame with overt fillers
	 * and stores that map as a serialized object
	 * @author Nathan Schneider (nschneid)
	 */
	public static void writeMapOfFrameElements() {
		String[] filePrefixes = {"lrdata/framenet.original", 
				"lrdata/semeval.fulltrain",
				"lrdata/semeval.fulldev",
				"lrdata/semeval.fulltest"};
		for (int j=0; j<filePrefixes.length; j++) {	// treat framenet.original, semeval.fulltrain, and semeval.fulldev separately
			String filePrefix = filePrefixes[j];
			
			String trainFrameElementsFile = filePrefix + ".sentences.frame.elements";
			String trainParseFile = filePrefix + ".sentences.all.tags";
			ArrayList<String> frameElementLines = ParsePreparation.readSentencesFromFile(trainFrameElementsFile);
			ArrayList<String> frameParseLines = ParsePreparation.readSentencesFromFile(trainParseFile);
			
			THashMap<String,THashSet<String>> framesToFEs = new THashMap<String,THashSet<String>>(/*new TObjectIdentityHashingStrategy<String>()*/);
			
			for (int l=0; l<frameElementLines.size()-1; l++) {
				String frameElementsLine = frameElementLines.get(l);
				int sentenceNum = Integer.parseInt(frameElementsLine.split("\t")[5]);
				//TODO: above should be = DataPointWithElements.parseFrameNameAndSentenceNum(frameElementsLine).getSecond();
				String frameParseLine = frameParseLines.get(sentenceNum);
				DataPointWithElements dp = new DataPointWithElements(frameParseLine, frameElementsLine);
				
				if (!framesToFEs.containsKey(dp.getFrameName())) {
					framesToFEs.put(dp.getFrameName(), new THashSet<String>(/*new TObjectIdentityHashingStrategy<String>()*/));
				}
				String[] feNames = dp.getOvertFilledFrameElementNames();
				for (String feName : feNames)
					framesToFEs.get(dp.getFrameName()).add(feName);
			}
			
			System.out.println(framesToFEs.size());
			String mapFile = filePrefix + ".frame.elements.map";
			SerializedObjects.writeSerializedObject(framesToFEs, mapFile);
		}
	}
	
	
	public static void matchNumberOfSentences()
	{
		String luFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.sentences.frames";
		String sentencesFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.sentences.tokenized";
	
		ArrayList<String> luSentences = ParsePreparation.readSentencesFromFile(luFile);
		ArrayList<String> sentences = ParsePreparation.readSentencesFromFile(sentencesFile);
		
		int size = luSentences.size();
		for(int i = 0; i < size; i ++)
		{
			String[] tokens = luSentences.get(i).split("\t");
			int sentenceNum = new Integer(tokens[tokens.length-1]);
			String storedSentence = sentences.get(sentenceNum);
			String tokenNums = tokens[tokens.length-3];
			String[] nums = tokenNums.split("_");
			String lexicalUnitOne = tokens[tokens.length-2];
			String lexicalUnitTwo = getTokens(storedSentence,nums);
			//System.out.println(lexicalUnitOne+"\t"+lexicalUnitTwo);
			if(!lexicalUnitOne.equals(lexicalUnitTwo))
			{
				System.out.println(lexicalUnitOne+"\t"+lexicalUnitTwo);
			}
		}
	}	
	
	public static String getTokens(String sentence, String[] nums)
	{
		StringTokenizer st = new StringTokenizer(sentence,"\t");
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
		int[] intNums = new int[nums.length];
		for(int i = 0; i < nums.length; i ++)
		{
			intNums[i] = new Integer(nums[i]);
		}
		String result="";
		Arrays.sort(intNums);
		for(int j = 0; j < intNums.length; j ++)
		{
			String token = data[0][intNums[j]];
			String POS = data[1][intNums[j]];
			result+=token+"_"+POS+" ";
		}
		return result.trim();
	}	
	
	public static void original()
	{
		String directory = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/luXML";
		String outFileFrames = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.sentences.frames";
		String outFileFEs = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.sentences.frame.elements";
		try
		{
			originalFNUnits(directory, outFileFrames);
			originalFNFrameElements(directory, outFileFEs);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}	
	}
	
	public static void semEvalFNUnits() throws Exception
	{
		semEvalDev();
	}
	
	public static void semEvalDev() throws Exception
	{
		Arrays.sort(FramesSemEval.devSet);
		sentenceNum = 0;
		BufferedWriter bWriterFrames = new BufferedWriter(new FileWriter("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulldev.sentences.frames"));
		BufferedWriter bWriterFEs = new BufferedWriter(new FileWriter("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulldev.sentences.frame.elements"));
		for(int j = 0; j < FramesSemEval.devSet.length; j ++)
		{
			String fileName = FramesSemEval.devSet[j];
			System.out.println("\n\n"+fileName);
			getSemEvalFrames(fileName,bWriterFrames);
			getSemEvalFrameElements(fileName,bWriterFEs);
		}		
		bWriterFrames.close();
		bWriterFEs.close();
	}	
	
	public static void semEvalTest() throws Exception
	{
		Arrays.sort(FramesSemEval.testSet);
		sentenceNum = 0;
		BufferedWriter bWriterFrames = new BufferedWriter(new FileWriter("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulltest.sentences.frames"));
		BufferedWriter bWriterFEs = new BufferedWriter(new FileWriter("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulltest.sentences.frame.elements"));
		for(int j = 0; j < FramesSemEval.testSet.length; j ++)
		{
			String fileName = FramesSemEval.testSet[j];
			System.out.println("\n\n"+fileName);
			getSemEvalFrames(fileName,bWriterFrames);
			getSemEvalFrameElements(fileName,bWriterFEs);
		}		
		bWriterFrames.close();
		bWriterFEs.close();
	}	
	
	public static void semEvalTrain() throws Exception
	{
		Arrays.sort(FramesSemEval.devSet);
		sentenceNum = 0;
		BufferedWriter bWriterFrames = new BufferedWriter(new FileWriter("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulltrain.sentences.frames"));
		BufferedWriter bWriterFEs = new BufferedWriter(new FileWriter("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulltrain.sentences.frame.elements"));
		String[] sets = {"ANC","NTI","PropBank"};
		for(int i = 0; i < sets.length; i ++)
		{
			String directory = "/mal2/dipanjan/experiments/FramenetParsing/semeval-2007-task19/train/"+sets[i]; 
			File dir = new File(directory);
			FilenameFilter filter = new FilenameFilter(){
				public boolean accept(File dir, String name) {
					return name.endsWith(".xml");
				}
			};
			String[] fileNames = dir.list(filter);
			int len = fileNames.length;
			for(int j = 0; j < len; j ++)
			{
				String fileName = dir.getAbsolutePath()+"/"+fileNames[j];
				if(Arrays.binarySearch(FramesSemEval.devSet, fileName)>=0)	// make sure this document isn't part of the dev set
					continue;
				System.out.println("\n\n"+fileName);
				getSemEvalFrames(fileName,bWriterFrames);
				getSemEvalFrameElements(fileName,bWriterFEs);
			}
		}
		bWriterFrames.close();
		bWriterFEs.close();
	}
	
	/**
	 * Lists frame targets and corresponding frames from annotated Semeval data
	 * @param fileName
	 * @param bWriter
	 * @throws Exception
	 * @author dipanjan
	 * @see originalFNUnits()
	 */
	public static void getSemEvalFrames(String fileName, BufferedWriter bWriter) throws Exception
	{
		Document d = XmlUtils.parseXmlFile(fileName, false);
		String xPath = "/corpus/documents/document/paragraphs/paragraph/sentences/sentence/text";
		Element[] luList = XmlUtils.applyXPath(d,xPath);
		
		for(int i = 0; i < luList.length; i ++)
		{
			String text = luList[i].getTextContent();
			System.out.println(text);
			Node parent = luList[i].getParentNode();
			NodeList children = parent.getChildNodes();
			int len = children.getLength();
			String frameName=null;
			ArrayList<Node> frameAnnotations = new ArrayList<Node>();
			for(int j = 0; j < len; j ++)
			{
				Node n2 = children.item(j);
				if(!n2.getNodeName().equals("annotationSets"))
					continue;
				
				NodeList annotations = n2.getChildNodes();
				
				for(int k = 0; k < annotations.getLength(); k ++)
				{
					Node n3 = annotations.item(k);
					if(!n3.getNodeName().equals("annotationSet"))
						continue;
					frameName = ((Element)n3).getAttribute("frameName").trim();
					if(!frameName.equals(""))
					{
						frameAnnotations.add(n3);
					}
				}
			}
			if(frameAnnotations.size()==0)
			{
				System.out.print("NULL\tNULL\tNULL\tNULL\n");
				sentenceNum++;
				continue;
			}			
			int annotationSize = frameAnnotations.size();
			for(int j = 0; j < annotationSize; j ++)
			{
				Node fnNode = frameAnnotations.get(j);
				String luName = ((Element)fnNode).getAttribute("luName");
				String localFrameName = ((Element)fnNode).getAttribute("frameName");
				NodeList layerss = fnNode.getChildNodes();
				for(int k = 0; k < layerss.getLength(); k ++)
				{
					Node n4 = layerss.item(k);
					if(!n4.getNodeName().equals("layers"))
						continue;
					NodeList layers = n4.getChildNodes();
					for(int l = 0; l < layers.getLength(); l ++)
					{
						Node n5 = layers.item(l);
						if(!n5.getNodeName().equals("layer"))
							continue;
						String n5Name = ((Element)n5).getAttribute("name");
						if(!n5Name.equals("Target"))
							continue;
						int start = -1;
						int end = -1;
						try
						{
							start = LexicalUnitsFrameExtraction.getStart((Element)n5);
							end = LexicalUnitsFrameExtraction.getEnd((Element)n5);
						}
						catch(Exception e)
						{
							e.printStackTrace();
							System.out.println("Problem. Skipping. Text: "+text);
							//System.out.print("NULL\tNULL\tNULL\tNULL\n");
							break;
						}
						String numRep = getTokenNumberRepresentation(text,start,end);
						System.out.println(localFrameName+"\t"+luName+"\t"+numRep+"\t"+text.substring(start,end+1)+"\t"+sentenceNum+"\n");
						bWriter.write(localFrameName+"\t"+luName+"\t"+numRep+"\t"+text.substring(start,end+1)+"\t"+sentenceNum+"\n");
						break;
					}
				}
			}
			System.out.println(sentenceNum);
			sentenceNum++;
		}
	}
	
	/**
	 * Lists frame targets and corresponding frames from annotated examples in the FrameNet corpus
	 * @param directory
	 * @param outFile
	 * @throws Exception
	 * @author dipanjan
	 * @see getSemEvalFrames()
	 */
	public static void originalFNUnits(String directory, String outFile) throws Exception
	{
		File dir = new File(directory);
		FilenameFilter filter = new FilenameFilter(){
			public boolean accept(File dir, String name) {
				return name.endsWith(".xml");
			}
		};
		String[] fileNames = dir.list(filter);
		int len = fileNames.length;
		System.out.println("Length of files:"+len);
		int totalNumberOfSentences = 0;
		BufferedWriter bWriter = new BufferedWriter(new FileWriter(outFile));
		sentenceNum = 0;	/* actually, this is a global annotation (frame instance) number, 
		but it is only used to find the corresponding sentence text (and the sentence text has 
		been written once per LU instance). This was verified for a sentence which repeats the 
		same LU ("We had all kinds of fruit -- we had everything .", under the Possession frame) */
		for(int i = 0; i < len; i ++)
		{
			String fileName = dir.getAbsolutePath()+"/"+fileNames[i];
			Document d = XmlUtils.parseXmlFile(fileName, false);
			Element[] luList = XmlUtils.applyXPath(d, FNConstants.LU_PATH);
			String luName = luList[0].getAttribute(FNConstants.LU_NAME_ATTR);
			String frame = luList[0].getAttribute(FNConstants.LU_FRAME_ATTR);
			if(luList.length>1)
			{
				System.out.println("Problem. Filename:"+fileName);
				System.exit(0);
			}		
			String xpath = "/lexunit-annotation/subcorpus/annotationSet/layers/layer[@name='Target']";
			Element[] targetList = XmlUtils.applyXPath(d, xpath);
			System.out.println(fileName);
			for(int j = 0; j < targetList.length; j ++)
			{
				
				int start = getStart(targetList[j]);
				int end = getEnd(targetList[j]);
				Node annotationSet = targetList[j].getParentNode().getParentNode();
				NodeList bSetChildren = annotationSet.getChildNodes();
				for(int l = 0; l < bSetChildren.getLength(); l ++)
				{
					Node n2 = bSetChildren.item(l);
					if(!n2.getNodeName().equals(FNConstants.SENTENCE))
						continue;
					String text = Frames.getTextOfSentence(n2).trim();
					String numRep = getTokenNumberRepresentation(text,start,end);
					bWriter.write(frame+"\t"+luName+"\t"+numRep+"\t"+text.substring(start,end+1)+"\t"+sentenceNum+"\n");
					sentenceNum++;
				}
			}			
		}
		bWriter.close();
		System.out.println("Total number of sentences:"+totalNumberOfSentences);
		
	}
	
	
	/**
	 * For each frame instance in the annotated Semeval data, lists information about the target and frame elements.
	 * Line format (tab-separated; ## indicates either a single token number or a multi-token span, like 9_10):
	 * #spans	target_LU	target_token##	LU_token	sentence#	FE_name1	token1##	FE_name2	token2## ...
	 * @param fileName
	 * @param bWriter
	 * @throws Exception
	 * @author Nathan Schneider (nschneid)
	 * @see Adapted from getSemEvalFrames()
	 * @see originalFNUnits()
	 */
	public static void getSemEvalFrameElements(String fileName, BufferedWriter bWriter) throws Exception
	{
		Document d = XmlUtils.parseXmlFile(fileName, false);
		String xPath = "/corpus/documents/document/paragraphs/paragraph/sentences/sentence/text";
		Element[] luList = XmlUtils.applyXPath(d,xPath);
		
		for(int i = 0; i < luList.length; i ++)
		{
			String text = luList[i].getTextContent();
			System.out.println(text);
			Node parent = luList[i].getParentNode();
			NodeList children = parent.getChildNodes();
			int len = children.getLength();
			String frameName=null;
			ArrayList<Node> frameAnnotations = new ArrayList<Node>();
			for(int j = 0; j < len; j ++)
			{
				Node n2 = children.item(j);
				if(!n2.getNodeName().equals("annotationSets"))
					continue;
				
				NodeList annotations = n2.getChildNodes();
				
				for(int k = 0; k < annotations.getLength(); k ++)
				{
					Node n3 = annotations.item(k);
					if(!n3.getNodeName().equals("annotationSet"))
						continue;
					frameName = ((Element)n3).getAttribute("frameName").trim();
					if(!frameName.equals(""))
					{
						frameAnnotations.add(n3);
					}
				}
			}
			if(frameAnnotations.size()==0)
			{
				//System.out.print("NULL\tNULL\tNULL\tNULL\n");
				sentenceNum++;
				continue;
			}			
			int annotationSize = frameAnnotations.size();
			for(int j = 0; j < annotationSize; j ++)
			{
				Node fnNode = frameAnnotations.get(j);
				String luName = ((Element)fnNode).getAttribute("luName");
				String localFrameName = ((Element)fnNode).getAttribute("frameName");
				NodeList layerss = fnNode.getChildNodes();
				for(int k = 0; k < layerss.getLength(); k ++)
				{
					Node n4 = layerss.item(k);
					if(!n4.getNodeName().equals("layers"))
						continue;
					NodeList layers = n4.getChildNodes();
					
					ArrayList<String> targets = new ArrayList<String>();	// Info for target spans (usually only 1) corresponding to this frame instance. Each will get its own line and have identical corresponding FEs.
					String feInfo = "";
					int numSpans = 1;	// number of FE fillers + 1 (the target)
					for(int l = 0; l < layers.getLength(); l ++)
					{
						Node n5 = layers.item(l);
						if(!n5.getNodeName().equals("layer"))
							continue;
						
						String n5Name = ((Element)n5).getAttribute("name");
						boolean isTarget = false;	// Either Target or FE (frame element); other layer types are ignored
						if(n5Name.equals("Target"))
							isTarget = true;
						else if (!n5Name.equals("FE"))
							continue;	// not a target or FE
						
						int start = -1;
						int end = -1;
						String numRep = null;
						try
						{
							Element layer = (Element)n5;
							if (isTarget) {
								start = LexicalUnitsFrameExtraction.getStart(layer);
								end = LexicalUnitsFrameExtraction.getEnd(layer);
								numRep = getTokenNumberRepresentation(text,start,end);
							}
							else {	// the FE layer
								NodeList layerChildren = layer.getChildNodes();
								for(int ii = 0; ii < layerChildren.getLength(); ii ++)
								{
									Node child = layerChildren.item(ii);
									if(!child.getNodeName().equals("labels"))
										continue;
									NodeList labelsChildren = child.getChildNodes();
									for(int jj = 0; jj < labelsChildren.getLength(); jj ++)
									{
										Node gChild = labelsChildren.item(jj);
										if(!gChild.getNodeName().equals("label"))
										{
											continue;
										}
										
										Element feNode = (Element)gChild;
										if (!feNode.hasAttribute("start"))	// e.g., null instantiations
											continue;
										start = new Integer(feNode.getAttribute("start"));
										end = new Integer(feNode.getAttribute("end"));
										numRep = getTokenNumberRangeRepresentation(text,start,end);
										String feName = feNode.getAttribute("name");
										feInfo += feName+"\t"+numRep+"\t";
										numSpans++;
									}
								}
							}
						}
						catch(Exception e)
						{
							e.printStackTrace();
							System.out.println("Problem. Skipping. Text: "+text);
							//System.out.print("NULL\tNULL\tNULL\tNULL\n");
							break;
						}
						
						
						if (isTarget)
							targets.add(localFrameName+"\t"+luName+"\t"+numRep+"\t"+text.substring(start,end+1)+"\t"+sentenceNum);
					}
					if (feInfo.endsWith("\t"))
						feInfo = feInfo.substring(0, feInfo.length()-1);
					for (String targetInfo : targets)
						bWriter.write(Integer.toString(numSpans) + "\t" + targetInfo +"\t" + feInfo + "\n");
				}
			}
			sentenceNum++;
		}
	}
	
	/**
	 * Lists frame targets and corresponding frames from annotated examples in the FrameNet corpus

	 */
	/**
	 * For each frame instance in annotated examples from the FrameNet corpus, lists information about the target and frame elements.
	 * Line format (tab-separated; ## indicates either a single token number or a multi-token span, like 9_10):
	 * #spans	target_LU	target_token##	LU_token	sentence#	FE_name1	token1##	FE_name2	token2## ...
	 * @param directory
	 * @param outFile
	 * @throws Exception
	 * @author Nathan Schneider (nschneid)
	 * @see Adapted from originalFNUnits()
	 * @see getSemevalFrameElements()
	 */
	public static void originalFNFrameElements(String directory, String outFile) throws Exception
	{
		File dir = new File(directory);
		FilenameFilter filter = new FilenameFilter(){
			public boolean accept(File dir, String name) {
				return name.endsWith(".xml");
			}
		};
		String[] fileNames = dir.list(filter);
		int len = fileNames.length;
		System.out.println("Length of files:"+len);
		int totalNumberOfSentences = 0;
		BufferedWriter bWriter = new BufferedWriter(new FileWriter(outFile));
		sentenceNum = 0;	// actually, this is a global annotation (frame instance) number, but it is only used to find the corresponding sentence text (and the sentence text has been written once per LU instance)
		for(int i = 0; i < len; i ++)	// iterate through LU files
		{
			String fileName = dir.getAbsolutePath()+"/"+fileNames[i];
			Document d = XmlUtils.parseXmlFile(fileName, false);
			Element[] luList = XmlUtils.applyXPath(d, FNConstants.LU_PATH);
			String luName = luList[0].getAttribute(FNConstants.LU_NAME_ATTR);
			String frame = luList[0].getAttribute(FNConstants.LU_FRAME_ATTR);
			
			if(luList.length>1)
			{
				System.out.println("Problem. Filename:"+fileName);
				System.exit(0);
			}
			System.out.println(fileName);
			
			String xASet = "/lexunit-annotation/subcorpus/annotationSet";
			Element[] aSets = XmlUtils.applyXPath(d, xASet);
			for (int a=0; a<aSets.length; a++) {	// iterate through (frame instance) annotations
				Element aSet = aSets[a];
				
				// Identify frame elements
				ArrayList<Pair<String,Pair<Integer,Integer>>> feInfo = new ArrayList<Pair<String,Pair<Integer,Integer>>>();
				{
					String xpath = "layers/layer[@name='FE']/labels/label";
					Element[] feNodes = XmlUtils.applyXPath(aSet, xpath);
					
					for(int j = 0; j < feNodes.length; j ++)
					{
						Element feNode = feNodes[j];
						if (!feNode.hasAttribute("start"))	// e.g., null instantiations
							continue;
						int start = new Integer(feNode.getAttribute("start"));
						int end = new Integer(feNode.getAttribute("end"));
						String feName = feNode.getAttribute("name");
						feInfo.add(new Pair<String,Pair<Integer,Integer>>(feName, new Pair<Integer,Integer>(start, end)));
					}
				}
				
				{	// Identify target span(s), and for each one write to the file the target and FE information
					String xpath = "layers/layer[@name='Target']";
					Element[] targetList = XmlUtils.applyXPath(aSet, xpath);
					
					for(int j = 0; j < targetList.length; j ++)
					{
						
						int start = getStart(targetList[j]);
						int end = getEnd(targetList[j]);
						Node annotationSet = targetList[j].getParentNode().getParentNode();
						NodeList bSetChildren = annotationSet.getChildNodes();
						for(int l = 0; l < bSetChildren.getLength(); l ++)
						{
							Node n2 = bSetChildren.item(l);
							if(!n2.getNodeName().equals(FNConstants.SENTENCE))
								continue;
							String text = Frames.getTextOfSentence(n2).trim();
							String numRep = getTokenNumberRepresentation(text,start,end);
							String feInfoS = "";
							for (Pair<String,Pair<Integer,Integer>> feData : feInfo) {
								String feName = feData.getFirst();
								if (text.equals("")) {
									System.out.print("");
								}
								String feNumRep = getTokenNumberRangeRepresentation(text,feData.getSecond().getFirst(),feData.getSecond().getSecond());
								feInfoS += feName + "\t" + feNumRep + "\t";
							}
							if (feInfoS.endsWith("\t"))
								feInfoS = feInfoS.substring(0, feInfoS.length()-1);
							int numSpans = feInfo.size()+1;	// number of FE fillers + 1 (the target)
							bWriter.write(Integer.toString(numSpans) + "\t"+frame+"\t"+luName+"\t"+numRep+"\t"+text.substring(start,end+1)+"\t"+sentenceNum+"\t" + feInfoS + "\n");
						}
					}
				}
				
				sentenceNum++;	// increment for each annotation. (sometimes there are multiple annotations for this LU in a particular sentence, but this doesn't matter.)
			}
		}
		bWriter.close();
		System.out.println("Total number of sentences:"+totalNumberOfSentences);
		
	}
	
	public static int getStart(Element targetLayer)
	{
		NodeList layerChildren = targetLayer.getChildNodes();
		ArrayList<Integer> starts = new ArrayList<Integer>();
		for(int i = 0; i < layerChildren.getLength(); i ++)
		{
			Node child = layerChildren.item(i);
			if(!child.getNodeName().equals("labels"))
				continue;
			NodeList labelsChildren = child.getChildNodes();
			for(int j = 0; j < labelsChildren.getLength(); j ++)
			{
				Node gChild = labelsChildren.item(j);
				if(!gChild.getNodeName().equals("label"))
				{
					continue;
				}
				int start = new Integer(((Element)gChild).getAttribute("start"));
				starts.add(start);
			}
		}
		if(starts.size()==1)
			return starts.get(0);
		Integer[] startArray = new Integer[starts.size()];
		starts.toArray(startArray);
		Arrays.sort(startArray);
		return startArray[0];	// i.e., if there are multiple parts to the target, pretend it is contiguous (I think)
	}
	
	public static int getEnd(Element targetLayer)
	{
		NodeList layerChildren = targetLayer.getChildNodes();
		ArrayList<Integer> ends = new ArrayList<Integer>();
		for(int i = 0; i < layerChildren.getLength(); i ++)
		{
			Node child = layerChildren.item(i);
			if(!child.getNodeName().equals("labels"))
				continue;
			NodeList labelsChildren = child.getChildNodes();
			for(int j = 0; j < labelsChildren.getLength(); j ++)
			{
				Node gChild = labelsChildren.item(j);
				if(!gChild.getNodeName().equals("label"))
				{
					continue;
				}
				int end = new Integer(((Element)gChild).getAttribute("end"));
				ends.add(end);
			}
		}
		if(ends.size()==1)
			return ends.get(0);
		Integer[] endArray = new Integer[ends.size()];
		ends.toArray(endArray);
		Arrays.sort(endArray);
		return endArray[ends.size()-1];	// i.e., if there are multiple parts to the target, pretend it is contiguous (I think)
	}
	
	
	
	public static String getTokenNumberRepresentation(String text, int start, int end)
	{
		StringTokenizer st = new StringTokenizer(text," ",true);
		String result = "";
		int countChars = 0;
		int countToks = 0;
		boolean foundStart = false;
		int oldCountChars = 0;
		while(st.hasMoreTokens())
		{
			String token = st.nextToken();
			if(!foundStart)
			{
				if(start==countChars)
				{
					result+=countToks+"_";
					foundStart=true;
				}
			}
			if(end==countChars)
			{
				break;
			}
			else if(end>countChars&&foundStart&&countChars!=start)
			{
				if(!token.equals(" "))	
					result+=countToks+"_";
			}
			countChars+=token.length();
			if(!token.equals(" "))	
				countToks++;
		}
		return result.substring(0,result.length()-1);
	}
	
	public static String getTokenNumberRangeRepresentation(String text, int start, int end)
	{
		if (!(start==0 || text.charAt(start-1)==' ') || !(end==text.length()-1 || text.charAt(end+1)==' ')) {
			System.err.println("Expanding character range so it corresponds to token boundaries: was start=" + Integer.toString(start) + ", end=" + Integer.toString(end) + ", sentence=\"" + text + "\"");
			
			// fix 'start' to be the beginning of a token, and 'end' the end of a token, by expanding boundaries outwards
			if (start>0 && text.charAt(start-1)!=' ') {
				int pSpaceIndex = text.lastIndexOf(" ", start-1);
				start = ((pSpaceIndex>-1) ? pSpaceIndex+1 : 0);
			}
			if (end<text.length() && text.charAt(end+1)!=' ') {
				int nSpaceIndex = text.indexOf(" ", end+1);
				end = ((nSpaceIndex>-1) ? nSpaceIndex-1 : 0);
			}
			
			System.err.println("...Now start=" + Integer.toString(start) + ", end=" + Integer.toString(end));
			
			/* This only affected a couple of annotations. In the original FN corpus:
That number has now increased to some 340,000. 95% of patients are detained without their consent , at a rate of 250 per 100,000 population , compared with just 2.5 per 100,000 in Europe
  end=44 => end=45
The awards at Levels 1 and 2 are precisely targeted at people working in the retail industry or undergoing training for retail work . 
 start=117 => start=116
			 */
		}
		String allTokenNums = getTokenNumberRepresentation(text,start,end);	// all token numbers in range, separated by underscores
		if (!allTokenNums.contains("_"))	// a single token--just return that number
			return allTokenNums;
		// firstTokenNum:lastTokenNum
		String tokenNumRange = allTokenNums.substring(0, allTokenNums.indexOf("_")) + ":" + allTokenNums.substring(allTokenNums.lastIndexOf("_")+1, allTokenNums.length());
		return tokenNumRange;
	}
	
}
