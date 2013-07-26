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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Strings.nullToEmpty;
import static java.lang.Math.exp;
import static org.apache.commons.io.IOUtils.closeQuietly;


public class FrameIdentificationRelease {
	public static Pair<IdFeatureExtractor, TObjectDoubleHashMap<String>>
			parseParamFile(String paramsFile) throws IOException {
		final List<String> lines = Files.readLines(new File(paramsFile), Charsets.UTF_8);
		final IdFeatureExtractor featureExtractor = IdFeatureExtractor.fromName(lines.get(0));
		final TObjectDoubleHashMap<String> model = readModel(lines.subList(1, lines.size()));
		return Pair.of(featureExtractor, model);
	}

	public static TObjectDoubleHashMap<String> readModel(Collection<String> featureLines) {
		final TObjectDoubleHashMap<String> model = new TObjectDoubleHashMap<String>(featureLines.size());
		int count = 0;
		for (String line : featureLines) {
			String[] fields = line.split("\t");
			final String featureName = fields[0].trim();
			final Double featureValue = Double.parseDouble(fields[1].trim());
			model.put(featureName, featureValue);
			count++;
			if (count % 100000 == 0) System.err.print(count + " ");
		}
		return model;
	}

	public static TObjectDoubleHashMap<String> readOldModel(String idParamsFile) throws IOException {
		TObjectDoubleHashMap<String> params = new TObjectDoubleHashMap<String>();
		int count = 0;
		String line;
		final BufferedReader input = Files.newReader(new File(idParamsFile), Charsets.UTF_8);
		try {
			while ((line = input.readLine()) != null) {
				final String[] nameAndVal = line.split("\t");
				final String[] logAndSign = nameAndVal[1].split(", ");
				final double value = exp(Double.parseDouble(logAndSign[0]));
				final double sign = Boolean.parseBoolean(logAndSign[1]) ? 1.0 : -1.0;
				params.put(nameAndVal[0], value * sign);
				count++;
				if (count % 100000 == 0) System.err.print(count + " ");
			}
		} finally {
			closeQuietly(input);
		}
		return params;
	}

	public static Pair<String, String> getTokenRepresentation(String tokNum, String parse) {
		String[] tokNums = tokNum.split("_");
		List<Integer> indices = Lists.newArrayList();
		for (String tokNum1 : tokNums) {
			indices.add(Integer.parseInt(tokNum1));
		}
		return getTokenRepresentation(indices, Sentence.fromAllLemmaTagsArray(AllLemmaTags.readLine(parse)));
	}

	public static Pair<String, String> getTokenRepresentation(List<Integer> indices, Sentence sentence) {
		final List<Token> tokens = sentence.getTokens();
		List<String> actualTokens = Lists.newArrayList();
		for (int i : indices) {
			actualTokens.add(tokens.get(i).getForm());
		}
		return Pair.of(getFirstTokenAndCpos(indices, sentence), Joiner.on(" ").join(actualTokens));
	}

	public static String getFirstTokenAndCpos(List<Integer> indices, Sentence sentence) {
		if(indices.isEmpty()) return "";
		final Token firstToken = sentence.getTokens().get(indices.get(0));
		return firstToken.getForm().toLowerCase() + "." +
				nullToEmpty(firstToken.getPostag()).substring(0, 1).toLowerCase();

	}
}
