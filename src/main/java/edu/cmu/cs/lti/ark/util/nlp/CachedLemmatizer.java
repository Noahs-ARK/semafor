package edu.cmu.cs.lti.ark.util.nlp;

import java.util.Map;

/**
* @author sthomson@cs.cmu.edu
*/
public class CachedLemmatizer implements Lemmatizer {
	private final Map<String, String> lemmaCache;

	public CachedLemmatizer(Map<String, String> lemmaCache) {
		this.lemmaCache = lemmaCache;
	}

	public String getLowerCaseLemma(String word, String POS) {
		return lemmaCache.get(word + "_" + POS);
	}
}
