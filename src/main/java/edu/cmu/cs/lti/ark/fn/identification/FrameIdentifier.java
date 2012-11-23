/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * FrameIdentifier.java is part of SEMAFOR 2.0.
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
import java.util.Date;
import java.util.StringTokenizer;

import org.apache.hadoop.util.StringUtils;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.fn.utils.DataPoint;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.optimization.Alphabet;
import edu.cmu.cs.lti.ark.util.optimization.LDouble;
import edu.cmu.cs.lti.ark.util.optimization.LogFormula;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectDoubleHashMap;

public class FrameIdentifier
{
	public static void main(String[] args)
	{
		System.out.println("Starting to sleep...");
		try
		{
			Thread.currentThread().sleep(2000);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		System.out.println("Finished sleeping.");
		
		FNModelOptions opts = new FNModelOptions(args);
		if(opts.train.get())
		{ 
			ArrayList<String> trainFrameLines = ParsePreparation.readSentencesFromFile(opts.trainFrameFile.get());
			ArrayList<String> trainParseLines = ParsePreparation.readSentencesFromFile(opts.trainParseFile.get());
			train(trainFrameLines, trainParseLines, opts);
		}
		else if(opts.dev.get())
		{
			
		}
		else if(opts.test.get())
		{
			System.out.println("Not handling yet.");
		}
	}
	
	public static void train(ArrayList<String> trainFrameLines, ArrayList<String> trainParseLines, FNModelOptions opts)
	{
		WordNetRelations wnr = new WordNetRelations(opts.stopWordsFile.get(), opts.wnConfigFile.get());
		TObjectDoubleHashMap<String> paramList = parseParamFile(opts.modelFile.get()); 
		THashMap<String,THashSet<String>> frameMap = (THashMap<String,THashSet<String>>)SerializedObjects.readSerializedObject(opts.frameNetMapFile.get());	
		LRIdentificationModelSingleNode lrModel = new LRIdentificationModelSingleNode(paramList, 
									  								 trainFrameLines, 
									  								 trainParseLines, 
									  								 opts.reg.get(), 
									  								 opts.lambda.get(),
									  								 null, 
									  								 wnr,
									  								 frameMap, 
									  								 opts.modelFile.get(), 
									  								 "train");
		System.out.println(new Date());
		LogFormula f = lrModel.getFormula(7000);
		System.out.println("Value:"+f.evaluate(lrModel));
		System.out.println("Taking derivatives...."+new Date());
		System.out.println("Finished taking derivatives...."+new Date());
	}
	
	
	private static TObjectDoubleHashMap<String> parseParamFile(String paramsFile)
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
			System.err.println("Caught exception while parsing the cached file '" + paramsFile + "' : " + StringUtils.stringifyException(ioe));
		}
		return startParamList;
	}		
}
