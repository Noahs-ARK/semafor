/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * DataPoint.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.utils;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.Range;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParses;
import gnu.trove.THashMap;

import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import static java.lang.Integer.parseInt;

public class DataPoint {
	private DependencyParses parses;
	private String frameName;
	/* token indices of target phrase */
	private int[] targetTokenIdxs;
	private int sentNum;
	
	protected String dataSet;
	
	/**
	 * Maps token numbers in the sentence to corresponding character indices
	 * @see #getCharacterIndicesForToken(int)
	 */
	private THashMap<Integer,Range0Based> tokenIndexMap;

	// for benefit of subclasses
	protected DataPoint() { }

	public DataPoint(DependencyParses parses) {
		this.parses = parses;
	}

	protected DataPoint(DependencyParses parses, String dataSet) {
		this(parses);
		this.dataSet = dataSet;
	}

	/**
	 * Given a sentence tokenized with space separators, populates tokenIndexMap with mappings 
	 * from token numbers to strings in the format StartCharacterOffset\tEndCharacterOffset
	 */
	public void processOrgLine(String tokenizedSentence) {
		final StringTokenizer st = new StringTokenizer(tokenizedSentence.trim(), " ", true);
		final THashMap<Integer, Range0Based> localTokenIndexMap = new THashMap<Integer, Range0Based>();
		int count = 0;
		int tokNum = 0;
		while(st.hasMoreTokens()) {
			String token = st.nextToken();
			if(token.equals(" ")) {
				count++;
				continue;
			}
			token = token.trim();
			int start = count;
			int end = count + token.length() - 1;
			localTokenIndexMap.put(tokNum, new Range0Based(start,end));
			tokNum++;
			count += token.length();
		}
		tokenIndexMap = localTokenIndexMap;
	}

	public void processFrameLine(String frameLine) {
		// tokens are separated by tabs
		// tokens[0]: frame name
		// tokens[1]: lexical unit
		// tokens[2]: target token indexes (0-based), separated by "_"
		// tokens[3]: target word(s), separated by " "
		// tokens[4]: sentence number
		final String[] tokens = frameLine.split("\t");
		frameName = tokens[0].intern();
		sentNum = parseInt(tokens[4]);
		// The above 3 lines are duplicated in parseFrameNameAndSentenceNum()
		String[] tokNums = tokens[2].split("_");
		targetTokenIdxs = new int[tokNums.length];
		for(int j = 0; j < tokNums.length; j ++) {
			targetTokenIdxs[j] = parseInt(tokNums[j]);
		}
		Arrays.sort(targetTokenIdxs);
	}

	protected static Pair<String,Integer> parseFrameNameAndSentenceNum(String frameLine) {
		// A subset of the code in processFrameLine()
		String[] toks = frameLine.split("\t");
		String frameName = toks[0].intern();
		int sentNum = Integer.parseInt(toks[4]);
		return new Pair<String,Integer>(frameName, sentNum);
	}
	
	public DependencyParses getParses() {
		return parses;
	}
	
	public String getFrameName() {
		return frameName;
	}

	public int[] getTargetTokenIdxs() {
		return targetTokenIdxs;
	}
	
	public int getSentenceNum() {
		return sentNum;
	}

	public static DependencyParse[] buildParsesForLine(String parseLine) {
		StringTokenizer st = new StringTokenizer(parseLine, "\t");
		int numWords = Integer.parseInt(st.nextToken());	// number of word tokens in the sentence
		String[] parts = new String[6];
		
		String nextToken = st.nextToken().trim();
		for(int p = 0; p < 6; p ++)	// 0=>word tokens; 1=>POS tags; 2=>dependency types; 3=>parent indices; 4=>NE tags; 5=>lemmas from WordNet
		{
			parts[p]="";
			while(true) {
				for(int j = 0; j < numWords; j ++) {
					String tkn = (j==0) ? nextToken : st.nextToken().trim();
					parts[p] += tkn+"\t";
				}
				parts[p]=parts[p].trim();
				
				if (st.hasMoreElements()) {
					nextToken = st.nextToken().trim();
					if (nextToken.equals("|")) {	// the | symbol (with tabs on either side) indicates that another series of tokens is present, e.g. k-best list of parses or POS taggings
						parts[p] += "\t||\t";
						nextToken = st.nextToken().trim();
						continue;	// get 'numWords' more tokens for this part of the analysis
					}
				}
				break;
			}
		}
		DependencyParse[] dependencyParses = DependencyParse.buildParseTrees(parts, 0.0);
		for (DependencyParse parse : dependencyParses)
			parse.processSentence();
		return dependencyParses;
	}

	public Range getCharacterIndicesForToken(int tokenNum) {
		return tokenIndexMap.get(tokenNum);
	}

	public List<Range0Based> getTokenStartEnds(boolean mergeAdjacent) {
		final List<Range0Based> result = Lists.newArrayList();
		Optional<Range0Based> oCurrent = Optional.absent();
		for (int tknNum : targetTokenIdxs) {
			if (!oCurrent.isPresent()) {
				oCurrent = Optional.of(new Range0Based(tknNum, tknNum));
			} else {
				final Range0Based current = oCurrent.get();
				if (mergeAdjacent && current.start == tknNum - 1) {
					// merge with previous
					oCurrent = Optional.of(new Range0Based(current.start, tknNum));
				} else {
					// done with group of consecutive tokens
					result.add(current);
					oCurrent = Optional.of(new Range0Based(tknNum, tknNum));
				}
			}
		}
		// add the final span
		if (oCurrent.isPresent()) result.add(oCurrent.get());
		return result;
	}

	public List<Range0Based> getCharStartEnds(List<Range0Based> tokenRanges) {
		final List<Range0Based> result = Lists.newArrayList();
		for (Range0Based tokenRange : tokenRanges) {
			final Range0Based charRange =
					new Range0Based(
							tokenIndexMap.get(tokenRange.start).start,
							tokenIndexMap.get(tokenRange.end).end
					);
			result.add(charRange);
		}
		return result;
	}
}
