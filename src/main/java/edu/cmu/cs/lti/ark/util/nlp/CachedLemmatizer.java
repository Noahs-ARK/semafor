package edu.cmu.cs.lti.ark.util.nlp;

import com.google.common.base.Optional;

import java.util.Map;

/**
* @author sthomson@cs.cmu.edu
*/
public class CachedLemmatizer extends Lemmatizer {
	private final Map<String, String> lemmaCache;

	public CachedLemmatizer(Map<String, String> lemmaCache) {
		this.lemmaCache = lemmaCache;
	}

	public String getLemma(String word, String postag) {
		final Optional<String> oLemma = Optional.fromNullable(lemmaCache.get(word + "_" + postag));
		return oLemma.isPresent() ? oLemma.get() : word;
	}
}
