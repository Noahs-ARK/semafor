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

import com.google.common.collect.ImmutableSet;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.IFeatureExtractor;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Extracts features for the frame identification model
 */
public class FeatureExtractor implements IFeatureExtractor {
	private static String getLowerCaseLemma(int index, String[][] data) {
		return data[5][index];
	}

	public IntCounter<String> extractFeatures(String mFrameName,
											  int[] tokenNums,
											  String hiddenWord,
											  String[][] parseData,
											  WordNetRelations wnr,
											  THashMap<String, THashSet<String>> wnCacheMap,
											  THashMap<String, String> lemmaCache,
											  DependencyParse parse) {
		final IRelations wnRelations = new WNRelations(wnr, wnCacheMap);
		final ILemmatizer lemmatizer = new CachingWordNetLemmatizer(wnr, lemmaCache);
		boolean parseHasLemmas = false;
		return extractFeatures(mFrameName, tokenNums, hiddenWord, parseData, parse, wnRelations, lemmatizer, parseHasLemmas);
	}

	public IntCounter<String> extractFeaturesLessMemory(String mFrameName,
														int[] tokenNums,
														String hiddenWord,
														String[][] parseData,
														Map<String, Set<String>> relatedWordsForWord,
														Map<String, Map<String, Set<String>>> revisedRelationsMap,
														Map<String, String> mHVLemmas,
														DependencyParse parse) {
		final IRelations wnRelations = new CachedRelations(revisedRelationsMap, relatedWordsForWord);
		final ILemmatizer lemmatizer = new CachedLemmatizer(mHVLemmas);
		boolean parseHasLemmas = true;
		return extractFeatures(mFrameName, tokenNums, hiddenWord, parseData, parse, wnRelations, lemmatizer, parseHasLemmas);
	}

