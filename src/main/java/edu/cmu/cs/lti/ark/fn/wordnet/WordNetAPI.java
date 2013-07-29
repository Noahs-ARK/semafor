/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * WordNetAPI.java is part of SEMAFOR 2.0.
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


import gnu.trove.THashMap;
import gnu.trove.THashSet;
import net.didion.jwnl.JWNL;
import net.didion.jwnl.data.*;
import net.didion.jwnl.data.list.PointerTargetNode;
import net.didion.jwnl.data.list.PointerTargetNodeList;
import net.didion.jwnl.dictionary.Dictionary;

import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author Mengqiu Wang
 *
 */
public class WordNetAPI {
	private static WordNetAPI instance = null;
	
	private static Dictionary wDict;
	private PointerUtils pUtils;
	private static Set<String> identity;
	private static Set<String> synonyms;
	private static Set<String> hypernyms;
	private static Set<String> hyponyms;
	
	private static Set<String> derived;
	private static Set<String> verbGroup;
	private static Set<String> causes;
	private static Set<String> entailments;
	private static Set<String> entailedBys;
	private static Set<String> antonyms;
	private static Set<String> synonyms2;
	private static Set<String> alsosees;
	private static Set<String> extendedAntonyms;
	private static Set<String> indirectAntonyms;
	private static Set<String> morph;

	private static Set<IndexWord> allIndexWords = new THashSet<IndexWord>();
	private static Set<Synset> allSenses = new THashSet<Synset>();

	private static Set<String> multiword;

	private static Set<String[]> relatedWordWithPOS;
	private static Set<String> relatedWordWithPOSCheck = new THashSet<String>();
	private static Set<String> relatedWord;

	private static String[] verbPOS = new String[]{"VB","VBD", "VBG", "VBN","VBP","VBZ"  };
	private static String[] nounPOS = new String[]{"NN","NNS","NNP","NNPS"};
	private static String[] adjPOS = new String[]{"JJ","JJR","JJS"};
	private static String[] advPOS = new String[]{"RB","RBR","RBS"};
	public static Map<POS,String[]> posMap = new THashMap<POS,String[]>();

	static{
		posMap.put(POS.NOUN, nounPOS);
		posMap.put(POS.VERB, verbPOS);
		posMap.put(POS.ADVERB, advPOS);
		posMap.put(POS.ADJECTIVE, adjPOS);
	}
	

	public static enum RelationType
	{
		idty, //in paper 
		hype, //in paper
		hypo, //in paper
		synm, //in paper
		derv, //in paper
		vgrp, //in paper 
		cause, //in paper
		entl, //in paper
		entlby, //in paper
		antm, //in paper
		syn2, 
		alsoc, //in paper 
		extd, 
		indi, 
		morph //in paper
	}

	public static WordNetAPI getInstance(InputStream configFile) throws Exception  {
		if (instance == null)
			instance = new WordNetAPI(configFile);
		return instance;
	}

	private static void info(String info){
		System.out.println(info);
	}


	private WordNetAPI(InputStream propsFile) throws Exception {

		info("Initialize WordNet...: ");
		
		if (propsFile == null)
			throw new RuntimeException("Missing required property 'WN_PROP'");

		try {
			JWNL.initialize(propsFile);
			wDict = Dictionary.getInstance();
			pUtils = PointerUtils.getInstance();
		} catch (Exception e) {
			throw new RuntimeException("Initialization failed", e);
		}
		info("Done initializing WordNet...");
	}


	public void getAllIndexWords(String word){
		allIndexWords.clear();
		IndexWord[] iWordArr;
		IndexWord iWord;
		try {
			IndexWordSet iWordSet = wDict.lookupAllIndexWords(word);
			if(iWordSet != null){
				iWordArr = iWordSet.getIndexWordArray();
				for (IndexWord anIWordArr : iWordArr) {
					iWord = anIWordArr;
					allIndexWords.add(iWord);
				}
			}  
		} catch (Exception e) {
			//System.out.println("getSynonym ERROR: " + word);
		}
	}

