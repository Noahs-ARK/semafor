package edu.cmu.cs.lti.ark.fn.data.prep.formats;

import com.google.common.base.Charsets;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;

import static com.google.common.collect.ImmutableList.copyOf;
import static edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec.*;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.junit.Assert.assertEquals;


/**
 * @author sthomson@cs.cmu.edu
 */
public class SentenceTest {
	private static final String CONNL_FILENAME = "fixtures/example.conll";
	private File CONNL_FILE;
	private static final String POS_FILENAME = "fixtures/example.pos.tagged";
	private File POS_FILE;
	private static final String MALT_FILENAME = "fixtures/example.maltparsed";
	private File MALT_FILE;
	private static final String TOKENIZED_FILENAME = "fixtures/example.tokenized";
	private File TOKENIZED_FILE;
	private BiMap<File, SentenceCodec> codecMap;

	@Before
	@SuppressWarnings("ConstantConditions")
	public void before() throws URISyntaxException {
		final ClassLoader classLoader = getClass().getClassLoader();
		CONNL_FILE = new File(classLoader.getResource(CONNL_FILENAME).toURI());
		POS_FILE = new File(classLoader.getResource(POS_FILENAME).toURI());
		MALT_FILE = new File(classLoader.getResource(MALT_FILENAME).toURI());
		TOKENIZED_FILE = new File(classLoader.getResource(TOKENIZED_FILENAME).toURI());
		codecMap = ImmutableBiMap.of(
				CONNL_FILE, ConllCodec,
				POS_FILE, PosTaggedCodec,
				MALT_FILE, MaltCodec,
				TOKENIZED_FILE, TokenizedCodec);
	}

	private String writeAll(SentenceIterator sentenceIterator, SentenceCodec codec) throws IOException {
		StringWriter stringWriter = new StringWriter();
		SentenceWriter writer = new SentenceWriter(codec, stringWriter);
		for(Sentence sentence : copyOf(sentenceIterator)) {
			writer.write(sentence);
		}
		return stringWriter.toString();
	}

	private void testFromTo(File inFile, File outFile)
			throws IOException {
		final String expected = Files.toString(outFile, Charsets.UTF_8);
		final SentenceCodec reader = codecMap.get(inFile);
		final SentenceCodec writer = codecMap.get(outFile);
		// read in and convert to sentences
		SentenceIterator sentenceIterator = reader.readFile(inFile);
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
		testFromTo(CONNL_FILE, CONNL_FILE);
	}

	@Test
	public void testToAndFromPosTagged() throws IOException {
		testFromTo(POS_FILE, POS_FILE);
	}

	@Test
	public void testToAndFromMalt() throws IOException {
		testFromTo(MALT_FILE, MALT_FILE);
	}

	@Test
	public void testMaltToConnl() throws IOException {
		testFromTo(MALT_FILE, CONNL_FILE);
	}

	@Test
	public void testMaltToPosTagged() throws IOException {
		testFromTo(MALT_FILE, POS_FILE);
	}

	@Test
	public void testMaltToTokenized() throws IOException {
		testFromTo(MALT_FILE, TOKENIZED_FILE);
	}

}
