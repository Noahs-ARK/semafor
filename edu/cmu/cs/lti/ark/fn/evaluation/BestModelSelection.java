/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * BestModelSelection.java is part of SEMAFOR 2.0.
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

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import gnu.trove.*;

public class BestModelSelection
{
	public static void main(String[] args)
	{
		int[] batchsizes = {2};
		String resultDirectory="/mal2/dipanjan/experiments/FramenetParsing/SSFrameStructureExtraction/idscripts/idresults_malt";
		String bestFile = null;
		double bestF1 = 0.0;
		File dir = new File(resultDirectory);		
		for(int i = 0; i < batchsizes.length; i ++)
		{
			int bSize = batchsizes[i];
			ArrayList<String> allFiles = new ArrayList<String>();
			ResultFileFilter f = new ResultFileFilter(bSize);
			String[] fileNames = dir.list(f);
			TIntObjectHashMap<THashSet<String>> map = new TIntObjectHashMap<THashSet<String>>();
			for(String fileName: fileNames)
			{
				String[] toks = fileName.split("_");
				int numPasses = new Integer(toks[2]);
				THashSet<String> set = map.get(numPasses);
				if(set==null)
				{
					set = new THashSet<String>();
					set.add(fileName);
					map.put(numPasses, set);
				}
				else
				{
					set.add(fileName);
				}	
			}			
			int[] keys = map.keys();
			for(int j = 0; j < keys.length; j ++)
			{
				THashSet<String> set = map.get(keys[j]);
				for(String string:set)
					allFiles.add(string);
			}			
			for(String file: allFiles)
			{
				double result = getAvgResult(resultDirectory,file);
				if(result>bestF1)
				{
					bestFile = file;
					bestF1=result;
				}
			}
		}
		System.out.println("Best Result:"+bestF1+"\tFile:"+bestFile);
		System.out.println("Results:");
		printResults(dir.getAbsolutePath(),bestFile);
	}	
	
	public static void printResults(String dir,String file)
	{
		double macroPrecision = 0.0;
		double macroRecall = 0.0;
		double macroF = 0.0;
		
		double totalCorrect = 0.0;
		double totalActual = 0.0;
		double totalFound = 0.0;
		ArrayList<String> lines = ParsePreparation.readSentencesFromFile(dir+"/"+file);
		String[] toks = file.split("_");
		int bSize = new Integer(toks[1]);
		int numPasses = new Integer(toks[2]);
		double reg = new Double(toks[3]);
		System.out.println("Results for batchSize:"+bSize+" Number of passes:"+numPasses+" Regularization:"+reg);
		double len = 0;
		for(String line:lines)
		{
			System.out.println(line);
			if(!line.contains("Sentences Scored"))
				continue;
			int firstIndexOpen = line.indexOf("(");
			int firstIndexClose = line.indexOf(")");
			String rest = line.substring(firstIndexClose+1).trim();
			String recall = line.substring(firstIndexOpen+1,firstIndexClose);
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
			macroPrecision+=(correct/found);
			macroRecall+=(correct/actual);
			macroF+=(2*(correct/actual)*(correct/found))/((correct/actual)+(correct/found));
			len+=1.0;
		}
		macroPrecision/=len;
		macroRecall/=len;
		macroF/=len;
		System.out.println("Macro Precision:"+macroPrecision+"\tMacro Recall:"+macroRecall+"\tMacro FMeasure:"+macroF);
		double precision = totalCorrect/totalFound;
		double recall = totalCorrect/totalActual;
		double f1 = (2.0*precision*recall)/(precision+recall);
		System.out.println("Micro Precision:"+precision+"\tMicro Recall:"+recall+"\tMicro FMeasure:"+f1);
		
	}
	
	public static double getAvgResult(String dir,String file)
	{
		double totalCorrect = 0.0;
		double totalActual = 0.0;
		double totalFound = 0.0;
		String[] toks = file.split("_");
		int bSize = new Integer(toks[1]);
		int numPasses = new Integer(toks[2]);
		double reg = new Double(toks[3]);
		file = dir+"/"+file;
		ArrayList<String> lines = ParsePreparation.readSentencesFromFile(file);
		for(String line:lines)
		{
			if(!line.contains("Sentences Scored"))
				continue;
			System.out.println(line);
			System.out.println(numPasses+"\t"+bSize+"\t"+reg);
			int firstIndexOpen = line.indexOf("(");
			int firstIndexClose = line.indexOf(")");
			String rest = line.substring(firstIndexClose+1).trim();
			String recall = line.substring(firstIndexOpen+1,firstIndexClose);
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
		}		
		double precision = totalCorrect/totalFound;
		double recall = totalCorrect/totalActual;
		double f1 = (2.0*precision*recall)/(precision+recall);
		return f1;
	}	
}

class ResultFileFilter implements FilenameFilter
{
	int mBatchSize;
	
	public ResultFileFilter(int batchSize)
	{
		mBatchSize = batchSize;
	}
	
	public boolean accept(File arg0, String arg1)
	{	
		boolean retVal = false;
		if(arg1.startsWith("result_"+mBatchSize+"_"))
		{
				retVal=true;
		}
		return retVal;
	}
	
}
