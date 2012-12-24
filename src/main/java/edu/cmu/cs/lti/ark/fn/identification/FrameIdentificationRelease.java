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


public class FrameIdentificationRelease {
	public static TObjectDoubleHashMap<String> parseParamFile(String paramsFile) {
		TObjectDoubleHashMap<String> startParamList = new TObjectDoubleHashMap<String>(); 
		try {
			BufferedReader fis = new BufferedReader(new FileReader(paramsFile));
			String pattern;
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
		List<String> actualTokens = Lists.newArrayList();
		for (int i : indices) {
			actualTokens.add(tokens.get(i).getForm());
		}
		return getFirstTokenAndCpos(indices, sentence) + "\t" + Joiner.on(" ").join(actualTokens);
	}

	public static String getFirstTokenAndCpos(List<Integer> indices, Sentence sentence) {
		if(indices.isEmpty()) return "";
		final Token firstToken = sentence.getTokens().get(indices.get(0));
		return firstToken.getForm().toLowerCase() + "." +
				nullToEmpty(firstToken.getPostag()).substring(0, 1).toLowerCase();

	}
}