	public void getAllSenses(String word){
		getAllIndexWords(word);
		allSenses.clear();
		Iterator<IndexWord> itr = allIndexWords.iterator();
		IndexWord iWord;
		while(itr.hasNext()){
			iWord = itr.next();
			try {
				Synset[] senses = iWord.getSenses();
				Collections.addAll(allSenses, senses);
			} catch (Exception e) {
				//System.out.println("getSynonym ERROR: " + word);
			}
		}
	}

	private Map<RelationType, Set<String>> createContainer(){
		Map<RelationType, Set<String>> ret = new THashMap<RelationType, Set<String>>();
		multiword =  new THashSet<String>();
		relatedWord=  new THashSet<String>();
		relatedWordWithPOS=  new THashSet<String[]>();
		relatedWordWithPOSCheck.clear();

		identity =  new THashSet<String>();
		synonyms   =  new THashSet<String>();
		hypernyms  = new THashSet<String>();
		hyponyms   =  new THashSet<String>();
		derived    =  new THashSet<String>();
		verbGroup  =  new THashSet<String>();
		causes      =  new THashSet<String>();
		entailments =  new THashSet<String>();
		entailedBys =  new THashSet<String>();
		antonyms =  new THashSet<String>();
		synonyms2   =  new THashSet<String>();
		alsosees =  new THashSet<String>();
		extendedAntonyms =  new THashSet<String>();
		indirectAntonyms =  new THashSet<String>();
		morph =  new THashSet<String>();

		ret.put(RelationType.idty, identity);
		ret.put(RelationType.synm, synonyms);
		ret.put(RelationType.hype, hypernyms);
		ret.put(RelationType.hypo, hyponyms);
		ret.put(RelationType.derv , derived);
		ret.put(RelationType.vgrp, verbGroup);
		ret.put(RelationType.cause, causes);
		ret.put(RelationType.entl, entailments);
		ret.put(RelationType.entlby, entailedBys);
		ret.put(RelationType.antm, antonyms);
		ret.put(RelationType.syn2, synonyms2);
		ret.put(RelationType.alsoc, alsosees);
		ret.put(RelationType.extd, extendedAntonyms);
		ret.put(RelationType.indi, indirectAntonyms);
		ret.put(RelationType.morph, morph);
		return ret;
	}

	public Map<RelationType, Set<String>> fillStopWord(String word) { 
		Map<RelationType, Set<String>> ret = createContainer();
		identity.add(word);
		relatedWord.add(word);
		return ret;
	}

	private void checkWordWithPOS(String w, POS p){
		String[] posSet = posMap.get(p);
		String pos;
		for (String aPosSet : posSet) {
			pos = aPosSet;
			String tempStr = w + pos;
			if (!relatedWordWithPOSCheck.contains(tempStr)) {
				relatedWordWithPOSCheck.add(tempStr);
				relatedWordWithPOS.add(new String[]{w, pos});
			}
		}
		relatedWord.add(w);
	}

	public void addMorph(String w, POS pos){
		morph.add(w);
		checkWordWithPOS(w, pos);
	}

