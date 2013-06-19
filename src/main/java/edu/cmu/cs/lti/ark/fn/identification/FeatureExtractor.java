/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 *
 * FeatureExtractor.java is part of SEMAFOR 2.0.
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

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.IFeatureExtractor;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags.*;
import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;

/**
 * Extracts features for the frame identification model
 */
public class FeatureExtractor implements IFeatureExtractor {
	private static final Joiner SPACE = Joiner.on(" ");
	private static final Joiner UNDERSCORE = Joiner.on("_");

	public static IntCounter<String> extractFeatures(String frameName,
													 int[] tokenNums,
													 String hiddenWord,
													 String[][] allLemmaTags,
													 WordNetRelations wnr,
													 THashMap<String, THashSet<String>> wnCacheMap,
													 THashMap<String, String> lemmaCache,
													 DependencyParse parse) {
		final IRelations wnRelations = new WNRelations(wnr, wnCacheMap);
		final ILemmatizer lemmatizer = new CachingWordNetLemmatizer(wnr, lemmaCache);
		boolean parseHasLemmas = false;
		return extractFeatures(frameName, tokenNums, hiddenWord, allLemmaTags, parse, wnRelations, lemmatizer, parseHasLemmas);
	}

	public static IntCounter<String> extractFeaturesLessMemory(String frameName,
															   int[] tokenNums,
															   String hiddenWord,
															   String[][] allLemmaTags,
															   Map<String, Set<String>> relatedWordsForWord,
															   Map<String, Map<String, Set<String>>> revisedRelationsMap,
															   Map<String, String> mHVLemmas,
															   DependencyParse parse) {
		final IRelations wnRelations = new CachedRelations(revisedRelationsMap, relatedWordsForWord);
		final ILemmatizer lemmatizer = new CachedLemmatizer(mHVLemmas);
		boolean parseHasLemmas = true;
		return extractFeatures(frameName, tokenNums, hiddenWord, allLemmaTags, parse, wnRelations, lemmatizer, parseHasLemmas);
	}

	private static String getCpostag(String postag) {
		return postag.substring(0, 1).toLowerCase();
	}

