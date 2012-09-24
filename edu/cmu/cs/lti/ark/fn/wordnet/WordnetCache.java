/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * WordnetCache.java is part of SEMAFOR 2.0.
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

import java.io.Serializable;
import java.util.Map;
import java.util.Set;



public class WordnetCache implements Serializable
{
	private static final long serialVersionUID = -7236889982431545458L;

	//contains all the relations for a word
	private Map<String, THashMap<String, Set<String>>> wordNetMap = new THashMap<String, THashMap<String, Set<String>>>(1000);
	
	//for one word, contains the list of ALL related words
	private Map<String, Set<String>> relatedWordsForWord = new THashMap<String,Set<String>>();
	
	//mapping a pair of words to a set of relations
	private Map<String, Set<String>> wordPairMap = new THashMap<String, Set<String>>(); 
	
	//mapping a word to its lemma
	private Map<String, String> wordLemmaMap = new THashMap<String, String>();
	
	public void setWordnetMap(Map<String, THashMap<String, Set<String>>> map)
	{
		wordNetMap = map;
	}
	
	public void setRelatedWordsForWordMap(Map<String, Set<String>> map)
	{
		relatedWordsForWord = map;
	}
	
	public void setWordPairMap(Map<String, Set<String>> map)
	{
		wordPairMap = map;
	}	
	
	public Map<String, THashMap<String, Set<String>>> getWordnetMap()
	{
		return wordNetMap;
	}
	
	public Map<String, Set<String>> getRelatedWordsForWordMap()
	{
		return relatedWordsForWord;
	}
	
	public Map<String, Set<String>> getWordPairMap()
	{
		return wordPairMap;
	}
	
	public Map<String,String> getWordLemmaMap()
	{
		return wordLemmaMap;
	}
	
	public void setWordLemmaMap(Map<String,String> map)
	{
		wordLemmaMap=map;
	}
	
}