	public Map<RelationType, Set<String>> getAllRelatedWords(String word) {
		Map<RelationType, Set<String>> ret = createContainer();
		identity.add(word);
		relatedWord.add(word);
		
		getAllSenses(word);
		if(allSenses.size() == 0){
			relatedWordWithPOS.add(new String[]{word});
			return ret; 
		}

		Iterator<Synset> senseItr = allSenses.iterator();
		Synset sense;
		Word[] words;
		Word w;
		POS pos;
		String lemma;

		while(senseItr.hasNext()){
			sense = senseItr.next();
			words = sense.getWords();
			/** Synonyms */
			String syn;
			for (Word word1 : words) {
				w = word1;
				syn = w.getLemma();
				pos = w.getPOS();
				synonyms.add(syn);
				checkWordWithPOS(syn, pos);
			} 

			/** Morphological variation */
			POS sensePOS = sense.getPOS();
			if(sensePOS == POS.NOUN){
				addMorph(word+"s", sensePOS);
				addMorph(word+"es", sensePOS);
				addMorph(word.substring(0, word.length()-1)+"ies", sensePOS);
			}else if(sensePOS ==  POS.VERB){
				if(word.endsWith("e")){
					addMorph(word+"r", sensePOS);
					addMorph(word.substring(0, word.length()-1)+"ing", sensePOS);
					addMorph(word+"d", sensePOS);
				}  
				else{  
					addMorph(word+"er", sensePOS);
					addMorph(word+"ing", sensePOS);
					addMorph(word+"ed", sensePOS);
				}  
			}

			/** Hypernyms, etc */
			for(RelationType relation: RelationType.values()){
				PointerTargetNodeList nodeList = null;
				Set<String> listToStore;
				try{
					switch(relation){
					case hype:
						nodeList = pUtils.getDirectHypernyms(sense);
						break;
					case hypo:
						nodeList = pUtils.getDirectHyponyms(sense);
						break;
					case derv:
						nodeList = pUtils.getDerived(sense);
						break;
					case vgrp:
						nodeList = pUtils.getVerbGroup(sense);
						break;
					case cause:
						nodeList = pUtils.getCauses(sense);
						break;
					case entl:
						nodeList = pUtils.getEntailments(sense);
						break;
					case entlby:
						nodeList = pUtils.getEntailedBy(sense);
						break;
					case antm:
						nodeList = pUtils.getAntonyms(sense);
						break;
					case syn2:
						nodeList = pUtils.getSynonyms(sense);
						break;
					case alsoc:
						nodeList = pUtils.getAlsoSees(sense);
						break;
					case extd:
						//pUtils.getExtendedAntonyms(sense).print();
						nodeList = (PointerTargetNodeList)pUtils.getExtendedAntonyms(sense).toList();
						break;
					case indi:
						//pUtils.getIndirectAntonyms(sense).print();
						nodeList = (PointerTargetNodeList)pUtils.getIndirectAntonyms(sense).toList();
						break;
					}
				} catch (Exception ignored) { }
				if(nodeList != null){
					listToStore = ret.get(relation);
					Iterator targetItr = nodeList.iterator();
					PointerTargetNode pTargetNode;
					while(targetItr.hasNext()){
						pTargetNode = (PointerTargetNode)targetItr.next();
						if(!pTargetNode.isLexical()){
							words = pTargetNode.getSynset().getWords();
							for (Word word1 : words) {
								w = word1;
								lemma = w.getLemma();
								pos = w.getPOS();
								if (lemma.contains("_")) {
									String[] parts = lemma.split("_");
									if (parts.length == 2)
										multiword.add(lemma.toLowerCase());
								} else {
									listToStore.add(lemma);
									checkWordWithPOS(lemma, pos);
								}
							} 
						}else{
							w = pTargetNode.getWord();
							lemma = w.getLemma();
							pos = w.getPOS();
							if(lemma.contains("_")){
								String[] parts = lemma.split("_");
								if(parts.length == 2)
									multiword.add(lemma.toLowerCase());
							}else{
								listToStore.add(lemma);
								checkWordWithPOS(lemma, pos);
							}  
						}
					}
				}
			}
		}
		return ret;
	}

	public Set<String> getRelatedWord(){
		return relatedWord;
	}

	public static void clear(){
		multiword=null;
		relatedWord=null;
		relatedWordWithPOS=null;
		relatedWordWithPOSCheck=null;

		identity=null;
		synonyms=null;
		hypernyms=null;
		hyponyms=null;
		derived=null;
		verbGroup=null;
		causes=null;
		entailments=null;
		entailedBys=null;
		antonyms=null;
		synonyms2=null;
		alsosees=null;
		extendedAntonyms=null;
		indirectAntonyms=null;
		morph=null;
	}
}
