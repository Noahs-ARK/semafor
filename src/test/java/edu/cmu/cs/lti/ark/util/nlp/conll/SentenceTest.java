package edu.cmu.cs.lti.ark.util.nlp.conll;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static edu.cmu.cs.lti.ark.util.nlp.conll.Sentence.SentenceIterator;



/**
 * @author sthomson@cs.cmu.edu
 */
public class SentenceTest {
	private static final String CONNL_FILENAME = "fixtures/example.conll.txt";
	private static File CONNL_FILE;

	@Before
	public void before() throws URISyntaxException {
		CONNL_FILE = new File(getClass().getClassLoader().getResource(CONNL_FILENAME).toURI());
	}

	@Test
	public void testSentenceIterator() throws IOException {

		final String original = Files.toString(CONNL_FILE, Charsets.UTF_8);

		// read in and convert to sentences
		SentenceIterator it = new SentenceIterator(CONNL_FILE);
		try {
			// convert back to strings
			final String output = Joiner.on("").join(it);
			// verify we end up with what we started with
			org.junit.Assert.assertEquals(original, output);
		} finally {
			it.close();
		}
	}
}
