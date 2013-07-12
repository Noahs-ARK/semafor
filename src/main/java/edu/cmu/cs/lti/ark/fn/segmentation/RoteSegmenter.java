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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Token;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Iterables.any;
import static com.google.common.primitives.Ints.min;
import static edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags.*;
import static edu.cmu.cs.lti.ark.util.IntRanges.range;
import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;

public class RoteSegmenter extends Segmenter {
	// the maximum length of ngrams we'll look for
	public static final int MAX_LEN = 4;

	private static final ImmutableSet<String> FORBIDDEN_WORDS =
			ImmutableSet.of("a", "an", "as", "for", "i", "in particular",
					"it", "of course", "so", "the", "with");
	// if these words precede "of", "of" should not be discarded
	private static final ImmutableSet<String> PRECEDING_WORDS_OF =
			ImmutableSet.of("%", "all", "face", "few", "half", "majority",
					"many", "member", "minority", "more", "most", "much",
					"none", "one", "only", "part", "proportion",
					"quarter", "share", "some", "third");
	// if these words follow "of", "of" should not be discarded
	private static final ImmutableSet<String> FOLLOWING_WORDS_OF =
			ImmutableSet.of("all", "group", "their", "them", "us");
	// all prepositions should be discarded
	private static final ImmutableSet<String> LOC_PREPS =
			ImmutableSet.of("above", "against", "at", "below", "beside", "by",
					"in", "on", "over", "under");
	private static final ImmutableSet<String> TEMPORAL_PREPS = ImmutableSet.of("after", "before");
	private static final ImmutableSet<String> DIR_PREPS = ImmutableSet.of("into", "through", "to");
	private static final ImmutableSet<String> FORBIDDEN_POS_PREFIXES = ImmutableSet.of("PR", "CC", "IN", "TO");
	private static final ImmutableSet<String> DIRECT_OBJECT_LABELS = ImmutableSet.of("OBJ", "DOBJ");  // accommodates MST labels and Stanford labels

	public static final Predicate<DependencyParse> isObject = new Predicate<DependencyParse>() {
		@Override public boolean apply(@Nullable DependencyParse parse) {
			return parse != null && DIRECT_OBJECT_LABELS.contains(parse.getLabelType().toUpperCase());
		}
	};

	protected final Set<String> allRelatedWords;

	public RoteSegmenter(Set<String> allRelatedWords) {
		this.allRelatedWords = allRelatedWords;
	}

	public List<List<Integer>> getSegmentation(Sentence sentence) {
		final int numTokens = sentence.getTokens().size();
		// start indices that we haven't used yet
		final Set<Integer> remainingStartIndices = Sets.newHashSet(xrange(numTokens));
		final ImmutableList.Builder<List<Integer>> allNgramIndices = ImmutableList.builder();  // results

		final List<String> lemmas = getLemmasAndCoursePos(sentence);

		// look for ngrams, backing off to smaller n
		for(int n : range(1, MAX_LEN + 1).asList().reverse()) {
			for(int start : xrange(numTokens - n + 1)) {
				if(!remainingStartIndices.contains(start)) continue;
				final int end = start + n;
				final List<Integer> ngramIndices = ImmutableList.copyOf(xrange(start, end));
				final String ngramLemmas = Joiner.on(" ").join(lemmas.subList(start, end));
				if(allRelatedWords.contains(ngramLemmas)) {
					// found a good ngram, add it to results, and remove it from startIndices so we don't overlap later
					allNgramIndices.add(ngramIndices);
					remainingStartIndices.removeAll(ngramIndices);
				}
			}
		}
		return trimPrepositions(allNgramIndices.build(), sentence.toAllLemmaTagsArray());
	}

	private List<String> getLemmasAndCoursePos(Sentence sentence) {
		return Lists.transform(sentence.getTokens(), new Function<Token, String>() {
			@Override public String apply(@Nullable Token token) {
				assert token != null;
				final String postag = nullToEmpty(token.getPostag());
				return token.getLemma() + "_" + postag.substring(0, min(1, postag.length()));
			}
		});
	}

	/**
	 * Helper method for trimPrepositions. Determines whether an ngram should be kept or discarded
	 * based on hand-built rules
	 *
	 * @param idxs "_" joined indices of the ngram tokens in pData
	 * @param pData a 2d array containing the token, lemma, pos tag, and NE for each token
	 */
	public static boolean shouldIncludeToken(final List<Integer> idxs, final String[][] pData, DependencyParse[] mNodeList) {
		final int numTokens = pData[PARSE_TOKEN_ROW].length;
		// always include ngrams, n > 1
		if (idxs.size() > 1) return true;
		final int idx = idxs.get(0);
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
			precedingWord = pData[PARSE_TOKEN_ROW][idx-1].toLowerCase().trim();
			precedingPOS = pData[PARSE_POS_ROW][idx-1].trim();
			precedingLemma = pData[PARSE_LEMMA_ROW][idx-1].trim();
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
		if(LOC_PREPS.contains(token)) return false;
		if(DIR_PREPS.contains(token)) return false;
		if(TEMPORAL_PREPS.contains(token)) return false;
		if(FORBIDDEN_POS_PREFIXES.contains(pos.substring(0, min(2, pos.length())))) return false;
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
		// remove modal "will"s and "have"s
		if(token.equals("will")) return !pos.equals("MD");
		if(idx < mNodeList.length) {
			// TODO: Why idx+1?
			final DependencyParse headNode = mNodeList[idx+1];
			if(lemma.equals("have")) return any(headNode.getChildren(), isObject);
		}
		// remove all forms of "be"
		return !lemma.equals("be");
	}
}
