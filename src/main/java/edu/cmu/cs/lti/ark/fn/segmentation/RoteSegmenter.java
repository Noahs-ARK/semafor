/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * RoteSegmenter.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.segmentation;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;

import gnu.trove.*;

import javax.annotation.Nullable;

public class RoteSegmenter
{
	public static final int MAX_LEN = 4;

	public static final ImmutableSet<String> FORBIDDEN_WORDS =
			ImmutableSet.of("a", "an", "as ", "for ", "i ", "in particular ",
					"it ", "of course ", "so ", "the ", "with");
	// if these words precede "of", "of" should not be discarded
	public static final ImmutableSet<String> PRECEDING_WORDS_OF =
			ImmutableSet.of("%", "all ", "face ", "few ", "half ", "majority ",
					"many ", "member ", "minority ", "more ", "most ", "much ",
					"none ", "one ", "only ", "part ", "proportion ",
					"quarter ", "share ", "some ", "third");
	// if these words follow "of", "of" should not be discarded
	public static final ImmutableSet<String> FOLLOWING_WORDS_OF =
			ImmutableSet.of("all", "group", "their", "them", "us");
	// all prepositions should be discarded
	public static final ImmutableSet<String> LOC_PREPS =
			ImmutableSet.of("above", "against", "at", "below", "beside", "by",
					"in", "on", "over", "under");
	public static final ImmutableSet<String> TEMPORAL_PREPS = ImmutableSet.of("after", "before");
	public static final ImmutableSet<String> DIR_PREPS = ImmutableSet.of("into", "through", "to");
	// description of the parse array format
	// TODO: would be better to just make a class for parses
	public static final int PARSE_TOKEN_ROW = 0;
	public static final int PARSE_POS_ROW = 1;
	public static final int PARSE_NE_ROW = 4;
	public static final int PARSE_LEMMA_ROW = 5;

	public static final Predicate<DependencyParse> isObject = new Predicate<DependencyParse>() {
		@Override
		public boolean apply(@Nullable DependencyParse parse) {
			return parse != null && parse.getLabelType().equals("OBJ");
		}
	};

	private DependencyParse[] mNodeList = null;
	private DependencyParse mParse = null;

	public static void main(String[] args) {
		RoteSegmenter seg = new RoteSegmenter();
		seg.roteSegmentation();
	}
	
	public RoteSegmenter() {
		mNodeList = null;
		mParse =  null;
	}

	public void roteSegmentation() {
		THashSet<String> allRelatedWords = (THashSet<String>)SerializedObjects.readSerializedObject("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/allRelatedWords.set.identity.extended");
		System.out.println(allRelatedWords.size());
		
		String state = "train";
		String trainParseFile=null;
		String tokenNumsFile=null;
		
		if(state.equals("train")) {
			trainParseFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/alldev.m45.parsed";
			tokenNumsFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/segmentationData/semeval.dev.tokenNums";	
		}
		else {
			trainParseFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulltrain.sentences.all.tags";
			tokenNumsFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/segmentationData/semeval.train.tokenNums";
		}
		ArrayList<String> parses = ParsePreparation.readSentencesFromFile(trainParseFile);
		ArrayList<String> tokenNums = ParsePreparation.readSentencesFromFile(tokenNumsFile);
		
		WordNetRelations mWNR = new WordNetRelations("/mal2/dipanjan/experiments/QG/QG4Entailment/data/stopwords.txt","file_properties.xml");
		ArrayList<String> segs = findSegmentation(tokenNums, parses, allRelatedWords, mWNR);
		ParsePreparation.writeSentencesToFile("segs.txt", segs);
	}

	/**
	 *
	 * @param parse tsv. first field is the number of tokens in the sentence
	 * @param allRelatedWords
	 * @return
	 */
	public String getHighRecallSegmentation(String parse, THashSet<String> allRelatedWords) {
		final StringTokenizer st = new StringTokenizer(parse, "\t");
		final int tokensInFirstSent = Integer.parseInt(st.nextToken());
		final String[][] data = new String[6][tokensInFirstSent];
		for(int k = 0; k < 6; k ++) {
			for(int j = 0; j < tokensInFirstSent; j++) {
				data[k][j] = st.nextToken().trim();
			}
		}
		final ArrayList<String> startInds = Lists.newArrayList();
		for(int i = 0; i < data[0].length; i ++) {
			startInds.add("" + i);
		}
		String tokNums = "";
		for(int i = MAX_LEN; i >= 1; i--) {
			for(int j = 0; j <= tokensInFirstSent-i; j++) {
				String ind = ""+j;
				if(!startInds.contains(ind)) continue;
				String lTok = "";
				for(int k = j; k < j + i; k++) {
					String pos = data[1][k];
					String cPos = pos.substring(0,1);
					String l = data[5][k];    
					lTok += l+"_"+cPos+" ";
				}
				lTok=lTok.trim();
				if(allRelatedWords.contains(lTok)) {
					String tokRep = "";
					for(int k = j; k < j + i; k ++) {
						tokRep += k+" ";
						ind = ""+k;
						startInds.remove(ind);
					}
					tokRep = tokRep.trim().replaceAll(" ", "_");
					tokNums += tokRep+"\t";
				}
			}				
		}
		tokNums = tokNums.trim();
		return tokNums;
	}

