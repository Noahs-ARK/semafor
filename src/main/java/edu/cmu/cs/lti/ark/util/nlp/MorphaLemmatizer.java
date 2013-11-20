package edu.cmu.cs.lti.ark.util.nlp;

import com.google.common.base.Preconditions;
import uk.ac.susx.informatics.Morpha;

import java.io.IOException;
import java.io.StringReader;

/**
 * Adapts Morpha to Lemmatizer interface
 *
 * @author sthomson@cs.cmu.edu
 */
public class MorphaLemmatizer extends Lemmatizer {
	@Override
	public String getLemma(String word, String postag) {
		Preconditions.checkNotNull(word);
		Preconditions.checkNotNull(postag);
		if (word.isEmpty()) return "";
		final String token = word.toLowerCase();
		final String tokenAndPostag = String.format("%s_%s", token.replaceAll("_", "-"), postag.toUpperCase());
		try {
			return new Morpha(new StringReader(tokenAndPostag), true).next();
		} catch (IOException e) {
			return token;
		} catch (Error e) {
			if (e.getMessage().equals("Error: could not match input")) {
				return token;
			} else {
				throw e;
			}
		}
	}
}
