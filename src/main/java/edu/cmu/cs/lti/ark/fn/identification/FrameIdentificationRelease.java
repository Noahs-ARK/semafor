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

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Token;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import gnu.trove.TObjectDoubleHashMap;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.google.common.base.Strings.nullToEmpty;


public class FrameIdentificationRelease {
	public static Pair<IdFeatureExtractor, TObjectDoubleHashMap<String>>
			parseParamFile(String paramsFile) throws IOException {
		final List<String> lines = Files.readLines(new File(paramsFile), Charsets.UTF_8);
		TObjectDoubleHashMap<String> model = new TObjectDoubleHashMap<String>(lines.size());
		int count = 0;
		final IdFeatureExtractor featureExtractor = new IdFeatureExtractor.Converter().convert(lines.get(0));
		for (String line : lines.subList(1, lines.size())) {
			String[] fields = line.split("\t");
			final String featureName = fields[0].trim();
			final Double featureValue = Double.parseDouble(fields[1].trim());
			model.put(featureName, featureValue);
			count++;
			if (count % 100000 == 0) System.out.print(count + " ");
		}
		return Pair.of(featureExtractor, model);
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