	/**
	 * Helper method for trimPrepositions. Determines whether a token should be kept or discarded
	 * based on hand-built rules
	 * @param idxStr the index of the token (or something with "_"?) in pData
	 * @param pData a 2d array containing the token, lemma, pos tag, and NE for each token
	 * @return
	 */
	private boolean shouldIncludeToken(final String idxStr, final String[][] pData) {
		final int numTokens = pData[PARSE_TOKEN_ROW].length;
		// include through fields with "_" in them
		if(idxStr.contains("_")) return true;

		final int idx = Integer.parseInt(idxStr);
		// look up the word in pData
		final String token = pData[PARSE_TOKEN_ROW][idx].toLowerCase().trim();
		final String pos = pData[PARSE_POS_ROW][idx].trim();
		final String lemma = pData[PARSE_LEMMA_ROW][idx].trim();
		// look up the preceding and following words in pData, if they exist
		String precedingWord = "";
		String precedingPOS = "";
		String precedingLemma = "";
		String precedingNE = "";
		if(idx >= 1) {
			precedingWord = pData[PARSE_TOKEN_ROW][idx-1].toLowerCase();
			precedingPOS = pData[PARSE_POS_ROW][idx-1];
			precedingLemma = pData[PARSE_LEMMA_ROW][idx-1];
			precedingNE = pData[PARSE_NE_ROW][idx-1];
		}
		String followingWord = "";
		String followingPOS = "";
		String followingNE = "";
		if(idx < numTokens - 1) {
			followingWord = pData[PARSE_TOKEN_ROW][idx+1].toLowerCase();
			followingPOS = pData[PARSE_POS_ROW][idx+1];
			followingNE = pData[PARSE_NE_ROW][idx+1];
		}

		if(FORBIDDEN_WORDS.contains(token)) return false;
		// the three types of prepositions should all be skipped
		if(LOC_PREPS.contains(token)) return false;
		if(DIR_PREPS.contains(token)) return false;
		if(TEMPORAL_PREPS.contains(token)) return false;

		if(pos.startsWith("PR") || pos.startsWith("CC") || pos.startsWith("IN") || pos.startsWith("TO")) return false;
		// skip "of course" and "in particular"
		if(token.equals("course") && precedingWord.equals("of")) return false;
		if(token.equals("particular") && precedingWord.equals("in")) return false;

		if(token.equals("of")) {
			if(PRECEDING_WORDS_OF.contains(precedingLemma)) return true;
			if(FOLLOWING_WORDS_OF.contains(followingWord)) return true;

			if(precedingPOS.startsWith("JJ") || precedingPOS.startsWith("CD")) return true;

			if(followingPOS.startsWith("CD")) return true;

			if(followingPOS.startsWith("DT")) {
				if(idx < numTokens - 2) {
					final String followingFollowingPOS = pData[PARSE_POS_ROW][idx+2];
					if(followingFollowingPOS.startsWith("CD")) return true;
				}
			}
			return followingNE.startsWith("GPE") ||
					followingNE.startsWith("LOCATION") ||
					precedingNE.startsWith("CARDINAL");
		}

		if(token.equals("will")) return !pos.equals("MD");

		// TODO: mNodeList depends on methods being called in a certain order. not nice. also, no guarantee idx+1 in range
		final DependencyParse headNode = mNodeList[idx+1];
		if(lemma.equals("have")) return Iterables.any(headNode.getChildren(), isObject);

		return !lemma.equals("be");
	}
	
	/**
	 * Removes prepositions from the given sentence
	 *
	 * @param tokenIndices a tsv row of token indices
	 * @param pData an array of parse data. each column is a word. relevant rows are:  0: token, 1: pos, 5: lemma
	 * @return
	 */
	public String trimPrepositions(String tokenIndices, String[][] pData) {
		final String[] candidateTokens = tokenIndices.trim().split("\t");
		final int numTokens = pData[PARSE_TOKEN_ROW].length;

		final ArrayList<String> result = Lists.newArrayListWithExpectedSize(numTokens);
		for(String candidateTokenIdxStr: candidateTokens) {
			if(shouldIncludeToken(candidateTokenIdxStr, pData)) {
				result.add(candidateTokenIdxStr);
			}
		}
		return Joiner.on("\t").join(result).trim();
	}

