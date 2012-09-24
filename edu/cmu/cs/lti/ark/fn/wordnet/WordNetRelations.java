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

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetAPI.RelationType;

import net.didion.jwnl.data.POS;


import gnu.trove.THashMap;
import gnu.trove.THashSet;

public class WordNetRelations
{
	public static final String NO_RELATION = "no-relation";
	private static Pattern puncPattern = Pattern.compile("\\p{Punct}");
	private static final int NUM_THRESH = 4;
	
	private String sourceWord = null;
	private String targetWord = null;
	
	private WordNetAPI mWN = null;
	
	//contains all the relations for a word
	private Map<String, THashMap<String, Set<String>>> wordNetMap = new THashMap<String, THashMap<String, Set<String>>>(1000);
	
	public Map<String, THashMap<String, Set<String>>> getWordNetMap() {
		return wordNetMap;
	}

	public void setWordNetMap(Map<String, THashMap<String, Set<String>>> wordNetMap) {
		this.wordNetMap = wordNetMap;
	}

	//for one word, contains the list of ALL related words
	private Map<String, Set<String>> relatedWordsForWord = new THashMap<String,Set<String>>();
	
	public Map<String, Set<String>> getRelatedWordsForWord() {
		return relatedWordsForWord;
	}

	public void setRelatedWordsForWord(Map<String, Set<String>> relatedWordsForWord) {
		this.relatedWordsForWord = relatedWordsForWord;
	}

	//mapping a pair of words to a set of relations
	private Map<String, Set<String>> wordPairMap = new THashMap<String, Set<String>>(); 
		
	private Set<String> stopwords = null;
	
	public THashMap<String, Set<String>> workingRelationSet = null;
	
	public Set<String> workingRelatedWords = null;
	
	public Set<String> workingLSRelations = null;
		
	//mapping a word to its lemma 
	public Map<String,String> wordLemmaMap = new THashMap<String, String>();
	
	public WordNetRelations(String stopWordFile, String configFile)
	{
		initializeStopWords(stopWordFile);
		initializeWordNet(configFile);
	}
	
	public WordNetRelations(String serializedFile)
	{
		WordnetCache wc = (WordnetCache)SerializedObjects.readSerializedObject(serializedFile);
		relatedWordsForWord = wc.getRelatedWordsForWordMap();
		wordNetMap = wc.getWordnetMap();
		wordPairMap = wc.getWordPairMap();
		wordLemmaMap = wc.getWordLemmaMap();
	}
	
	public void setCache(String serializedFile)
	{
		WordnetCache wc = (WordnetCache)SerializedObjects.readSerializedObject(serializedFile);
		relatedWordsForWord = wc.getRelatedWordsForWordMap();
		wordNetMap = wc.getWordnetMap();
		wordPairMap = wc.getWordPairMap();
		wordLemmaMap = wc.getWordLemmaMap();
	}
	
	
	public void clearWordNetCache()
	{
		relatedWordsForWord.clear();
		wordNetMap.clear();
		wordPairMap.clear();
		wordLemmaMap.clear();
		mWN.nullInstance();
		sourceWord=null;
		targetWord=null;
	}
	
	public String getLemmaForWord(String word, String pos)
	{
		if(wordLemmaMap.containsKey(word+"_"+pos))
			return wordLemmaMap.get(word+"_"+pos);
		POS wnPOS=null;
		if(pos.startsWith("V"))
		{
			wnPOS = POS.VERB;
		}
		else if(pos.startsWith("J"))
		{
			wnPOS = POS.ADJECTIVE;
		}
		else if(pos.startsWith("R"))
		{
			wnPOS = POS.ADVERB;
		}
		else
			wnPOS = POS.NOUN;
		if(word.equals("'ve"))
			word="have";
		else if(word.equals("n't"))
			word="not";
		else if(word.equals("'s")&&pos.startsWith("V"))
			word="is";
		else if(word.equals("'ll"))
			word="will";
		else if(word.equals("'re"))
			word="are";
		String lemma=getLemma(word, wnPOS);	
		wordLemmaMap.put(word+"_"+pos, lemma);
		return lemma;
	}	
	
	public void writeWordNetCache(String serializedFile)
	{
		WordnetCache wc = new WordnetCache();
		wc.setRelatedWordsForWordMap(relatedWordsForWord);
		wc.setWordnetMap(wordNetMap);
		wc.setWordPairMap(wordPairMap);
		wc.setWordLemmaMap(wordLemmaMap);
		
		SerializedObjects.writeSerializedObject(wc, serializedFile);
	}	
	
