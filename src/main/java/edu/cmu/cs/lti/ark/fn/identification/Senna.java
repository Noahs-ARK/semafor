package edu.cmu.cs.lti.ark.fn.identification;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;

/**
 * @author sthomson@cs.cmu.edu
 */
public class Senna {
	public static final int SENNA_VECTOR_DIM = 50;
	public static final String DEFAULT_SENNA_WORDS_FILE = "senna/words.lst";
	public static final String DEFAULT_SENNA_VECTORS_FILE = "senna/embeddings.txt";
	private static InputSupplier<InputStream> DEFAULT_WORDS_SUPPLIER = new InputSupplier<InputStream>() {
		@Override public InputStream getInput() throws IOException {
			return getClass().getClassLoader().getResourceAsStream(DEFAULT_SENNA_WORDS_FILE);
		} };
	private static InputSupplier<InputStream> DEFAULT_VECTORS_SUPPLIER = new InputSupplier<InputStream>() {
		@Override public InputStream getInput() throws IOException {
			return getClass().getClassLoader().getResourceAsStream(DEFAULT_SENNA_VECTORS_FILE);
		} };

	private final Map<String, double[]> embeddings;

	public Senna(Map<String, double[]> embeddings) {
		this.embeddings = embeddings;
	}

	public static Senna load() throws IOException {
		return load(CharStreams.newReaderSupplier(DEFAULT_WORDS_SUPPLIER, Charsets.UTF_8),
				CharStreams.newReaderSupplier(DEFAULT_VECTORS_SUPPLIER, Charsets.UTF_8));
	}

	public static Senna load(InputSupplier<InputStreamReader> wordsInput,
							 InputSupplier<InputStreamReader> vectorsInput) throws IOException {
		return new Senna(readFiles(wordsInput, vectorsInput));
	}

	private static Map<String, double[]> readFiles(InputSupplier<InputStreamReader> wordsInput,
												   InputSupplier<InputStreamReader> vectorsInput) throws IOException {
		final Map<String, double[]> embeddings = Maps.newHashMapWithExpectedSize(130000);
		final List<String> words = CharStreams.readLines(wordsInput);
		final List<String> vectorLines = CharStreams.readLines(vectorsInput);
		for (int i : xrange(words.size())) {
			final String[] fields = vectorLines.get(i).split(" ");
			final double[] vector = new double[SENNA_VECTOR_DIM];
			for (int j : xrange(SENNA_VECTOR_DIM)) {
				vector[j] = Float.parseFloat(fields[j]);
			}
			embeddings.put(words.get(i), vector);
		}
		return embeddings;
	}

	public Optional<double[]> getEmbedding(String word) {
		final String lower = word.toLowerCase();
		return Optional.fromNullable(embeddings.get(lower));
	}
}
