/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * RequiredDataForFrameIdentification.java is part of SEMAFOR 2.0.
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

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import gnu.trove.THashMap;
import gnu.trove.THashSet;


public class RequiredDataForFrameIdentification implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4903392701779783678L;

	private THashSet<String> allRelatedWords=null;
	private Map<String,Set<String>> relatedWordsForWord=null;
	private Map<String,THashMap<String,Set<String>>> wordNetMap=null;
	private THashMap<String,THashSet<String>> frameMap = null;
	private THashMap<String,THashSet<String>> cMap = null;
	private Map<String, Map<String, Set<String>>> revisedRelMap=null;
	private Map<String, String> hvLemmaCache = null;
	
	public RequiredDataForFrameIdentification(THashSet<String> a, 
											  Map<String,Set<String>> r, 
											  Map<String,THashMap<String,Set<String>>> w, 
											  THashMap<String,THashSet<String>> f, 
											  THashMap<String,THashSet<String>> c,
											  Map<String, Map<String, Set<String>>> rr,
											  Map<String, String> lc)
	{
		setAllRelatedWords(a);
		setRelatedWordsForWord(r);
		setWordNetMap(w);
		setFrameMap(f);
		setcMap(c);
		setRevisedRelMap(rr);
		setHvLemmaCache(lc);
	}

	public void setAllRelatedWords(THashSet<String> allRelatedWords) {
		this.allRelatedWords = allRelatedWords;
	}

	public THashSet<String> getAllRelatedWords() {
		return allRelatedWords;
	}

	public void setRelatedWordsForWord(Map<String,Set<String>> relatedWordsForWord) {
		this.relatedWordsForWord = relatedWordsForWord;
	}

	public Map<String,Set<String>> getRelatedWordsForWord() {
		return relatedWordsForWord;
	}

	public void setWordNetMap(Map<String,THashMap<String,Set<String>>> wordNetMap) {
		this.wordNetMap = wordNetMap;
	}

	public Map<String,THashMap<String,Set<String>>> getWordNetMap() {
		return wordNetMap;
	}

	public void setFrameMap(THashMap<String,THashSet<String>> frameMap) {
		this.frameMap = frameMap;
	}

	public THashMap<String,THashSet<String>> getFrameMap() {
		return frameMap;
	}

	public void setcMap(THashMap<String,THashSet<String>> cMap) {
		this.cMap = cMap;
	}

	public THashMap<String,THashSet<String>> getcMap() {
		return cMap;
	}

	public void setRevisedRelMap(Map<String, Map<String, Set<String>>> revisedRelMap) {
		this.revisedRelMap = revisedRelMap;
	}

	public Map<String, Map<String, Set<String>>> getRevisedRelMap() {
		return revisedRelMap;
	}

	public void setHvLemmaCache(Map<String, String> hvLemmaCache) {
		this.hvLemmaCache = hvLemmaCache;
	}

	public Map<String, String> getHvLemmaCache() {
		return hvLemmaCache;
	}
	
}
