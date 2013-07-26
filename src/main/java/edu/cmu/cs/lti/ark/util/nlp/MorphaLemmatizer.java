package edu.cmu.cs.lti.ark.util.nlp;

import com.google.common.base.Preconditions;
import edu.washington.cs.knowitall.morpha.MorphaStemmer;

/**
 * Adapts MorphaStemmer to our Lemmatizer interface
 *
 * @author sthomson@cs.cmu.edu
 */
public class MorphaLemmatizer extends Lemmatizer {
	@Override
	public String getLemma(String word, String postag) {
		Preconditions.checkNotNull(word);
		Preconditions.checkNotNull(postag);
		Preconditions.checkArgument(!word.isEmpty());
		final String lemma = MorphaStemmer.stemToken(word.toLowerCase(), postag.toUpperCase()).toLowerCase();
		return lemma.isEmpty() ? word : lemma;
	}
}
