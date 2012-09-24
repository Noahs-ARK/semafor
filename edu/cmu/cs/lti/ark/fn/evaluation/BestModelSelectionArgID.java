/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * BestModelSelectionArgID.java is part of SEMAFOR 2.0.
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
import java.util.Set;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import gnu.trove.*;

public class BestModelSelectionArgID
{
	public static void main(String[] args)
	{
		int[] batchsizes = {2};
		int[] fileInfixes = {146, 178, 280, 329};
		String resultDirectory=args[0];
		//String resultDirectory="../result_os_local";
		int bestResultBatch = -1;
		String bestResultPass = null;
		double bestF1 = 0.0;
		File dir = new File(resultDirectory);		
		for(int i = 0; i < batchsizes.length; i ++)
		{
			int bSize = batchsizes[i];
			ArrayList<String> numPassesList = new ArrayList<String>();
			ArgResultFileFilter f = new ArgResultFileFilter(fileInfixes,bSize);
			String[] fileNames = dir.list(f);
			System.out.println("Files for batchsize:"+bSize);
			for(String filename:fileNames)
				System.out.println(filename);
			THashMap<String,THashSet<String>> map = new THashMap<String,THashSet<String>>();
			for(String fileName: fileNames)
			{
				String[] toks = fileName.split("\\.");
				int numPasses = new Integer(toks[toks.length-1]);
				int reg = new Integer(toks[toks.length-3]);
				System.out.println("numpasses:"+numPasses+" reg:"+reg);
				THashSet<String> set = map.get("r."+reg+".p."+numPasses);
				if(set==null)
				{
					set = new THashSet<String>();
					set.add(fileName);
					map.put("r."+reg+".p."+numPasses, set);
				}
				else
				{
					set.add(fileName);
				}	
			}
			Set<String> keys = map.keySet();
			for(String key:keys)
			{
				THashSet<String> set = map.get(key);
				if(set.size()!=fileInfixes.length)
					continue;
				numPassesList.add(key);
			}
			System.out.println("Getting average results:");
			for(String numPasses: numPassesList)
			{
				double result = getAvgResult(dir,numPasses,bSize,fileInfixes);
				if(result>bestF1)
				{
					bestResultBatch=bSize;
					bestResultPass=numPasses;
					bestF1=result;
				}
			}
		}
		System.out.println("Best Result:"+bestF1+"\tNumber of passes:"+bestResultPass+"\tBatch Size:"+bestResultBatch);
		System.out.println("Results:");
		printResults(dir,bestResultPass,bestResultBatch,fileInfixes);
	}	
	
	public static void printResults(File dir, String numPasses, int bSize, int[] fileInfixes)
	{
		double macroPrecision = 0.0;
		double macroRecall = 0.0;
		double macroF = 0.0;
		
		double totalCorrect = 0.0;
		double totalActual = 0.0;
		double totalFound = 0.0;
		for(int i = 0; i < fileInfixes.length; i ++)
		{
			String file = dir.getAbsolutePath()+"/result_"+fileInfixes[i]+"_k."+bSize+"."+numPasses;
			System.out.println("Results for "+fileInfixes[i]);
			ArrayList<String> lines = ParsePreparation.readSentencesFromFile(file);
			for(String line:lines)
			{
				System.out.println(line);
			}
			String scoreLine = lines.get(lines.size()-1);
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
		macroPrecision/=fileInfixes.length;
		macroRecall/=fileInfixes.length;
		macroF/=fileInfixes.length;
		System.out.println("Macro Precision:"+macroPrecision+"\tMacro Recall:"+macroRecall+"\tMacro FMeasure:"+macroF);
		double precision = totalCorrect/totalFound;
		double recall = totalCorrect/totalActual;
		double f1 = (2.0*precision*recall)/(precision+recall);
		System.out.println("Micro Precision:"+precision+"\tMicro Recall:"+recall+"\tMicro FMeasure:"+f1);
		
	}
	
	public static double getAvgResult(File dir, String numPasses, int bSize, int[] fileInfixes)
	{
		double totalCorrect = 0.0;
		double totalActual = 0.0;
		double totalFound = 0.0;
		for(int i = 0; i < fileInfixes.length; i ++)
		{
			String file = dir.getAbsolutePath()+"/result_"+fileInfixes[i]+"_k."+bSize+"."+numPasses;
			ArrayList<String> lines = ParsePreparation.readSentencesFromFile(file);
			String scoreLine = lines.get(lines.size()-1);
			System.out.println(scoreLine);
			System.out.println(numPasses+"\t"+bSize+"\t"+fileInfixes[i]);
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
		}		
		double precision = totalCorrect/totalFound;
		double recall = totalCorrect/totalActual;
		double f1 = (2.0*precision*recall)/(precision+recall);
		return f1;
	}	
}

class ArgResultFileFilter implements FilenameFilter
{
	int[] mInfixes;
	int mBatchSize;
	
	public ArgResultFileFilter(int[] infixes, int batchSize)
	{
		mInfixes= infixes;
		mBatchSize = batchSize;
	}
	
	public boolean accept(File arg0, String arg1)
	{	
		boolean retVal = false;
		for(int j = 0; j < mInfixes.length; j++)
		{
			if(arg1.startsWith("result_"+mInfixes[j]+"_k."+mBatchSize+".r."))
			{
				retVal=true;
			}
		}
		return retVal;
	}
	
}
