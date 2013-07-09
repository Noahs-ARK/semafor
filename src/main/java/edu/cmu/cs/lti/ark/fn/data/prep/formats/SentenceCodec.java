package edu.cmu.cs.lti.ark.fn.data.prep.formats;

import com.google.common.base.Joiner;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;

/**
 * Reads and writes annotated sentences in a few formats
 * @author sthomson@cs.cmu.edu
 */
public abstract class SentenceCodec {
	final protected String sentenceDelimiterPattern;
	final protected String sentenceDelimiter;
	final protected String tokenDelimiter;

	// Some useful codecs
	public static final SentenceCodec ConllCodec = new SentenceCodec("\n\n", "\n") {
		@Override public Token decodeToken(String tokenStr) { return Token.fromConll(tokenStr); }
		@Override public String encodeToken(Token token) { return token.toConll(); }
	};
	public static final SentenceCodec PosTaggedCodec = new SentenceCodec("\n", " ") {
		@Override public Token decodeToken(String tokenStr) { return Token.fromPosTagged(tokenStr); }
		@Override public String encodeToken(Token token) { return token.toPosTagged(); }
	};
	// TODO: what is this number at the end of malt parses?
	public static final SentenceCodec MaltCodec = new SentenceCodec("\t\\d*\n", "\t1\n", " ") {
		@Override public Token decodeToken(String tokenStr) { return Token.fromMalt(tokenStr); }
		@Override public String encodeToken(Token token) { return token.toMalt(); }
	};
	public static final SentenceCodec TokenizedCodec = new SentenceCodec("\n", " ") {
		@Override public Token decodeToken(String tokenStr) { return new Token(tokenStr); }
		@Override public String encodeToken(Token token) { return token.getForm(); }
	};

	protected SentenceCodec(String sentenceDelimiter, String tokenDelimiter) {
		this(sentenceDelimiter, sentenceDelimiter, tokenDelimiter);
	}

	protected SentenceCodec(String sentenceDelimiterPattern, String sentenceDelimiter, String tokenDelimiter) {
		this.sentenceDelimiterPattern = sentenceDelimiterPattern;
		this.sentenceDelimiter = sentenceDelimiter;
		this.tokenDelimiter = tokenDelimiter;
	}

	public abstract String encodeToken(Token token);

	public abstract Token decodeToken(String tokenStr);

	/**
	 * Encodes a Sentence as a String
	 *
	 * @param sentence a Sentence
	 * @return a the Sentence encoded as a String
	 */
	public String encode(Sentence sentence) {
		ArrayList<String> tokenStrs = Lists.newArrayList();
		for(Token token : sentence.getTokens()) {
			tokenStrs.add(encodeToken(token));
		}
		return Joiner.on(tokenDelimiter).join(tokenStrs) + sentenceDelimiter;
	}

	/**
	 * Converts a sentence encoded as a String into a Sentence
	 * @param sentenceStr a sentence encoded as a String in some format (TBD by subclasser)
	 * @return a new Sentence
	 */
	public Sentence decode(String sentenceStr) {
		ArrayList<Token> tokens = Lists.newArrayList();
		final String[] tokenStrs = sentenceStr.split(tokenDelimiter);
		for(int i : xrange(tokenStrs.length)) {
			try {
				final Token token = decodeToken(tokenStrs[i]);
				//indexed starting at 1!
				tokens.add(token.withIndex(i + 1));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(tokenStrs[i], e);
			}
		}
		return new Sentence(tokens);
	}

	public SentenceIterator readInput(Readable input) {
		return new SentenceIterator(this, input);
	}

	public SentenceWriter getFileWriter(File file) throws IOException {
		return new SentenceWriter(this, file);
	}

	/**
	 * Slurp a File or input stream as an Iterator of Sentences
	 */
	public static class SentenceIterator extends AbstractIterator<Sentence> implements Closeable {
		private final Scanner scanner;
		private final SentenceCodec codec;

		public SentenceIterator(SentenceCodec codec, Readable input)  {
			this.codec = codec;
			this.scanner = new Scanner(input).useDelimiter(codec.sentenceDelimiter);
		}

		public SentenceIterator(SentenceCodec codec, File file) throws FileNotFoundException {
			this(codec, new FileReader(file));
		}

		@Override
		protected Sentence computeNext() {
			if(scanner.hasNext()) return codec.decode(scanner.next());
			return endOfData();
		}

		@Override
		public void close() throws IOException {
			scanner.close();
		}
	}

	/**
	 * Write sentences to a File or Writer
	 */
	public static class SentenceWriter implements Closeable {
		private final SentenceCodec codec;
		private final Writer stream;

		public SentenceWriter(SentenceCodec codec, Writer stream)  {
			this.codec = codec;
			this.stream = stream;
		}

		public SentenceWriter(SentenceCodec codec, File file) throws IOException {
			this(codec, new BufferedWriter(new FileWriter(file)));
		}

		public void write(Sentence sentence) throws IOException {
			stream.write(codec.encode(sentence));
		}

		@Override
		public void close() throws IOException {
			stream.close();
		}
	}
}
