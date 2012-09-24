/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * PaperEvaluation.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.evaluation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Set;
import java.util.StringTokenizer;

import edu.cmu.cs.lti.ark.fn.data.prep.LexicalUnitsFrameExtraction;
import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntObjectHashMap;



public class PaperEvaluation
{
	public static final String malRootDir="/mal2/dipanjan/experiments/FramenetParsing/SSFrameStructureExtraction/argscripts";
	//public static final String malRootDir="../testdata";
	public static void main(String[] args) throws Exception
	{
		WordNetRelations wnr = new WordNetRelations("lrdata/stopwords.txt","file_properties.xml");
		Set<String> set = wnr.getRelations("intent","intention");
		for(String s:set)
			System.out.println(s);
		//extractOurSegmentation();
		//evaluateSegmentation();
		//extractJohanssonSegmentation();
		//printAllResults();
		//convertGoldTestToFramesFile();
		//convertGoldDevToFrameFileOracleSpans();
		//convertGoldTestToFramesFileOracleSpans();
	}	

	
	
	public static void convertGoldTestToFramesFile()
	{
		String goldFile = malRootDir+"/testdata/johansson.fulltest.sentences.frames";
		String outFile = malRootDir+"/FrameStructureExtraction/release/temp_johansson_full/file.frame.elements";
		ArrayList<String> inLines = ParsePreparation.readSentencesFromFile(goldFile);
		ArrayList<String> outLines = new ArrayList<String>();
		THashMap<String,THashSet<String>> frameMap = (THashMap<String,THashSet<String>>)SerializedObjects.readSerializedObject("lrdata/framenet.original.map");
		for(String inLine: inLines)
		{	
			String[] toks = inLine.split("\t");
			if(!frameMap.contains(toks[0]))
					continue;
			String outLine = "1\t"+toks[0]+"\t"+toks[1]+"\t"+toks[2]+"\t"+toks[3]+"\t"+toks[4];
			outLines.add(outLine);
		}
		ParsePreparation.writeSentencesToTempFile(outFile, outLines);
	}
	
	public static void convertGoldJohanssonToFramesFile()
	{
		String goldFile = malRootDir+"/testdata/semeval.fulltest.sentences.frame.elements";
		String outFile = malRootDir+"/testdata/file.frame.elements";
		ArrayList<String> inLines = ParsePreparation.readSentencesFromFile(goldFile);
		ArrayList<String> outLines = new ArrayList<String>();
		THashMap<String,THashSet<String>> frameMap = (THashMap<String,THashSet<String>>)SerializedObjects.readSerializedObject("lrdata/framenet.original.map");
		for(String inLine: inLines)
		{	
			String[] toks = inLine.split("\t");
			if(!frameMap.contains(toks[1]))
					continue;
			String outLine = "1\t"+toks[1]+"\t"+toks[2]+"\t"+toks[3]+"\t"+toks[4]+"\t"+toks[5];
			outLines.add(outLine);
		}
		ParsePreparation.writeSentencesToTempFile(outFile, outLines);
	}
	
	public static void convertGoldTestToFramesFileOracleSpans()
	{
		String goldFile = malRootDir+"/testdata/semeval.fulltest.sentences.frame.elements";
		String outFile = malRootDir+"/testdata/file.os.frame.elements";
		ArrayList<String> inLines = ParsePreparation.readSentencesFromFile(goldFile);
		ArrayList<String> outLines = new ArrayList<String>();
		THashMap<String,THashSet<String>> frameMap = (THashMap<String,THashSet<String>>)SerializedObjects.readSerializedObject("lrdata/framenet.original.map");
		for(String inLine: inLines)
		{
			String[] toks = inLine.split("\t");
			if(!frameMap.contains(toks[1]))
					continue;
			outLines.add(inLine);
		}
		ParsePreparation.writeSentencesToTempFile(outFile, outLines);
	}	
	
