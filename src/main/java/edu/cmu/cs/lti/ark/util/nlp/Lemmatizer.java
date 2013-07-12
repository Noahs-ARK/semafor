package edu.cmu.cs.lti.ark.util.nlp;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Token;

/**
* @author sthomson@cs.cmu.edu
*/
public abstract class Lemmatizer {
	public abstract String getLemma(String word, String postag);

	public Sentence addLemmas(Sentence sentence) {
		return new Sentence(Lists.transform(sentence.getTokens(), new Function<Token, Token>() {
			@Override public Token apply(Token input) {
				return input.setLemma(getLemma(input.getForm(), input.getPostag()));
			} }));
	}
}