	/**
	 * Extract features for a (frame, target, hidden l.u.) tuple
	 *
	 * @param frameName the name of the candidate frame
	 * @param targetTokenIdxs the token indexes (0-indexed) of the target
	 * @param hiddenLexUnit the latent l.u.
	 * @param allLemmaTags the sentence in AllLemmaTags format
	 * @param parse the dependency parse for the sentence
	 * @param wnRelations a way to look up all the WordNet relations between target and the latent l.u.
	 * @param lemmatizer a way to look up lemmas for (token, postag) pairs
	 * @param parseHasLemmas whether or not allLemma already includes lemmas for each token
	 * @return a map from feature name -> count
	 */
	public static IntCounter<String> extractFeatures(String frameName,
													 int[] targetTokenIdxs,
													 String hiddenLexUnit,
													 String[][] allLemmaTags,
													 DependencyParse parse,
													 IRelations wnRelations,
													 ILemmatizer lemmatizer,
													 boolean parseHasLemmas) {
		// Get lemmas and postags for prototype
		// hiddenLexUnit is in format: "form1_pos1 form2_pos2 ... formn_posn"
		final String[] hiddenTokenAndPos = hiddenLexUnit.split(" ");
		final List<String> hiddenTokenAndCpostags = Lists.newArrayList(hiddenTokenAndPos.length);
		//final List<String> hiddenLemmas = Lists.newArrayList(hiddenTokenAndPos.length);
		final List<String> hiddenCpostags = Lists.newArrayList(hiddenTokenAndPos.length);
		final List<String> hiddenLemmaAndCpostags = Lists.newArrayList(hiddenTokenAndPos.length);
		for (String hiddenTok : hiddenTokenAndPos) {
			final String[] arr = hiddenTok.split("_");
			final String form = arr[0];
			final String postag = arr[1];
			final String cpostag = getCpostag(postag);
			final String lemma = lemmatizer.getLowerCaseLemma(form, postag);
			hiddenCpostags.add(cpostag);
			//hiddenLemmas.add(lemma);
			hiddenTokenAndCpostags.add(form + "_" + cpostag);
			hiddenLemmaAndCpostags.add(lemma + "_" + cpostag);
		}
		final String hiddenTokenAndCpostagsStr = UNDERSCORE.join(hiddenTokenAndCpostags);
		//final String hiddenLemmasStr = UNDERSCORE.join(hiddenLemmas);
		final String hiddenCpostagsStr = UNDERSCORE.join(hiddenCpostags);
		final String hiddenLemmaAndCpostagsStr = UNDERSCORE.join(hiddenLemmaAndCpostags);

		// Get lemmas and postags for target
		final List<String> actualTokenAndCpostags = Lists.newArrayList(targetTokenIdxs.length);
		//final List<String> actualLemmas = Lists.newArrayList(targetTokenIdxs.length);
		final List<String> actualCpostags = Lists.newArrayList(targetTokenIdxs.length);
		final List<String> actualLemmaAndCpostags = Lists.newArrayList(targetTokenIdxs.length);
		Arrays.sort(targetTokenIdxs);
		for (int tokenIdx : targetTokenIdxs) {
			final String form = allLemmaTags[PARSE_TOKEN_ROW][tokenIdx];
			final String postag = allLemmaTags[PARSE_POS_ROW][tokenIdx];
			final String cpostag = getCpostag(postag);
			final String lemma = parseHasLemmas ? allLemmaTags[PARSE_LEMMA_ROW][tokenIdx]
											: lemmatizer.getLowerCaseLemma(form, postag);
			actualTokenAndCpostags.add(form + "_" + cpostag);
			//actualLemmas.add(lemma);
			actualCpostags.add(cpostag);
			actualLemmaAndCpostags.add(lemma + "_" + cpostag);
		}
		final String actualTokenAndCpostagsStr = UNDERSCORE.join(actualTokenAndCpostags);
		//final String actualLemmasStr = UNDERSCORE.join(actualLemmas);
		final String actualCpostagsStr = UNDERSCORE.join(actualCpostags);
		final String actualLemmaAndCpostagsStr = UNDERSCORE.join(actualLemmaAndCpostags);

		final Set<String> relations = wnRelations.getRelations(SPACE.join(actualTokenAndCpostags), SPACE.join(hiddenTokenAndCpostags));

		final IntCounter<String> featureMap = new IntCounter<String>();
		/*
		 * base features
		 * will be conjoined in various ways
		 * (always conjoined with the frame name)
		 */
		final String frameFtr = "f:" + frameName;
		final String actualCpostagsFtr = "aP:" + actualCpostagsStr;
		final String actualLemmaAndCpostagsFtr = "aLP:" + actualLemmaAndCpostagsStr;
		final String hiddenTokenAndCpostagsFtr = "hT:" + hiddenTokenAndCpostagsStr;
		//final String hiddenLemmasFtr = "hL:" + hiddenLemmasStr;
		final String hiddenCpostagsFtr = "hP:" + hiddenCpostagsStr;
		final String hiddenLemmaAndCpostagsFtr = "hLP:" + hiddenLemmaAndCpostagsStr;

		// add a feature for each word in the sentence
		for (int tokenIdx : xrange(allLemmaTags[0].length)) {
			final String form = allLemmaTags[PARSE_TOKEN_ROW][tokenIdx];
			final String postag = allLemmaTags[PARSE_POS_ROW][tokenIdx];
			final String cpostag = getCpostag(postag);
			final String lemma = parseHasLemmas ? allLemmaTags[PARSE_LEMMA_ROW][tokenIdx]
					: lemmatizer.getLowerCaseLemma(form, postag);
			featureMap.increment("sTP:" + form + "_" + cpostag);
			featureMap.increment("sLP:" + lemma + "_" + cpostag);
		}

		featureMap.increment(UNDERSCORE.join(
				hiddenTokenAndCpostagsFtr,
				frameFtr));
//		featureMap.increment(UNDERSCORE.join(
//				hiddenLemmasFtr,
//				frameFtr));
		featureMap.increment(UNDERSCORE.join(
				hiddenLemmaAndCpostagsFtr,
				frameFtr));

		// extract features for each WordNet relation by which the target and prototype are connected
		for (String relation : relations) {
			if (relation.equals(WordNetRelations.NO_RELATION)) continue;
			final String relationFeature = "tRLn:" + relation;
			featureMap.increment(UNDERSCORE.join(
					relationFeature,
					frameFtr));
			featureMap.increment(UNDERSCORE.join(
					relationFeature,
					hiddenTokenAndCpostagsStr,
					frameFtr));
			featureMap.increment(UNDERSCORE.join(
					relationFeature,
					hiddenTokenAndCpostagsStr,
					hiddenCpostagsFtr,
					actualCpostagsFtr,
					frameFtr));
		}

		if (hiddenTokenAndCpostagsStr.equals(actualTokenAndCpostagsStr)) {
			final String tokenMatchFtr = "sTs";
			featureMap.increment(UNDERSCORE.join(
					tokenMatchFtr,
					frameFtr));
			featureMap.increment(UNDERSCORE.join(
					tokenMatchFtr,
					hiddenTokenAndCpostagsFtr,
					frameFtr));
			featureMap.increment(UNDERSCORE.join(
					tokenMatchFtr,
					actualCpostagsFtr,
					hiddenCpostagsFtr,
					frameFtr));
			featureMap.increment(UNDERSCORE.join(
					tokenMatchFtr,
					actualLemmaAndCpostagsFtr,
					hiddenLemmaAndCpostagsFtr,
					frameFtr));
		}
		if (hiddenLemmaAndCpostagsStr.equals(actualLemmaAndCpostagsStr)) {
			final String lemmaMatchFtr = "sLs";
			featureMap.increment(UNDERSCORE.join(
					lemmaMatchFtr,
					frameFtr));
			featureMap.increment(UNDERSCORE.join(
					lemmaMatchFtr,
					hiddenTokenAndCpostagsFtr,
					frameFtr));
			featureMap.increment(UNDERSCORE.join(
					lemmaMatchFtr,
					actualCpostagsFtr,
					hiddenCpostagsFtr,
					frameFtr));
			featureMap.increment(UNDERSCORE.join(
					lemmaMatchFtr,
					actualLemmaAndCpostagsFtr,
					hiddenLemmaAndCpostagsFtr,
					frameFtr));
		}

		/*
		 * syntactic features
		 */
		final DependencyParse[] sortedNodes = parse.getIndexSortedListOfNodes();
		final DependencyParse head = DependencyParse.getHeuristicHead(sortedNodes, targetTokenIdxs);
		final String headCpostag = getCpostag(head.getPOS());

		final List<DependencyParse> children = head.getChildren();

		final SortedSet<String> depLabels = Sets.newTreeSet(); // unordered set of arc labels of children
		for (DependencyParse child : children) {
			depLabels.add(child.getLabelType().toLowerCase());
		}
		final String dependencyFtr = "d:" + UNDERSCORE.join(depLabels);
		featureMap.increment(UNDERSCORE.join(
				dependencyFtr,
				frameFtr));

		if (headCpostag.equals("v")) {
			final List<String> subcat = Lists.newArrayList(children.size()); // ordered arc labels of children
			for (DependencyParse child : children) {
				final String labelType = child.getLabelType().toLowerCase();
				if (!labelType.equals("sub") && !labelType.equals("p") && !labelType.equals("cc")) {
					// TODO(smt): why exclude "sub"?
					subcat.add(labelType);
				}
			}
			final String subcatFtr = "sC:" + UNDERSCORE.join(subcat);
			featureMap.increment(UNDERSCORE.join(
					subcatFtr,
					frameFtr));
		}

		final DependencyParse parent = head.getParent();
		final String parentPosFtr = "pP:" + ((parent == null) ? "NULL" : parent.getPOS());
		featureMap.increment(UNDERSCORE.join(
				parentPosFtr,
				frameFtr));
		final String parentLabelFtr = "pL:" + ((parent == null) ? "NULL" : parent.getLabelType());
		featureMap.increment(UNDERSCORE.join(
				parentLabelFtr,
				frameFtr));

		return featureMap;
	}