	public static void convertGoldDevToFrameFileOracleSpans()
	{
		int[] indices = {146,178,280,329,397};
		String goldFile = "lrdata/semeval.fulldev.sentences.frame.elements";
		ArrayList<String> inLines = ParsePreparation.readSentencesFromFile(goldFile);
		THashMap<String,THashSet<String>> frameMap = (THashMap<String,THashSet<String>>)SerializedObjects.readSerializedObject("lrdata/framenet.original.map");
		for(int i = 0; i < indices.length-1; i ++)
		{
			int start = indices[i];
			int end = indices[i+1];
			ArrayList<String> outLines = new ArrayList<String>();
			for(int j = 0; j < inLines.size(); j ++)
			{
				String line = inLines.get(j);
				String[] toks = line.split("\t");
				if(!frameMap.contains(toks[1]))
					continue;
				int sentNum = new Integer(toks[5]);
				if(sentNum>=start&&sentNum<end)
				{
					outLines.add(line);
				}
			}
			String outFile = malRootDir+"/dev.os.frame.elements"+start;
			ParsePreparation.writeSentencesToTempFile(outFile, outLines);
		}
	}
	
	public static void printAllResults()
	{
		String[] conditions = {"gt"};
		String[] eval = {"e","p"};
		String[] locality = {"overlapcheck","nooverlapcheck"};
		for(int i = 0; i < conditions.length; i ++)
		{
			for(int j = 0; j < eval.length; j ++)
			{
				for(int k = 0; k < locality.length; k ++)
				{
					String file = null;
					if(conditions[i].equals(""))
						file = malRootDir+"/results_"+locality[k]+"/results_"+eval[j];
					else
						file = malRootDir+"/results_"+conditions[i]+"_"+locality[k]+"/results_"+eval[j];
					System.out.println("Results for:"+file);
					printResults(file);
					System.out.println("\n\n");					
				}
			}
		}
	}
	
	public static void printResults(String file)
	{
		double macroPrecision = 0.0;
		double macroRecall = 0.0;
		double macroF = 0.0;
		
		double totalCorrect = 0.0;
		double totalActual = 0.0;
		double totalFound = 0.0;
		ArrayList<String> lines = ParsePreparation.readSentencesFromFile(file);
		for(String line:lines)
		{
			System.out.println(line);
		}
		int[] indices = {6,10,14};
		for(int i = 0; i < indices.length; i ++)
		{
			String scoreLine = lines.get(indices[i]);
			int firstIndexOpen = scoreLine.indexOf("(");
			int firstIndexClose = scoreLine.indexOf(")");
			String rest = scoreLine.substring(firstIndexClose+1).trim();
			String recall = scoreLine.substring(firstIndexOpen+1,firstIndexClose);
			String[] recToks = recall.split("/");
			double correct = new Double(recToks[0]);
			double actual = new Double(recToks[1]);			
			int secondIndexOpen = rest.indexOf("(");
			int secondIndexClose = rest.indexOf(")");
			String precision = rest.substring(secondIndexOpen+1, secondIndexClose); 
			String[] precToks = precision.split("/");
			double found = new Double(precToks[1]);
			totalCorrect+=correct;
			totalActual+=actual;
			totalFound+=found;
			System.out.println();
			macroPrecision+=(correct/found);
			macroRecall+=(correct/actual);
			macroF+=(2*(correct/actual)*(correct/found))/((correct/actual)+(correct/found));
		}
		macroPrecision/=indices.length;
		macroRecall/=indices.length;
		macroF/=indices.length;
		System.out.println("Macro Precision:"+macroPrecision+"\tMacro Recall:"+macroRecall+"\tMacro FMeasure:"+macroF);
		double precision = totalCorrect/totalFound;
		double recall = totalCorrect/totalActual;
		double f1 = (2.0*precision*recall)/(precision+recall);
		System.out.println("Micro Precision:"+precision+"\tMicro Recall:"+recall+"\tMicro FMeasure:"+f1);
	}
	
	
	public static void extractJohanssonSegmentation() throws Exception
	{
		
		String rootDir=malRootDir+"/dipanjan_semeval/submitted_texts";
		String[] files = {"IntroOfDublin.xml","ChinaOverview.xml","workAdvances.xml"};
		BufferedWriter bWriterFrames = new BufferedWriter(new FileWriter(malRootDir+"/testdata/johansson.fulltest.sentences.frames"));
		for(int i = 0; i < files.length; i ++)
		{
			String file = rootDir+"/"+files[i];
			System.out.println("\n\n"+file);
			LexicalUnitsFrameExtraction.getSemEvalFrames(file,bWriterFrames);
		}		
		bWriterFrames.close();
	}
	
