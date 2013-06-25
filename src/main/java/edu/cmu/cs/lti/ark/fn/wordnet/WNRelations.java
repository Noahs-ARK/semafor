package edu.cmu.cs.lti.ark.fn.wordnet;

import com.google.common.collect.ImmutableSet;
import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Finds token relationships using the WordNetRelations object */
public class WNRelations implements Relations {
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
