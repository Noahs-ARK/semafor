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

import java.util.ArrayList;
import java.util.StringTokenizer;
import edu.cmu.cs.lti.ark.fn.utils.LemmatizeStuff;


public class AllAnnotationsMergingWithoutNE {
	public static void main(String[] args) {
		// parse args
		final String tokenizedFile = args[0];
		final String conllParseFile = args[1];
		final String infile = args[2];
		final String stopWordsFile = args[3];
		final String wordNetConfigFile = args[4];
		final String outfile = args[5];

		ArrayList<String> tokenizedSentences = ParsePreparation.readSentencesFromFile(tokenizedFile);
		ArrayList<String> neSentences = findDummyNESentences(tokenizedSentences);
		ArrayList<ArrayList<String>> parses = OneLineDataCreation.readCoNLLParses(conllParseFile);
		ArrayList<String> perSentenceParses =
				OneLineDataCreation.getPerSentenceParses(parses, tokenizedSentences, neSentences);
		ParsePreparation.writeSentencesToTempFile(infile, perSentenceParses);
		LemmatizeStuff.lemmatize(stopWordsFile, wordNetConfigFile, infile, outfile);
	}
	
	public static ArrayList<String> findDummyNESentences(ArrayList<String> tokenizedSentences) {
		ArrayList<String> res = new ArrayList<String>();
		for(String sentence : tokenizedSentences) {
			final StringTokenizer st = new StringTokenizer(sentence.trim());
			String resSent = "";
			while(st.hasMoreTokens())
			{
				resSent += st.nextToken() + "_O ";
			}
			resSent = resSent.trim();
			res.add(resSent);
		}
		return res;
	}
} 