	private void initializeStopWords(String stopFile)
	{
		stopwords = new THashSet<String>();
		try {
			BufferedReader bReader = new BufferedReader(new FileReader(stopFile));
			String line = null;
			while((line=bReader.readLine())!=null)
			{
				stopwords.add(line.trim());
			}
		} catch (Exception e) {
			System.err.println("Problem initializing stopword file");
			e.printStackTrace();
		}
	}	
	
	public THashMap<String, Set<String>> getAllRelationsMap(String sWord)
	{
		/*
		 * when sWord = sourceWord
		 */
		if(sWord.equals(sourceWord))
		{
			return workingRelationSet;
		}
		
		sourceWord = sWord;
		
		/*
		 * when sourceWord is contained in the memory 
		 */
		if(relatedWordsForWord.containsKey(sourceWord))
		{
			workingRelationSet = wordNetMap.get(sourceWord);
			workingRelatedWords = relatedWordsForWord.get(sourceWord);
		}
		/*
		 * when sourceWord is not contained in memory
		 */
		else
		{
			updateMapWithNewSourceWord();
		}
		targetWord=null;
		workingLSRelations=null;
		
		return workingRelationSet;
	}
	
	public Set<String> getAllRelatedWords(String sWord)
	{
		/*
		 * when sWord = sourceWord
		 */
		if(sWord.equals(sourceWord))
		{
			return workingRelatedWords;
		}
		
		sourceWord = sWord;
		
		/*
		 * when sourceWord is contained in the memory 
		 */
		if(relatedWordsForWord.containsKey(sourceWord))
		{
			workingRelationSet = wordNetMap.get(sourceWord);
			workingRelatedWords = relatedWordsForWord.get(sourceWord);
		}
		/*
		 * when sourceWord is not contained in memory
		 */
		else
		{
			updateMapWithNewSourceWord();
		}
		targetWord=null;
		workingLSRelations=null;
		
		return workingRelatedWords;
	}
	
	public void updateMapWithNewSourceWord()
	{
		Map<RelationType, Set<String>> rel = null;
		Set<String> relatedWords = null;
		//if punctuation
		if(stopwords.contains(sourceWord.toLowerCase()) || puncPattern.matcher(sourceWord.toLowerCase()).matches())
		{
			rel  = mWN.fillStopWord(sourceWord);
			relatedWords = mWN.getRelatedWord();
		}
		else if(isMoreThanThresh())
		{
			rel  = mWN.fillStopWord(sourceWord);
			relatedWords = mWN.getRelatedWord();
		}
		else
		{
			rel = mWN.getAllRelatedWords(sourceWord);
			relatedWords = mWN.getRelatedWord();
		}
		workingRelationSet = collapseFinerRelations(rel);
		workingRelatedWords = refineRelatedWords(relatedWords);
		wordNetMap.put(sourceWord,workingRelationSet);
		relatedWordsForWord.put(sourceWord,workingRelatedWords);
	}
	
	public boolean isMoreThanThresh()
	{
		String[] arr = sourceWord.trim().split(" ");
		if(arr.length>NUM_THRESH)
			return true;
		else
			return false;
	}
	
	public Set<String> getRelations(String sWord, String tWord)
	{
		
		/*
		 * when sWord = sourceWord and tWord = targetWord
		 */
		if(sWord.equals(sourceWord)&&tWord.equals(targetWord))
		{
			return workingLSRelations;
		}
		
		/*
		 * when the pair is contained in the map
		 * it is assumed that the source word's whole wordnet map is present in the memory
		 */
		String pair = sWord+"-"+tWord;
		Set<String> relations = wordPairMap.get(pair);
		if(relations!=null)
		{
			sourceWord = new String(sWord);
			targetWord = new String(tWord);
			workingRelationSet = wordNetMap.get(sourceWord);
			workingRelatedWords = relatedWordsForWord.get(sourceWord);
			workingLSRelations = relations;
			return relations;
		}
				
		/*
		 * when sWord is the present sourceWord, workingRelatedWords & wordkingRelationSet need not be updated
		 */
		targetWord = new String(tWord);
		if(sWord.equals(sourceWord))
		{
			Set<String> pairRelations = getRelationWN();
			workingLSRelations = pairRelations;
			wordPairMap.put(pair, pairRelations);
			return pairRelations;
		}	
		
		
		sourceWord=new String(sWord);
		/*
		 * when sourceWord is contained in the memory; workingLSRelations, workingRelatedWords & workingRelationSet
		 * have to be updated
		 */
		if(relatedWordsForWord.containsKey(sourceWord))
		{
			workingRelationSet = wordNetMap.get(sourceWord);
			workingRelatedWords = relatedWordsForWord.get(sourceWord);
		}
		/*
		 * when sourceWord is not contained in the memory; workingLSRelations, workingRelatedWords & workingRelationSet
		 * have to be updated
		 */
		else
		{
			updateMapWithNewSourceWord();
		}	
		THashSet<String> set = getRelationWN();
		workingLSRelations = set;
		wordPairMap.put(pair, set);
		
		return set;	
	}
	
	
	public THashSet<String> getAllPossibleRelationSubset(String sWord)
	{
		//putting stuff into the map
		getAllRelatedWords(sWord);
		
		THashSet<String> result =  new THashSet<String>();
		result.add(new String(NO_RELATION));
		
		/*
		 * workingRelatedWords contains all the related words
		 */
		Iterator<String> itr = workingRelatedWords.iterator();		
		while(itr.hasNext())
		{
			Set<String> relations = getRelations(sWord,itr.next());
			String[] array = new String[relations.size()];
			relations.toArray(array);
			Arrays.sort(array);
			String concat = "";
			for(String rel: array)
			{
				concat+=rel+":";
			}
			if(!result.contains(concat))
			{
				result.add(concat);
			}
		}
		
		return result;
	}
	