	/** Finds token relationships */
	public static interface IRelations {
		public Set<String> getRelations(String actualTokens, String hiddenUnitTokens);
	}

	/** Finds token relationships using the WordNetRelations object */
	public static class WNRelations implements IRelations {
		private final WordNetRelations wnr;
		private final THashMap<String, THashSet<String>> wnCacheMap;
		private final ReentrantReadWriteLock.WriteLock writeLock;

		public WNRelations(WordNetRelations wnr, THashMap<String, THashSet<String>> wnCacheMap) {
			this.wnr = wnr;
			this.wnCacheMap = wnCacheMap;
			writeLock = new ReentrantReadWriteLock().writeLock();
		}

		public Set<String> getRelations(String actualTokens, String hiddenUnitTokens) {
			// TODO(smt): use a LoadingCache
			Set<String> relations;
			final String sWordLower = hiddenUnitTokens.toLowerCase();
			final String tWordLower = actualTokens.toLowerCase();
			final String pair = sWordLower + "\t" + tWordLower;
			if (wnCacheMap != null) {
				if (!wnCacheMap.contains(pair)) {
					relations = ImmutableSet.of(WordNetRelations.NO_RELATION);
				} else {
					relations = wnCacheMap.get(pair);
				}
			} else {
				writeLock.lock();
				try {
					relations = wnr.getRelations(sWordLower, tWordLower);
				} finally {
					writeLock.unlock();
				}
			}
			return relations;
		}
	}

