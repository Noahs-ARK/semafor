package edu.cmu.cs.lti.ark.util.nlp.conll;

import com.google.common.base.Joiner;
import com.google.common.collect.*;

import javax.annotation.concurrent.Immutable;
import java.io.*;
import java.util.*;

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
		this.tokens = ImmutableList.copyOf(tokens);
	}

	public List<Token> getTokens() {
		return tokens;
	}

	/**
	 * Converts a sentence into a String in conll format
	 * @return this sentence in conll format
	 */
	@Override
	public String toString() {
		return Joiner.on("\n").join(tokens) + "\n\n";
	}

	/**
	 * Converts a sequence of lines into a Sentence
	 * @param lines a sequence of lines in conll format
	 * @return a new Sentence
	 */
	public static Sentence fromLines(Iterable<String> lines) {
		ArrayList<Token> tokens = Lists.newArrayList();
		for(String line : lines) {
			if(!line.equals("")) tokens.add(Token.fromString(line));
		}
		return new Sentence(tokens);
	}

	public static Sentence fromString(String lines) {
		return fromLines(Arrays.asList(lines.split("\n")));
	}

	public static class SentenceIterator extends AbstractIterator<Sentence> implements Closeable {
		private final Scanner scanner;

		public SentenceIterator(InputStreamReader input)  {
			this.scanner = new Scanner(input).useDelimiter("\n\n");
		}

		public SentenceIterator(File file) throws FileNotFoundException {
			this(new FileReader(file));
		}

		@Override
		protected Sentence computeNext() {
			if(scanner.hasNext()) return Sentence.fromString(scanner.next());
			return endOfData();
		}

		@Override
		public void close() throws IOException {
			scanner.close();
		}
	}
}
