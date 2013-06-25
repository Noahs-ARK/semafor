package edu.cmu.cs.lti.ark.fn.wordnet;

import java.util.Set;

/** Finds token relationships */
public interface Relations {
	public Set<String> getRelations(String actualTokens, String hiddenUnitTokens);
}
