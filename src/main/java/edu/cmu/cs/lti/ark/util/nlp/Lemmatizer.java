package edu.cmu.cs.lti.ark.util.nlp;

/**
* @author sthomson@cs.cmu.edu
*/
public interface Lemmatizer {
	String getLowerCaseLemma(String word, String POS);
}
