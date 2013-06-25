package edu.cmu.cs.lti.ark.util.nlp;

import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import gnu.trove.THashMap;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
* @author sthomson@cs.cmu.edu
*/
public class CachingWordNetLemmatizer implements Lemmatizer {
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