	private IntCounter<String> extractFeatures(String mFrameName,
											   int[] tokenNums,
											   String hiddenWord,
											   String[][] parseData,
											   DependencyParse parse,
											   IRelations wnRelations,
											   ILemmatizer lemmatizer,
											   boolean parseHasLemmas) {
		Arrays.sort(tokenNums);
		IntCounter<String> featureMap = new IntCounter<String>();

		String hiddenUnitTokens = "";
		String hiddenUnitLemmas = "";
		String hiddenLemmaAndFPOS = "";

		String actualTokens = "";
		String actualLemmas = "";
		String actualLemmaAndFPOS = "";

		String hiddenPOSSeq = "";
		String hiddenFinePOSSeq = "";

		String actualPOSSeq = "";
		String actualFinePOSSeq = "";

		String[] hiddenTokens = hiddenWord.split(" ");
		for (String hiddenTok : hiddenTokens) {
			String[] arr = hiddenTok.split("_");
			hiddenUnitTokens += arr[0] + " ";
			hiddenPOSSeq += arr[1] + " ";
			hiddenFinePOSSeq += arr[1].substring(0, 1) + " ";
			hiddenUnitLemmas += lemmatizer.getLowerCaseLemma(arr[0], arr[1]) + " ";
			hiddenLemmaAndFPOS += lemmatizer.getLowerCaseLemma(arr[0], arr[1]) + "_" + arr[1].substring(0, 1) + " ";
		}
		hiddenUnitTokens = hiddenUnitTokens.trim();
		hiddenUnitLemmas = hiddenUnitLemmas.trim();
		hiddenPOSSeq = hiddenPOSSeq.trim();
		hiddenFinePOSSeq = hiddenFinePOSSeq.trim();
		hiddenLemmaAndFPOS = hiddenLemmaAndFPOS.trim();

		for (int mTokenNum : tokenNums) {
			String lexUnit = parseData[0][mTokenNum];
			String pos = parseData[1][mTokenNum];
			actualTokens += lexUnit + " ";
			final String actualLemma = parseHasLemmas ? getLowerCaseLemma(mTokenNum, parseData)
											: lemmatizer.getLowerCaseLemma(lexUnit, pos);
			actualLemmas += actualLemma + " ";
			actualPOSSeq += pos + " ";
			actualFinePOSSeq += pos.substring(0, 1) + " ";
			actualLemmaAndFPOS += actualLemma + "_" + pos.substring(0, 1) + " ";
		}
		actualTokens = actualTokens.trim();
		actualLemmas = actualLemmas.trim();
		actualPOSSeq = actualPOSSeq.trim();
		actualFinePOSSeq = actualFinePOSSeq.trim();
		actualLemmaAndFPOS = actualLemmaAndFPOS.trim();
		Set<String> relations = wnRelations.getRelations(actualTokens, hiddenUnitTokens);

		for (String relation : relations) {
			String feature = "tRLn:" + relation +
					"_f:" + mFrameName;
			featureMap.increment(feature);
			feature = "tRLn:" + relation +
					"_hU:" + hiddenUnitTokens.replaceAll(" ", "_") +
					"_f:" + mFrameName;
			featureMap.increment(feature);
			if (relation.equals(WordNetRelations.NO_RELATION))
				continue;
			feature = "tRLn:" + relation +
					"_hU:" + hiddenUnitTokens.replaceAll(" ", "_") +
					"_hP:" + hiddenFinePOSSeq.replaceAll(" ", "_") +
					"_aP:" + actualFinePOSSeq.replaceAll(" ", "_") +
					"_f:" + mFrameName;
			featureMap.increment(feature);
		}

		/*
		 * features
		 */
		String feature = "hTs:" + hiddenUnitTokens.replaceAll(" ", "_") +
				"_f:" + mFrameName;
		featureMap.increment(feature);
		feature = "hLs:" + hiddenUnitLemmas.replaceAll(" ", "_") +
				"_f:" + mFrameName;
		featureMap.increment(feature);
		feature = "hLFPOSs:" + hiddenLemmaAndFPOS.replaceAll(" ", "_") +
				"_f:" + mFrameName;
		featureMap.increment(feature);
		if (hiddenUnitTokens.equals(actualTokens)) {
			feature = "sTs_f:" + mFrameName;
			featureMap.increment(feature);
			feature = "sTs_hLs:" + hiddenUnitTokens.replaceAll(" ", "_") +
					"_f:" + mFrameName;
			featureMap.increment(feature);
			feature = "sTs_pSeqs_A:" + actualFinePOSSeq.replaceAll(" ", "_") +
					"_H:" + hiddenFinePOSSeq.replaceAll(" ", "_") +
					"_f:" + mFrameName;
			featureMap.increment(feature);
			feature = "sTs_pLSeqs_A:" + actualLemmaAndFPOS.replaceAll(" ", "_") +
					"_H:" + hiddenLemmaAndFPOS.replaceAll(" ", "_") +
					"_f:" + mFrameName;
			featureMap.increment(feature);
		}
		if (hiddenUnitLemmas.equals(actualLemmas)) {
			feature = "sLs_f:" + mFrameName;
			featureMap.increment(feature);
			feature = "sLs_hLs:" + hiddenUnitTokens.replaceAll(" ", "_") +
					"_f:" + mFrameName;
			featureMap.increment(feature);
			feature = "sLs_pSeqs_A:" + actualFinePOSSeq.replaceAll(" ", "_") +
					"_H:" + hiddenFinePOSSeq.replaceAll(" ", "_") +
					"_f:" + mFrameName;
			featureMap.increment(feature);
			feature = "sLs_pLSeqs_A:" + actualLemmaAndFPOS.replaceAll(" ", "_") +
					"_H:" + hiddenLemmaAndFPOS.replaceAll(" ", "_") +
					"_f:" + mFrameName;
			featureMap.increment(feature);
		}

		/*
		 * syntactic features
		 */
		DependencyParse[] sortedNodes = parse.getIndexSortedListOfNodes();

		DependencyParse node = DependencyParse.getHeuristicHead(sortedNodes, tokenNums);

		String nodePOS = node.getPOS().substring(0, 1);
		List<DependencyParse> children = node.getChildren();
		String subcat = "";
		String dependencies = "";
		THashSet<String> deps = new THashSet<String>();
		for (DependencyParse child : children) {
			String lab = child.getLabelType();
			deps.add(lab);
			if (nodePOS.equals("V")) {
				if (!lab.equals("SUB") && !lab.equals("P") && !lab.equals("CC")) {
					subcat += lab + "_";
				}
			}
		}
		for (String dep : deps) {
			dependencies += dep + "_";
		}
		feature = "d:" + dependencies + "f:" + mFrameName;
		featureMap.increment(feature);
		if (nodePOS.equals("V")) {
			feature = "sC:" + subcat + "f:" + mFrameName;
			featureMap.increment(feature);
		}
		DependencyParse dp = node.getParent();
		String parPOS;
		String parLab;
		if (dp == null) {
			parPOS = "NULL";
			parLab = "NULL";
		} else {
			parPOS = dp.getPOS();
			parLab = dp.getLabelType();
		}
		feature = "pP:" + parPOS + "_f:" + mFrameName;
		featureMap.increment(feature);
		feature = "pL:" + parLab + "_f:" + mFrameName;
		featureMap.increment(feature);

		return featureMap;
	}

	public static interface IRelations {
		public Set<String> getRelations(String actualTokens, String hiddenUnitTokens);
	}

	/** Finds token relationships */
	private static class WNRelations implements IRelations {
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
	private static class CachedRelations implements IRelations {
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
