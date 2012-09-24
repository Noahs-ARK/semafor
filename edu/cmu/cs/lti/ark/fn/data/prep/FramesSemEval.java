/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * FramesSemEval.java is part of SEMAFOR 2.0.
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.cmu.cs.lti.ark.util.XmlUtils;
import gnu.trove.THashMap;



public class FramesSemEval
{
	public static final String[] devSet = {
		"/mal2/dipanjan/experiments/FramenetParsing/semeval-2007-task19/train/ANC/HistoryOfLasVegas.xml",
		"/mal2/dipanjan/experiments/FramenetParsing/semeval-2007-task19/train/ANC/StephanopoulosCrimes.xml",
		"/mal2/dipanjan/experiments/FramenetParsing/semeval-2007-task19/train/NTI/Iran_Biological.xml",
		"/mal2/dipanjan/experiments/FramenetParsing/semeval-2007-task19/train/NTI/NorthKorea_Introduction.xml",
		"/mal2/dipanjan/experiments/FramenetParsing/semeval-2007-task19/train/NTI/WMDNews_042106.xml"
	};
	
	public static final String[] testSet = {
		"/mal2/dipanjan/experiments/FramenetParsing/dipanjan_semeval/gold_texts/ANC_IntroOfDublin.xml",
		"/mal2/dipanjan/experiments/FramenetParsing/dipanjan_semeval/gold_texts/NTI_chinaOverview.xml",
		"/mal2/dipanjan/experiments/FramenetParsing/dipanjan_semeval/gold_texts/NTI_workAdvances.xml"
	};
	
	public static void main(String[] args)
	{
		//tokenSanityCheck();
		separateParsedData();
	}
	
	public static void posTagging()
	{
		
	}	
	
	public static void tokenization()
	{
		//extractSentences();
		tokenize();
		tokenSanityCheck();
	}
	
