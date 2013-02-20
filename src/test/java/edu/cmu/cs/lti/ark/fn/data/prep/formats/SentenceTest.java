package edu.cmu.cs.lti.ark.fn.data.prep.formats;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.io.Resources;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.io.Resources.getResource;
import static edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec.*;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.junit.Assert.assertEquals;


/**
 * @author sthomson@cs.cmu.edu
 */
public class SentenceTest {
	public static final String CONNL_FILENAME = "fixtures/example.conll";
	private static final String POS_FILENAME = "fixtures/example.pos.tagged";
	private static final String MALT_FILENAME = "fixtures/example.maltparsed";
	private static final String TOKENIZED_FILENAME = "fixtures/example.tokenized";
	private BiMap<String, SentenceCodec> codecMap = ImmutableBiMap.of(
				CONNL_FILENAME, ConllCodec,
				POS_FILENAME, PosTaggedCodec,
				MALT_FILENAME, MaltCodec,
				TOKENIZED_FILENAME, TokenizedCodec);

	private String writeAll(SentenceIterator sentenceIterator, SentenceCodec codec) throws IOException {
		StringWriter stringWriter = new StringWriter();
		SentenceWriter writer = new SentenceWriter(codec, stringWriter);
		for(Sentence sentence : copyOf(sentenceIterator)) {
			writer.write(sentence);
		}
		return stringWriter.toString();
	}

	private void testFromTo(String inFilename, String outFilename)
			throws IOException {
		final String expected = Resources.toString(getResource(outFilename), UTF_8);
		final SentenceCodec reader = codecMap.get(inFilename);
		final SentenceCodec writer = codecMap.get(outFilename);
		// read in and convert to sentences
		final InputStreamReader inputStream = Resources.newReaderSupplier(getResource(inFilename), UTF_8).getInput();

		SentenceIterator sentenceIterator = reader.readInput(inputStream);
		try {
			// convert back to strings
			// verify we end up with what we started with
			assertEquals(expected, writeAll(sentenceIterator, writer));
		} finally {
			closeQuietly(sentenceIterator);
		}
	}

	@Test
	public void testToAndFromConll() throws IOException {
		testFromTo(CONNL_FILENAME, CONNL_FILENAME);
	}

	@Test
	public void testToAndFromPosTagged() throws IOException {
		testFromTo(POS_FILENAME, POS_FILENAME);
	}

	@Test
	public void testToAndFromMalt() throws IOException {
		testFromTo(MALT_FILENAME, MALT_FILENAME);
	}

	@Test
	public void testMaltToConnl() throws IOException {
		testFromTo(MALT_FILENAME, CONNL_FILENAME);
	}

	@Test
	public void testMaltToPosTagged() throws IOException {
		testFromTo(MALT_FILENAME, POS_FILENAME);
	}

	@Test
	public void testMaltToTokenized() throws IOException {
		testFromTo(MALT_FILENAME, TOKENIZED_FILENAME);
	}
}