	public static void extractOurSegmentation() throws Exception
	{
		LexicalUnitsFrameExtraction.sentenceNum=0;
		String rootDir=malRootDir+"/FrameStructureExtraction/evalscripts";
		String[] files = {"IntroOfDublin_local.xml","ChinaOverview_local.xml","workAdvances_local.xml"};
		BufferedWriter bWriterFrames = new BufferedWriter(new FileWriter(malRootDir+"/testdata/our.fulltest.sentences.frames"));
		for(int i = 0; i < files.length; i ++)
		{
			String file = rootDir+"/"+files[i];
			System.out.println("\n\n"+file);
			LexicalUnitsFrameExtraction.getSemEvalFrames(file,bWriterFrames);
		}
		bWriterFrames.close();
	}
	
	public static void evaluateSegmentation() throws Exception
	{
		String johansson = malRootDir+"/testdata/johansson.fulltest.sentences.frames";
		String our = malRootDir+"/testdata/our.fulltest.sentences.frames";
		System.out.println("Johansson's results:");
		evaluateSegmentationIndividual(johansson,true);
		System.out.println("Our results:");
		evaluateSegmentationIndividual(our,false);
	}
	
	public static void evaluateSegmentationIndividual(String file,boolean flag) throws Exception
	{
		String goldFile = malRootDir+"/testdata/semeval.fulltest.sentences.frame.elements";
		ArrayList<String> goldStuff = ParsePreparation.readSentencesFromFile(goldFile);
		TIntObjectHashMap<THashSet<String>> spans = new TIntObjectHashMap<THashSet<String>>();
		int totalGoldSpans = goldStuff.size();
		for(String gold:goldStuff)
		{
			String[] toks = gold.split("\t");
			int sentNum = new Integer(toks[5]);
			THashSet<String> set = spans.get(sentNum);
			if(set==null)
			{
				set=new THashSet<String>();
				set.add(toks[3]);
				spans.put(sentNum, set);
			}
			else
			{
				set.add(toks[3]);
				spans.put(sentNum, set);
			}
		}
		
		ArrayList<String> modelStuff = ParsePreparation.readSentencesFromFile(file);
		int totalModelSpans = modelStuff.size();
		int totalCorrect = 0;
		for(String line:modelStuff)
		{
			String[] toks = getTokens(line);
//			if(!flag)
//			{
//				sentIndex=3;
//				spanIndex=1;
//			}
			int sentNum = new Integer(toks[toks.length-1]);
			String span = toks[toks.length-3];
			System.out.println(span+"\t"+sentNum);
			THashSet<String> set = spans.get(sentNum);
			if(set!=null)
			{
				if(set.contains(span))
					totalCorrect++;
			}
		}
		double precision = (double)totalCorrect/(double)totalModelSpans;
		double recall = (double)totalCorrect/(double)totalGoldSpans;
		double f  = (2*precision*recall)/(precision+recall);
		
		System.out.println("precision:"+precision);
		System.out.println("recall:"+recall);
		System.out.println("f:"+f);
	}
	
	public static String[] getTokens(String line)
	{
		StringTokenizer st = new StringTokenizer(line,"\t",true);
		ArrayList<String> toks = new ArrayList<String>();
		while(st.hasMoreTokens())
		{
			String tok = st.nextToken().trim();
			if(tok.equals(""))
			{
				continue;
			}
			toks.add(tok);
		}
		
		String[] arr = new String[toks.size()];
		return toks.toArray(arr);
	}
	
}
