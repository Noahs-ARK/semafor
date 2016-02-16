/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * WordNetRelations.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.wordnet;

import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetAPI.RelationType;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.nlp.MorphaLemmatizer;
import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class WordNetRelations {
	public static final String DEFAULT_FILE_PROPERTIES_FILE = "file_properties.xml";
	public static final String DEFAULT_STOPWORDS_FILE = "stopwords.txt";
	private static final int LEMMA_CACHE_SIZE = 100000;
	public static final String NO_RELATION = "no-relation";
	private static Pattern PUNCTUATION_PATTERN = Pattern.compile("\\p{Punct}");
	private static final int NUM_THRESHOLD = 4;
	private static final MorphaLemmatizer lemmatizer = new MorphaLemmatizer();

	private static class SingletonHolder {
		public static final WordNetRelations INSTANCE;
		static {
			try {
				INSTANCE = new WordNetRelations();
			} catch (URISyntaxException e) { throw new RuntimeException(e); }
		}
	}

	private String sourceWord = null;
	private String targetWord = null;
	private WordNetAPI mWN = null;
	//contains all the relations for a word
	private Map<String, THashMap<String, Set<String>>> wordNetMap =
			new THashMap<String, THashMap<String, Set<String>>>(1000);
	//for one word, contains the list of ALL related words
	private Map<String, Set<String>> relatedWordsForWord = new THashMap<String,Set<String>>();
	//mapping a pair of words to a set of relations
	private Map<String, Set<String>> wordPairMap = new THashMap<String, Set<String>>();
	private Set<String> stopwords = null;
	public THashMap<String, Set<String>> workingRelationSet = null;
	public Set<String> workingRelatedWords = null;
	public Set<String> workingLSRelations = null;
	private final LoadingCache<Pair<String, String>, String> lemmaCache =
			CacheBuilder.newBuilder()
					.maximumSize(LEMMA_CACHE_SIZE)
					.build(new CacheLoader<Pair<String, String>, String>() {
						@Override public String load(Pair<String, String> lemmaAndPostag) throws Exception {
							final String lemma = lemmaAndPostag.first;
							final String postag = lemmaAndPostag.second;
							return lemmatizer.getLemma(lemma, postag).toLowerCase();
						}
					});

	/**
	 * Initialize a new WordNetRelations with the default file_properties and stopwords files
	 */
	public WordNetRelations() throws URISyntaxException {
		final ClassLoader classLoader = getClass().getClassLoader();
		final InputStream filePropertiesFile = classLoader.getResourceAsStream(DEFAULT_FILE_PROPERTIES_FILE);
		final InputSupplier<InputStreamReader> stopwordsFile =
				Resources.newReaderSupplier(classLoader.getResource(DEFAULT_STOPWORDS_FILE), Charsets.UTF_8);
		try {
			stopwords = ImmutableSet.copyOf(CharStreams.readLines(stopwordsFile));
			mWN = WordNetAPI.getInstance(filePropertiesFile);
		} catch (Exception e) { throw new RuntimeException(e); }
	}

	public WordNetRelations(String stopWordFile, String configFile) {
		try {
			final InputSupplier<InputStreamReader> stopwordSupplier =
					Files.newReaderSupplier(new File(stopWordFile), Charsets.UTF_8);
			stopwords = ImmutableSet.copyOf(CharStreams.readLines(stopwordSupplier));
			mWN = WordNetAPI.getInstance(new FileInputStream(configFile));
		} catch (Exception e) { throw new RuntimeException(e); }
	}

	/** Lazy-loading singleton */
	public static WordNetRelations getInstance() {
		return SingletonHolder.INSTANCE;
	}

	public Map<String, THashMap<String, Set<String>>> getWordNetMap() {
		return wordNetMap;
	}

	public void setWordNetMap(Map<String, THashMap<String, Set<String>>> wordNetMap) {
		this.wordNetMap = wordNetMap;
	}

	public Map<String, Set<String>> getRelatedWordsForWord() {
		return relatedWordsForWord;
	}

	public void setRelatedWordsForWord(Map<String, Set<String>> relatedWordsForWord) {
		this.relatedWordsForWord = relatedWordsForWord;
	}

	public String getLemma(String word, String postag) {
		return lemmaCache.getUnchecked(Pair.of(word.toLowerCase(), postag));
	}

	public THashMap<String, Set<String>> getAllRelationsMap(String sWord) {
		/*
		 * when sWord = sourceWord
		 */
		if(sWord.equals(sourceWord)) {
			return workingRelationSet;
		}
		sourceWord = sWord;
		/*
		 * when sourceWord is contained in the memory 
		 */
		if(relatedWordsForWord.containsKey(sourceWord)) {
			workingRelationSet = wordNetMap.get(sourceWord);
			workingRelatedWords = relatedWordsForWord.get(sourceWord);
		} else {
			/*
			 * when sourceWord is not contained in memory
			 */
			updateMapWithNewSourceWord();
		}
		targetWord=null;
		workingLSRelations=null;
		
		return workingRelationSet;
	}

	public void updateMapWithNewSourceWord() {
		final Map<RelationType, Set<String>> rel;
		final Set<String> relatedWords;
		if(stopwords.contains(sourceWord.toLowerCase())
				|| PUNCTUATION_PATTERN.matcher(sourceWord.toLowerCase()).matches()) {
			rel  = mWN.fillStopWord(sourceWord);
			relatedWords = mWN.getRelatedWord();
		} else if(isMoreThanThresh()) {
			rel  = mWN.fillStopWord(sourceWord);
			relatedWords = mWN.getRelatedWord();
		} else {
			rel = mWN.getAllRelatedWords(sourceWord);
			relatedWords = mWN.getRelatedWord();
		}
		workingRelationSet = collapseFinerRelations(rel);
		workingRelatedWords = refineRelatedWords(relatedWords);
		wordNetMap.put(sourceWord, workingRelationSet);
		relatedWordsForWord.put(sourceWord, workingRelatedWords);
	}
	
	public boolean isMoreThanThresh() {
		return sourceWord.trim().split(" ").length > NUM_THRESHOLD;
	}
	
	public Set<String> getRelations(String sWord, String tWord) {
		/*
		 * when sWord = sourceWord and tWord = targetWord
		 */
		if(sWord.equals(sourceWord)&&tWord.equals(targetWord)) {
			return workingLSRelations;
		}
		/*
		 * when the pair is contained in the map
		 * it is assumed that the source word's whole wordnet map is present in the memory
		 */
		final String pair = sWord+"-"+tWord;
		final Set<String> relations = wordPairMap.get(pair);
		if(relations != null) {
			sourceWord = sWord;
			targetWord = tWord;
			workingRelationSet = wordNetMap.get(sourceWord);
			workingRelatedWords = relatedWordsForWord.get(sourceWord);
			workingLSRelations = relations;
			return relations;
		}
		/*
		 * when sWord is the present sourceWord, workingRelatedWords & wordkingRelationSet need not be updated
		 */
		targetWord = tWord;
		if(sWord.equals(sourceWord)) {
			final Set<String> pairRelations = getRelationWN();
			workingLSRelations = pairRelations;
			wordPairMap.put(pair, pairRelations);
			return pairRelations;
		}
		sourceWord = sWord;
		/*
		 * when sourceWord is contained in the memory; workingLSRelations, workingRelatedWords & workingRelationSet
		 * have to be updated
		 */
		if(relatedWordsForWord.containsKey(sourceWord)) {
			workingRelationSet = wordNetMap.get(sourceWord);
			workingRelatedWords = relatedWordsForWord.get(sourceWord);
		} else {
			/*
			 * when sourceWord is not contained in the memory; workingLSRelations, workingRelatedWords & workingRelationSet
			 * have to be updated
			 */
			updateMapWithNewSourceWord();
		}	
		THashSet<String> set = getRelationWN();
		workingLSRelations = set;
		wordPairMap.put(pair, set);
		
		return set;	
	}

	private Set<String> refineRelatedWords(Set<String> relatedWords) {
		if(sourceWord == null) {
			System.out.println("Problem. Source Word Null. Exiting");
			System.exit(0);
		}
		if(sourceWord.charAt(0) >= '0' && sourceWord.charAt(0) <= '9') {
			relatedWords.add(sourceWord);
		}
		return relatedWords;
	}
	
	private THashMap<String, Set<String>> collapseFinerRelations(Map<RelationType, Set<String>> rel) {
		THashMap<String,Set<String>> result = new THashMap<String,Set<String>>();
		THashSet<String> identity = new THashSet<String>();
		THashSet<String> synonym = new THashSet<String>();
		THashSet<String> antonym = new THashSet<String>();
		THashSet<String> hypernym = new THashSet<String>();
		THashSet<String> hyponym = new THashSet<String>();
		THashSet<String> derivedForm = new THashSet<String>();
		THashSet<String> morphSet = new THashSet<String>();
		THashSet<String> verbGroup = new THashSet<String>();
		THashSet<String> entailment = new THashSet<String>();
		THashSet<String> entailedBy = new THashSet<String>();
		THashSet<String> seeAlso = new THashSet<String>();
		THashSet<String> causalRelation = new THashSet<String>();
		THashSet<String> sameNumber = new THashSet<String>();
		
		identity.addAll(rel.get(RelationType.idty));
		synonym.addAll(rel.get(RelationType.synm)); synonym.addAll(rel.get(RelationType.syn2));
		antonym.addAll(rel.get(RelationType.antm)); antonym.addAll(rel.get(RelationType.extd)); antonym.addAll(rel.get(RelationType.indi));
		hypernym.addAll(rel.get(RelationType.hype));
		hyponym.addAll(rel.get(RelationType.hypo));
		derivedForm.addAll(rel.get(RelationType.derv));
		morphSet.addAll(rel.get(RelationType.morph));
		verbGroup.addAll(rel.get(RelationType.vgrp));
		entailment.addAll(rel.get(RelationType.entl));
		entailedBy.addAll(rel.get(RelationType.entlby));
		seeAlso.addAll(rel.get(RelationType.alsoc));
		causalRelation.addAll(rel.get(RelationType.cause));
		if(sourceWord==null) {
			System.out.println("Problem. Source Word Null. Exiting");
			System.exit(0);
		}
		if(sourceWord.charAt(0)>='0'&&sourceWord.charAt(0)<='9') {
			sameNumber.add(sourceWord);
		}

		result.put("identity",identity);
		result.put("synonym",synonym);
		result.put("antonym",antonym);
		result.put("hypernym",hypernym);
		result.put("hyponym",hyponym);
		result.put("derived-form",derivedForm);
		result.put("morph",morphSet);
		result.put("verb-group",verbGroup);
		result.put("entailment",entailment);
		result.put("entailed-by",entailedBy);
		result.put("see-also",seeAlso);
		result.put("causal-relation",causalRelation);
		result.put("same-number", sameNumber);
		
		return result;
	}

	private THashSet<String> getRelationWN() {
		THashSet<String> result = new THashSet<String>();
		if(!workingRelatedWords.contains(targetWord)) {
			result.add(NO_RELATION);
			return result;
		}
		for (String key : workingRelationSet.keySet()) {
			if (workingRelationSet.get(key).contains(targetWord)) {
				result.add(key);
			}
		}
		return result;
	}
}
