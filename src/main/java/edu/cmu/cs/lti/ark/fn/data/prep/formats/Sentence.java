package edu.cmu.cs.lti.ark.fn.data.prep.formats;

import com.google.common.collect.ImmutableList;

import javax.annotation.concurrent.Immutable;
import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;

/**
 * Represents one sentence in conll format
 * Conll sentences are one token per line. Sentences are separated by blank lines.
 *
 * @author sthomson@cs.cmu.edu
 */
@Immutable
public class Sentence {
	private final ImmutableList<Token> tokens;

	public Sentence(Iterable<Token> tokens) {
		this.tokens = copyOf(tokens);
	}

	public List<Token> getTokens() {
		return tokens;
	}
}
