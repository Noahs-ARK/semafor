package edu.cmu.cs.lti.ark.fn.data.prep.formats;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import java.io.*;
import java.util.Map;

import static com.google.common.base.Charsets.UTF_8;
import static edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec.*;
import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Script to convert between various formats
 *
 * @author sthomson@cs.cmu.edu
 */
public class ConvertFormat {
	// Command line option converters
	private static Map<String, SentenceCodec> codecMap = ImmutableMap.of(
			"conll", ConllCodec,
			"pos", PosTaggedCodec,
			"malt", MaltCodec,
			"tokenized", TokenizedCodec);
	public static class CodecConverter implements IStringConverter<SentenceCodec> {
		@Override public SentenceCodec convert(String value) {
			return codecMap.get(value.trim().toLowerCase());
		}
	}
	public static class InputConverter implements IStringConverter<Reader> {
		@Override public Reader convert(String s) {
			try {
				return Files.newReader(new File(s), UTF_8);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
	}
	public static class OutputConverter implements IStringConverter<Writer> {
		@Override public Writer convert(String s) {
			try {
				return Files.newWriter(new File(s), UTF_8);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
	}

	/**
	 * Command Line Options
	 * input and output formats are required.
	 * input defaults to stdin
	 * output defaults to stdout
	 */
	private static class ConvertOptions {
		@Parameter(names = {"-i", "--input"}, converter = InputConverter.class)
		public Reader input = new BufferedReader(new InputStreamReader(System.in));

		@Parameter(names = {"-if", "--inputFormat"}, converter = CodecConverter.class, required = true)
		public SentenceCodec inputCodec;

		@Parameter(names = {"-o", "--output"}, converter = OutputConverter.class)
		public Writer output = new PrintWriter(System.out, true);

		@Parameter(names = {"-of", "--outputFormat"}, converter = CodecConverter.class, required = true)
		public SentenceCodec outputCodec;
	}

	/**
	 * Converts between any two supported formats (conll, pos, malt, tokenized)
	 * @param args command line args {@see ConvertOptions}
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		final ConvertOptions options = new ConvertOptions();
		new JCommander(options, args);
		convertStream(options.input, options.inputCodec, options.output, options.outputCodec);
	}

	/**
	 * Converts between any two supported formats (conll, pos, malt, tokenized)
	 *
	 * @param input Reader to read from
	 * @param inputCodec Codec to parse input with
	 * @param output Writer to write to
	 * @param outputCodec Codec to format output with
	 * @throws IOException
	 */
	private static void convertStream(Reader input, SentenceCodec inputCodec,
									  Writer output, SentenceCodec outputCodec) throws IOException {
		final SentenceIterator sentenceIterator = inputCodec.readInput(input);
		try {
			final SentenceWriter writer = new SentenceWriter(outputCodec, output);
			try {
				while (sentenceIterator.hasNext()) {
					writer.write(sentenceIterator.next());
				}
			} finally { closeQuietly(writer); }
		} finally { closeQuietly(sentenceIterator); }
	}
}
