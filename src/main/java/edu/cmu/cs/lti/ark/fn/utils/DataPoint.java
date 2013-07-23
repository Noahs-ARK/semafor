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
import java.util.Map;
import java.util.StringTokenizer;

import static java.lang.Integer.parseInt;

public class DataPoint {
	private DependencyParses parses;
	private String frameName;
	private String lexicalUnitName;	// e.g. 'cause.v'
	/* token indices of target phrase */
	private int[] targetTokenIdxs;
	private int sentNum;
	
	protected String dataSet;
	
	/**
	 * Maps token numbers in the sentence to corresponding character indices
	 * @see #processOrgLine(String)
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
		lexicalUnitName = tokens[1].intern();
		String[] tokNums = tokens[2].split("_");
		targetTokenIdxs = new int[tokNums.length];
		for(int j = 0; j < tokNums.length; j ++) {
			targetTokenIdxs[j] = parseInt(tokNums[j]);
		}
		Arrays.sort(targetTokenIdxs);
	}
	
	/**
	 * The inverse of processFrameLine(). Result does not include a trailing newline.
	 * @see #processFrameLine(String)
	 */
	public static String makeFrameLine(String frameName, String lexicalUnit, int[] tokenNums, String target, int sentNum) {
		String s = "";
		s += frameName + "\t" + lexicalUnit + "\t";
		for (int i=0; i<tokenNums.length; i++) {
			s += ((i==0) ? tokenNums[i] : "_" + tokenNums[i]);
		}
		s += "\t" + target + "\t" + sentNum;
		return s;
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

	public static final String FN13_LEXICON_EXEMPLARS = "exemplars";
	public static final String SEMEVAL07_TRAIN_SET = "train";
	public static final String SEMEVAL07_DEV_SET = "dev";
	public static final String SEMEVAL07_TEST_SET = "test";
	/** Sentence index ranges for documents in the train, dev, and test portions of the SemEval'07 data */
	protected static final Map<String,Map<String,? extends Range>> DOCUMENT_SENTENCE_RANGES = new THashMap<String,Map<String,? extends Range>>();
	static {
		{
		Map<String,Range0Based> exemplarMap = new THashMap<String,Range0Based>();
		exemplarMap.put("*", new Range0Based(0,139439,false));
		DOCUMENT_SENTENCE_RANGES.put(FN13_LEXICON_EXEMPLARS, exemplarMap);
		}
		
		/* Document descriptor: SubcorpusName/DocumentName
		 * Note that sentence indices are only unique within a particular data set
		
		train (18 documents, 1663 sentences)
		ANC/EntrepreneurAsMadonna	0	33
		ANC/HistoryOfJerusalem		171	292
		NTI/BWTutorial_chapter1		292     393
		NTI/Iran_Chemical			393     536
		NTI/Iran_Introduction		536     598
		NTI/Iran_Missile			598     778
		NTI/Iran_Nuclear			778     913
		NTI/Kazakhstan				913     942
		NTI/LibyaCountry1			942     983
		NTI/NorthKorea_ChemicalOverview		983     1055
		NTI/NorthKorea_NuclearCapabilities	1055    1085
		NTI/NorthKorea_NuclearOverview		1085    1206
		NTI/Russia_Introduction		1206    1247
		NTI/SouthAfrica_Introduction		1247    1300
		NTI/Syria_NuclearOverview			1300    1356
		NTI/Taiwan_Introduction		1356    1392
		NTI/WMDNews_062606			1392    1476
		PropBank/PropBankCorpus		1476    1801

		dev (4 documents, 251 sentences)
		ANC/StephanopoulosCrimes	146     178
		NTI/Iran_Biological			178     280
		NTI/NorthKorea_Introduction	280     329
		NTI/WMDNews_042106			329     397

		test (3 documents, 120 sentences)
		ANC/IntroOfDublin			0       67
		NTI/chinaOverview			67      106
		NTI/workAdvances			106     120
		*/

		{
		Map<String,Range0Based> trainMap = new THashMap<String,Range0Based>();
		trainMap.put("ANC/EntrepreneurAsMadonna",			new Range0Based(0,33,false));
		trainMap.put("ANC/HistoryOfJerusalem",				new Range0Based(171,292,false));
		trainMap.put("NTI/BWTutorial_chapter1",				new Range0Based(292,393,false));
		trainMap.put("NTI/Iran_Chemical",					new Range0Based(393,536,false));
		trainMap.put("NTI/Iran_Introduction",				new Range0Based(536,598,false));
		trainMap.put("NTI/Iran_Missile",					new Range0Based(598,778,false));
		trainMap.put("NTI/Iran_Nuclear",					new Range0Based(778,913,false));
		trainMap.put("NTI/Kazakhstan",						new Range0Based(913,942,false));
		trainMap.put("NTI/LibyaCountry1",					new Range0Based(942,983,false));
		trainMap.put("NTI/NorthKorea_ChemicalOverview",		new Range0Based(983,1055,false));
		trainMap.put("NTI/NorthKorea_NuclearCapabilities",	new Range0Based(1055,1085,false));
		trainMap.put("NTI/NorthKorea_NuclearOverview",		new Range0Based(1085,1206,false));
		trainMap.put("NTI/Russia_Introduction",				new Range0Based(1206,1247,false));
		trainMap.put("NTI/SouthAfrica_Introduction",		new Range0Based(1247,1300,false));
		trainMap.put("NTI/Syria_NuclearOverview",			new Range0Based(1300,1356,false));
		trainMap.put("NTI/Taiwan_Introduction",				new Range0Based(1356,1392,false));
		trainMap.put("NTI/WMDNews_062606",					new Range0Based(1392,1476,false));
		trainMap.put("PropBank/PropBankCorpus",				new Range0Based(1476,1801,false));
		DOCUMENT_SENTENCE_RANGES.put(SEMEVAL07_TRAIN_SET, trainMap);
		}
		
		{
		Map<String,Range0Based> devMap = new THashMap<String,Range0Based>();
		devMap.put("ANC/StephanopoulosCrimes",		new Range0Based(146,178,false));
		devMap.put("NTI/Iran_Biological",			new Range0Based(178,280,false));
		devMap.put("NTI/NorthKorea_Introduction",	new Range0Based(280,329,false));
		devMap.put("NTI/WMDNews_042106",			new Range0Based(329,397,false));
		DOCUMENT_SENTENCE_RANGES.put(SEMEVAL07_DEV_SET, devMap);
		}
		
		{
		Map<String,Range0Based> testMap = new THashMap<String,Range0Based>();
		testMap.put("ANC/IntroOfDublin",	new Range0Based(0,67,false));
		testMap.put("NTI/chinaOverview",	new Range0Based(67,106,false));
		testMap.put("NTI/workAdvances",		new Range0Based(106,120,false));
		DOCUMENT_SENTENCE_RANGES.put(SEMEVAL07_TEST_SET, testMap);
		}
	}
}
