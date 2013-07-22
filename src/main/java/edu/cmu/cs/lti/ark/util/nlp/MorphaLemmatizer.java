package edu.cmu.cs.lti.ark.util.nlp;

import edu.washington.cs.knowitall.morpha.MorphaStemmer;

/**
 * Adapts MorphaStemmer to our Lemmatizer interface
 *
 * @author sthomson@cs.cmu.edu
 */
public class MorphaLemmatizer extends Lemmatizer {
	@Override
	public String getLemma(String word, String postag) {
		return MorphaStemmer.stemToken(word.toLowerCase(), postag.toUpperCase()).toLowerCase();
	}
}