	/**
	 *
	 * @param tokenNums the last tsv field is the index of the sentence in `parses`
	 * @param parses a list of
	 * @param allRelatedWords
	 * @return
	 */
	public ArrayList<String> findSegmentationForTest(ArrayList<String> tokenNums, 
			ArrayList<String> parses, 
			THashSet<String> allRelatedWords) {
		final ArrayList<String> result = Lists.newArrayList();
		for(String tokenNum: tokenNums) {
			final String gold = tokenNum.trim();
			final String[] tokens = tokenNum.split("\t");

			// the last tsv field is the index of the sentence in `parses`
			final int sentNum = Integer.parseInt(tokens[tokens.length - 1]);
			final String parse = parses.get(sentNum);
			String tokNums = getHighRecallSegmentation(parse, allRelatedWords);

			StringTokenizer st = new StringTokenizer(parse.trim(), "\t");
			final int tokensInFirstSent = Integer.parseInt(st.nextToken());

			// what is this magic number 6? (sthomson)
			String[][] data = new String[6][tokensInFirstSent];
			for(int k = 0; k < 6; k ++) {
				data[k] = new String[tokensInFirstSent];
				for(int j = 0; j < tokensInFirstSent; j ++) {
					data[k][j] = "" + st.nextToken().trim();
				}
			}
			mParse = DependencyParse.processFN(data, 0.0);
			mNodeList = DependencyParse.getIndexSortedListOfNodes(mParse);
			mParse.processSentence();
			if(!tokNums.trim().equals("")) {
				tokNums = trimPrepositions(tokNums, data);
			}
			final String line = getTestLine(gold, tokNums).trim() + "\t" + sentNum;
			result.add(line.trim());
		}		
		return result;
	}

	public String getTestLine(String goldTokens, String actualTokens) {
		final ArrayList<String> result = Lists.newArrayList();
		final ArrayList<String> goldList = Lists.newArrayList(goldTokens.trim().split("\t"));
		final ArrayList<String> actList = Lists.newArrayList(actualTokens.trim().split("\t"));
		for(String aGoldList : goldList) {
			result.add(aGoldList.trim() + "#true");
		}
		for (String anActList : actList) {
			final String tokNum = anActList.trim();
			if (!goldList.contains(tokNum)) {
				result.add(tokNum + "#true");
			}
		}
		return Joiner.on("\t").join(result);
	}

	public ArrayList<String> findSegmentation(ArrayList<String> tokenNums, 
													 ArrayList<String> parses, 
													 THashSet<String> allRelatedWords, 
													 WordNetRelations mWNR)
	{
		ArrayList<String> result = new ArrayList<String>();
		for(String tokenNum: tokenNums)
		{
			String[] toks = tokenNum.split("\t");
			String gold = "";
			for(int i = 0; i < toks.length-1; i ++)
				gold += toks[i]+"\t";
			gold=gold.trim();
			int sentNum = Integer.parseInt(toks[toks.length-1]);
			String parse = parses.get(sentNum);
			String tokNums = getHighRecallSegmentation(parse,allRelatedWords);
			StringTokenizer st = new StringTokenizer(parse.trim(),"\t");
			int tokensInFirstSent = Integer.parseInt(st.nextToken());
			String[][] data = new String[5][tokensInFirstSent];
			for(int k = 0; k < 5; k ++)
			{
				data[k]=new String[tokensInFirstSent];
				for(int j = 0; j < tokensInFirstSent; j ++)
				{
					data[k][j]=""+st.nextToken().trim();
				}
			}
			mParse = DependencyParse.processFN(data, 0.0);
			mNodeList = DependencyParse.getIndexSortedListOfNodes(mParse);
			mParse.processSentence();
			if(!tokNums.trim().equals(""))
				tokNums=trimPrepositions(tokNums, data);
			String line = "zzzz\t"+gold+"#"+tokNums;
			String line1 = getActualTokenLine(gold,tokNums,data);
			System.out.println(line1+"\n"+mParse.getSentence()+"\n");
			result.add(line);
		}		
		return result;
	}	
	
	public String getActualTokenLine(String goldTokens, String actualTokens, String[][] data)
	{
		String result = "";
		ArrayList<String> goldList = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(goldTokens.trim(),"\t");
		while(st.hasMoreTokens())
		{
			goldList.add(st.nextToken());
		}
		ArrayList<String> actList = new ArrayList<String>();
		st = new StringTokenizer(actualTokens.trim(),"\t");
		while(st.hasMoreTokens())
		{
			actList.add(st.nextToken());
		}		
		
		int goldSize = goldList.size();
		for(int i = 0; i < goldSize; i ++)
		{
			String tokNum = goldList.get(i).trim();
			String[] toks = tokNum.split("_");
			String token = "";
			for(int j = 0; j < toks.length; j ++)
			{
				int num = Integer.parseInt(toks[j]);
				token += data[0][num]+"_"+data[1][num]+" ";
			}
			token=token.trim();
			result += token+"_"+tokNum+"\t";
		}	
		result=result.trim()+"\n";
		int actSize = actList.size();
		for(int i = 0; i < actSize; i ++)
		{
			String tokNum = actList.get(i).trim();
			String[] toks = tokNum.split("_");
			String token = "";
			for(int j = 0; j < toks.length; j ++)
			{
				int num = Integer.parseInt(toks[j]);
				token += data[0][num]+"_"+data[1][num]+" ";
			}
			token=token.trim();
			result += token+"_"+tokNum+"\t";
		}
		return result.trim();
	}	
}
