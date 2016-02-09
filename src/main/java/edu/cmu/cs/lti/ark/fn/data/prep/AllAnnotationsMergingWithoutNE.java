/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * AllAnnotationsMergingWithoutNE.java is part of SEMAFOR 2.0.
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

import com.google.common.collect.Lists;
import edu.cmu.cs.lti.ark.fn.utils.LemmatizeStuff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Script to merge the POS tags, dependency parse, and lemmatized version of each sentence into one line
 */
public class AllAnnotationsMergingWithoutNE {
	public static void main(String[] args) throws IOException {
		// parse args
		final String tokenizedFile = args[0];
		final String conllParseFile = args[1];
		final String tmpParseFile = args[2];
		final String outfile = args[3];
		// merge all annotations into one combined file (*.all.lemma.tags)
		mergeAllAnnotations(tokenizedFile, conllParseFile, tmpParseFile, outfile);
	}

	/**
	 * Merges the POS tags, dependency parse, and lemmatized version of each sentence into one line
	 *
	 * @param tokenizedFile path to file of the tokenized sentences
	 * @param conllParseFile path to the file of dependency parsed sentences
	 * @param tmpParseFile path to a temporary file
	 * @param outfile path to file to which to write the combined sentences
	 */
	public static void mergeAllAnnotations(String tokenizedFile, String conllParseFile, String tmpParseFile,
										   String outfile) throws IOException {
		List<String> tokenizedSentences = ParsePreparation.readLines(tokenizedFile);
		ArrayList<String> neSentences = findDummyNESentences(tokenizedSentences);
		ArrayList<ArrayList<String>> parses = OneLineDataCreation.readCoNLLParses(conllParseFile);
		ArrayList<String> perSentenceParses =
				OneLineDataCreation.getPerSentenceParses(parses, tokenizedSentences, neSentences);
		ParsePreparation.writeSentencesToFile(tmpParseFile, perSentenceParses);
		LemmatizeStuff.lemmatize(tmpParseFile, outfile);
	}

	public static ArrayList<String> findDummyNESentences(List<String> tokenizedSentences) {
		ArrayList<String> res = Lists.newArrayList();
		for(String sentence : tokenizedSentences) {
			final StringTokenizer st = new StringTokenizer(sentence.trim());
			String resSent = "";
			while(st.hasMoreTokens()) {
				resSent += st.nextToken() + "_O ";
			}
			resSent = resSent.trim();
			res.add(resSent);
		}
		return res;
	}
} 
