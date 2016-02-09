/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * OneLineDataCreation.java is part of SEMAFOR 2.0.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.FileReader;

import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;


/**
 * Methods for converting to the "all.lemma.tags" format, as described in training/data/README.md
 */
public class OneLineDataCreation {
	public static final String NETAG_SUFFIX = ".ne.tagged";
	public static final String TOKENIZED_SUFFIX = ".tokenized";
	public static final String CONLL_PARSED_SUFFIX = ".conll.parsed";
	public static final String ONELINE_SUFFIX = ".all.tags";
	// TODO(st): this shouldn't be hardcoded in
	public static final String DIR_ROOT = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData";
		
	public static void main(String[] args) throws IOException {
		String[] prefixes = {
				"framenet.original.sentences",
				"semeval.fulltrain.sentences",
				"semeval.fulldev.sentences",
				"semeval.fulltest.sentences"
		};
		for (String prefix : prefixes) {
			transFormIntoPerLineParse(prefix);
		}		
	}
	
	public static void transFormIntoPerLineParse(String prefix) throws IOException {
		prefix = DIR_ROOT + "/" + prefix;
		ArrayList<ArrayList<String>> parses = readCoNLLParses(prefix + CONLL_PARSED_SUFFIX);
		List<String> tokenizedSentences = ParsePreparation.readLines(prefix + TOKENIZED_SUFFIX);
		List<String> neTaggedSentences = ParsePreparation.readLines(prefix + NETAG_SUFFIX);
		ArrayList<String> perSentenceParses = getPerSentenceParses(parses,tokenizedSentences,neTaggedSentences);
		ParsePreparation.writeSentencesToFile(prefix + ONELINE_SUFFIX, perSentenceParses);
	}

	/**
	 * Zips the given lists of (dependency) parsed, tokenized, and NE tagged sentences into one list
	 * of one-line parses in the "all.lemma.tags" format.
	 *
	 * @param parses a list of dependency parsed sentences. each one is a list of strings
	 * @param tokenizedSentences a list of tokenized sentences
	 * @param neTaggedSentences a list of NE tagged sentences
	 */
	public static ArrayList<String> getPerSentenceParses(List<ArrayList<String>> parses,
														 List<String> tokenizedSentences,
														 List<String> neTaggedSentences) {
		ArrayList<String> result = new ArrayList<>();
		ArrayList<String> gatheredSentences;
		ArrayList<String> gatheredParses;
		ArrayList<String> gatheredNESentences;

		final int size = parses.size();
		for(int i : xrange(size)) {
			final String tokenizedSentence = tokenizedSentences.get(i);
			final ArrayList<String> parse = parses.get(i);
			final String neTaggedSentence = neTaggedSentences.get(i);
			gatheredSentences = new ArrayList<>();
			gatheredParses = new ArrayList<>();
			gatheredNESentences = new ArrayList<>();
			gatheredSentences.add(tokenizedSentence);
			gatheredParses.addAll(parse);
			gatheredNESentences.add(neTaggedSentence);
			String oneLineParse = processGatheredSentences(gatheredSentences, gatheredParses, gatheredNESentences);
			result.add(oneLineParse);
		}
		return result;
	}


	private static String processGatheredSentences(ArrayList<String> gatheredSentences,
												   ArrayList<String> gatheredParses,
												   ArrayList<String> gatheredNESents) {
		String result = "";
		int totalNumOfSentences = gatheredSentences.size();
		int totalTokens = 0;
		int[] tokenNums = new int[totalNumOfSentences];
		String neLine = "";
		for(int i = 0; i < totalNumOfSentences; i++) {
			String tokenizedSentence = gatheredSentences.get(i);
			StringTokenizer st = new StringTokenizer(tokenizedSentence);
			totalTokens += st.countTokens();
			tokenNums[i] = st.countTokens();
			while(st.hasMoreTokens()) {
				result += st.nextToken() + "\t";
			}
			st = new StringTokenizer(gatheredNESents.get(i));
			while(st.hasMoreTokens()) {
				String token = st.nextToken();
				int lastInd = token.lastIndexOf("_");
				String NE = token.substring(lastInd + 1);
				neLine += NE + "\t";
			}
		}
		result = totalTokens + "\t" + result;
		if(totalTokens != gatheredParses.size()) {
			System.out.println("Some problem: total number of tokens in gathered sentences not equal to gathered parses.");
			System.exit(0);
		}	
		int count = 0;
		String posTagSequence = "";
		String labelTagSequence = "";
		String parentTagSequence = "";
		int offset = 0;
		for(int i = 0; i < totalNumOfSentences; i++) {
			if(i > 0) {
				offset += tokenNums[i-1];
			}
			// parse CONLL format
			for(int j = 0; j < tokenNums[i]; j++) {
				String parseLine = gatheredParses.get(count).trim();
				StringTokenizer st = new StringTokenizer(parseLine, "\t");
				int countTokens = st.countTokens();
				if(countTokens != 10) {
					System.out.println("Parse line:" + parseLine + " does not have 10 tokens. Exiting.");
					System.exit(0);
				}
				st.nextToken();
				st.nextToken();
				st.nextToken();
				posTagSequence += st.nextToken().trim() + "\t";
				st.nextToken();
				st.nextToken();
				int parent = Integer.parseInt(st.nextToken().trim());
				if(parent != 0) {
					parent += offset;
				}
				parentTagSequence += parent + "\t";
				labelTagSequence += st.nextToken() + "\t";
				count++;
			}
		}	
		result += posTagSequence + labelTagSequence + parentTagSequence + neLine;
		result = result.trim();
		StringTokenizer st = new StringTokenizer(result, "\t");
		int tokensInFirstSent = Integer.parseInt(st.nextToken());
		String[] first = new String[5];
		for(int i = 0; i < 5; i ++) {
			first[i] = "";
			for(int j = 0; j < tokensInFirstSent; j++) {
				first[i] += st.nextToken().trim() + "\t";
			}
			first[i] = first[i].trim();
		}
		return result.trim();
	}	


	public static ArrayList<ArrayList<String>> readCoNLLParses(String conllParseFile) {
		ArrayList<ArrayList<String>> result = new ArrayList<>();
		try {
			BufferedReader bReader = new BufferedReader(new FileReader(conllParseFile));
			String line;
			ArrayList<String> thisParse = new ArrayList<>();
			while((line=bReader.readLine()) != null) {
				line=line.trim();
				if(line.equals("")) {
					result.add(thisParse);
					thisParse = new ArrayList<>();
				}
				else {
					thisParse.add(line);
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return result;
	}
}