	/** Finds relationships without the WordNetRelations object */
	public static class CachedRelations implements IRelations {
		private final Map<String, Map<String, Set<String>>> revisedRelationsMap;
		private final Map<String, Set<String>> relatedWordsForWord;

		public CachedRelations(Map<String, Map<String, Set<String>>> revisedRelationsMap,
							   Map<String, Set<String>> relatedWordsForWord) {
			this.revisedRelationsMap = revisedRelationsMap;
			this.relatedWordsForWord = relatedWordsForWord;
		}

		public Set<String> getRelations(String actualTokens, String hiddenUnitTokens) {
			Set<String> relations;
			final String sWordLower = hiddenUnitTokens.toLowerCase();
			final String tWordLower = actualTokens.toLowerCase();
			if (!relatedWordsForWord.containsKey(sWordLower)) {
				throw new RuntimeException("Hidden word:" + sWordLower + ". Not contained in cache.");
			}
			Set<String> relatedWords = relatedWordsForWord.get(sWordLower);
			if (!relatedWords.contains(tWordLower)) {
				relations = ImmutableSet.of(WordNetRelations.NO_RELATION);
			} else {
				relations = revisedRelationsMap.get(sWordLower).get(tWordLower);
			}
			return relations;
		}
	}

	public static interface ILemmatizer {
		String getLowerCaseLemma(String word, String POS);
	}

	public static class CachingWordNetLemmatizer implements ILemmatizer {
		private final WordNetRelations wnr;
		private final THashMap<String, String> lemmaCache;
		private final ReentrantReadWriteLock.WriteLock writeLock;

		public CachingWordNetLemmatizer(WordNetRelations wnr, THashMap<String, String> lemmaCache){
			this.wnr = wnr;
			this.lemmaCache = lemmaCache;
			writeLock = new ReentrantReadWriteLock().writeLock();
		}

		@Override
		public String getLowerCaseLemma(String word, String POS) {
			// TODO(smt): use LoadingCache
			final String pair = word + "_" + POS;
			writeLock.lock();
			try {
				if (!lemmaCache.contains(pair)) {
					final String lemma = wnr.getLemmaForWord(word, POS).toLowerCase();
					lemmaCache.put(pair, lemma);
					return lemma;
				} else {
					return lemmaCache.get(pair);
				}
			} finally {
				writeLock.unlock();
			}
		}
	}

	public static class CachedLemmatizer implements ILemmatizer {
		private final Map<String, String> lemmaCache;

		public CachedLemmatizer(Map<String, String> lemmaCache) {
			this.lemmaCache = lemmaCache;
		}

		public String getLowerCaseLemma(String word, String POS) {
			return lemmaCache.get(word + "_" + POS);
		}
	}
}