	public static void separateParsedData()
	{
		int trainSize = FixTokenization.readSentencesFromFile("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulltrain.sentences").size();
		int devSize = FixTokenization.readSentencesFromFile("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulldev.sentences").size();
		int testSize = FixTokenization.readSentencesFromFile("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulltest.sentences").size();
			
		String parsedFile = "/mal2/dipanjan/experiments/MSTParserStacked/data/semeval.dat.parsed";
		ArrayList<String> trainParses = new ArrayList<String>();
		ArrayList<String> devParses = new ArrayList<String>();
		ArrayList<String> testParses = new ArrayList<String>();
		
		ArrayList<String> allParses = new ArrayList<String>();
		try
		{
			BufferedReader bReader = new BufferedReader(new FileReader(parsedFile));
			String line = null;
			String wholeParse = "";
			while((line=bReader.readLine())!=null)
			{
				line = line.trim();
				if(line.equals(""))
				{
					allParses.add(wholeParse);
					wholeParse="";
				}
				else
				{
					wholeParse+=line+"\n";
				}
			}
			bReader.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		String trainParseFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulltrain.sentences.conll.parsed";
		String devParseFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulldev.sentences.conll.parsed";
		String testParseFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulltest.sentences.conll.parsed";
			
		for(int i = 0; i < trainSize; i ++)
		{
			String parse = allParses.get(i).trim();
			trainParses.add(parse);
		}
		for(int i = trainSize; i < trainSize+devSize; i ++)
		{
			String parse = allParses.get(i).trim();
			devParses.add(parse);
		}
		for(int i = trainSize+devSize; i < trainSize+devSize+testSize; i ++)
		{
			String parse = allParses.get(i).trim();
			testParses.add(parse);
		}
		FixTokenization.writeDepSentencesToTempFile(trainParseFile, trainParses);
		FixTokenization.writeDepSentencesToTempFile(devParseFile, devParses);
		FixTokenization.writeDepSentencesToTempFile(testParseFile, testParses);
	}
	
	
	public static void tokenSanityCheck()
	{
		String inputFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulldev.sentences";
		String outputFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulldev.sentences.tokenized";
		
		ArrayList<String> firstSet = FixTokenization.readSentencesFromFile(inputFile);
		ArrayList<String> secondSet = FixTokenization.readSentencesFromFile(outputFile);
		
		int size = firstSet.size();
		for(int i = 0; i < size; i ++)
		{
			String line1 = firstSet.get(i).trim();
			String line2 = secondSet.get(i).trim();
			
			StringTokenizer st1 = new StringTokenizer(line1);
			StringTokenizer st2 = new StringTokenizer(line2);
			
			if(st1.countTokens()!=st2.countTokens())
			{
				System.out.println(line1);
				System.out.println(line2);
			}	
		}		
	}
	
	public static String replaceSentenceWithPTBWords(String sentence)
	{
		sentence = sentence.replace("(","-LRB-");
		sentence = sentence.replace(")","-RRB-");
		sentence = sentence.replace("[","-LSB-");
		sentence = sentence.replace("]","-RSB-");
		sentence = sentence.replace("{","-LCB-");
		sentence = sentence.replace("}","-RCB-");
		return sentence;
	}
	
	public static void tokenize()
	{
		String prefix = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData";
		String[] inputs = {"semeval.fulltrain.sentences","semeval.fulldev.sentences","semeval.fulltest.sentences"};
		String[] outputs = {"semeval.fulltrain.sentences.tokenized","semeval.fulldev.sentences.tokenized","semeval.fulltest.sentences.tokenized"};
		String convFile = "/usr0/dipanjan/software/varcon/abbc.tab";
		THashMap<String,String> convMap = FixTokenization.readConversions(convFile);
		for(int i = 0; i < inputs.length; i ++)
		{
			String inputFile = prefix+"/"+inputs[i];
			String outputFile = prefix+"/"+outputs[i];
			ArrayList<String> sentences = FixTokenization.readSentencesFromFile(inputFile);
			int size = sentences.size();
			ArrayList<String> revisedSentences = new ArrayList<String>();
			for(int j = 0; j < size; j ++)
			{
				String revised = replaceSentenceWithPTBWords(sentences.get(j));
				revised = FixTokenization.replaceBritishWords(revised, convMap);
				revisedSentences.add(revised);
			}
			FixTokenization.writeSentencesToTempFile(outputFile, revisedSentences);
		}
	}
	
	
	public static void extractSentences()
	{
		train();
		dev();
		test();
	}
	
	public static void test()
	{
		ArrayList<String> sentences1 = FixTokenization.readSentencesFromFile("/mal2/dipanjan/experiments/FramenetParsing/semeval-2007-task19/test/ANC/IntroOfDublin.txt");
		sentences1.addAll(FixTokenization.readSentencesFromFile("/mal2/dipanjan/experiments/FramenetParsing/semeval-2007-task19/test/NTI/ChinaOverview.txt"));
		sentences1.addAll(FixTokenization.readSentencesFromFile("/mal2/dipanjan/experiments/FramenetParsing/semeval-2007-task19/test/NTI/workAdvances.txt"));
		FixTokenization.writeSentencesToTempFile("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulltest.sentences", sentences1);		
	}
	
	
	public static void train()
	{
		Arrays.sort(devSet);
		try
		{
			BufferedWriter bWriter = new BufferedWriter(new FileWriter("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulltrain.sentences"));
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
					if(Arrays.binarySearch(devSet, fileName)>=0)
						continue;
					getLUStatistics(fileName,bWriter);
				}
			}
			bWriter.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void dev()
	{
		try
		{
			BufferedWriter bWriter = new BufferedWriter(new FileWriter("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulldev.sentences"));
			for(int i = 0; i < devSet.length; i ++)
			{
				String fileName = devSet[i];
				getLUStatistics(fileName,bWriter);
			}
			bWriter.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void getLUStatistics(String fileName, BufferedWriter bWriter) throws Exception
	{
		int totalNumberOfSentences = 0;
		Document d = XmlUtils.parseXmlFile(fileName, false);
		Element[] luList = XmlUtils.applyXPath(d, "/corpus/documents/document/paragraphs/paragraph/sentences/sentence/text");
		int listLen = luList.length;
		System.out.println(listLen);
		for(int j = 0; j < listLen; j++)
		{
			String text = luList[j].getTextContent().trim();
			bWriter.write(text+"\n");
			totalNumberOfSentences++;
		}
		System.out.println("Total number of sentences:"+totalNumberOfSentences);
	}
	
	
	public static String getTextOfSentence(Node n)
	{
		NodeList children = n.getChildNodes();
		for(int i = 0; i < children.getLength(); i ++)
		{
			Node child = children.item(i);
			if(!child.getNodeName().equals("text"))
				continue;
			String content = child.getTextContent();
			return content;
		}
		System.out.println("Error");
		System.exit(0);
		return null;
	}	
}


