	public THashMap<Set<String>,Set<String>> getAllPossibleRelationSubset2(String sWord)
	{
		//putting stuff into the map
		getAllRelatedWords(sWord);
		
		THashMap<Set<String>,Set<String>> result =  new THashMap<Set<String>,Set<String>>();
		Set<String> set = new THashSet<String>();
		set.add(new String(NO_RELATION));
		result.put(set,null);
		
		/*
		 * workingRelatedWords contains all the related words
		 */
		Iterator<String> itr = workingRelatedWords.iterator();		
		while(itr.hasNext())
		{
			String itrWord = itr.next();
			Set<String> relations = getRelations(sWord,itrWord);
			if(!result.contains(relations))
			{
				Set<String> wordSet = new THashSet<String>();
				wordSet.add(itrWord);
				result.put(relations,wordSet);
			}
			else
			{
				Set<String> wordSet = result.get(relations);
				wordSet.add(itrWord);
			}
		}		
		return result;
	}
	
	private Set<String> refineRelatedWords(Set<String> relatedWords)
	{
		if(sourceWord==null)
		{
			System.out.println("Problem. Source Word Null. Exiting");
			System.exit(0);
		}
		if(sourceWord.charAt(0)>='0'&&sourceWord.charAt(0)<='9')
			relatedWords.add(sourceWord);
		return relatedWords;
	}
	
	private THashMap<String, Set<String>> collapseFinerRelations(Map<RelationType, Set<String>> rel)
	{
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
		if(sourceWord==null)
		{
			System.out.println("Problem. Source Word Null. Exiting");
			System.exit(0);
		}
		if(sourceWord.charAt(0)>='0'&&sourceWord.charAt(0)<='9')
			sameNumber.add(sourceWord);
		
		
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
	
	
	private THashSet<String> getRelationWN()
	{
		THashSet<String> result = new THashSet<String>();
		if(!workingRelatedWords.contains(targetWord))
		{
			result.add(NO_RELATION);
			return result;
		}
		Set<String> keys  = workingRelationSet.keySet();
		Iterator<String> keyIterator = keys.iterator();
		while(keyIterator.hasNext())
		{
			String key = keyIterator.next();
			Set<String> words = workingRelationSet.get(key);
			if(words.contains(targetWord))
				result.add(key);
		}			
		
		return result;
	}
	
	public static String[] getLexSemRelationList()
	{
		ArrayList<String> list = new ArrayList<String>();
		list.add("identity");
		list.add("synonym");
		list.add("antonym");
		list.add("hypernym");
		list.add("hyponym");
		list.add("derived-form");
		list.add("morph");
		list.add("verb-group");
		list.add("entailment");
		list.add("entailed-by");
		list.add("see-also");
		list.add("causal-relation");
		list.add("same-number");
		list.add(NO_RELATION);	
		
		String[] rels = new String[list.size()];
		return list.toArray(rels);
	}
	
	
	
	private void initializeWordNet(String configFile)
	{
		try {
			mWN = WordNetAPI.getInstance(configFile);
		} catch (Exception e) {
			System.out.println("Could not initialize wordnet. Exiting.");
			e.printStackTrace();
			System.exit(0);
		}	
	}
	
	public String getLemma(String word, POS pos)
	{
		return WordNetAPI.getLemma(word, pos);
	}
	
}
