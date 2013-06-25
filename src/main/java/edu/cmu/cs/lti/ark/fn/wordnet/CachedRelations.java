package edu.cmu.cs.lti.ark.fn.wordnet;

import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Set;

/** Finds relationships without the WordNetRelations object */
public class CachedRelations implements Relations {
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
			throw new RuntimeException("Hidden word \"" + sWordLower + "\" not contained in cache.");
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
