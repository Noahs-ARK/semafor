package edu.cmu.cs.lti.ark.util.nlp;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author sthomson@cs.cmu.edu
 */
public class MorphaLemmatizerTest {
	@Test
	public void testWordIsLowercased() throws Exception {
		final Lemmatizer lemmatizer = new MorphaLemmatizer();
		final String lemma = lemmatizer.getLemma("Nuclear", "JJ");
		Assert.assertEquals("nuclear", lemma);
	}

	@Test
	public void testContractions() {
		final Lemmatizer lemmatizer = new MorphaLemmatizer();
		Assert.assertEquals("'s", lemmatizer.getLemma("'s", "POS"));
		Assert.assertEquals("have", lemmatizer.getLemma("'ve", "VB"));
		Assert.assertEquals("not", lemmatizer.getLemma("n't", "RB"));
		Assert.assertEquals("be", lemmatizer.getLemma("'s", "VBZ"));
		Assert.assertEquals("will", lemmatizer.getLemma("'ll", "VB"));
		Assert.assertEquals("be", lemmatizer.getLemma("'re", "VBP"));
	}

	@Test
	public void testIrregulars() {
		final Lemmatizer lemmatizer = new MorphaLemmatizer();
		Assert.assertEquals("fall", lemmatizer.getLemma("fell", "VBD"));
		Assert.assertEquals("find", lemmatizer.getLemma("found", "VBD"));
		Assert.assertEquals("find", lemmatizer.getLemma("found", "VBN"));
		//Assert.assertEquals("lie", lemmatizer.getLemma("lay", "VBD"));
		Assert.assertEquals("see", lemmatizer.getLemma("saw", "VBD"));
		//Assert.assertEquals("person", lemmatizer.getLemma("people", "NNS"));
	}

	@Test
	public void testNullIsRetained() {
		// MorphaLemmatizer used to turn "null" into ""
		final Lemmatizer lemmatizer = new MorphaLemmatizer();
		Assert.assertEquals("null", lemmatizer.getLemma("null", "JJ"));
		Assert.assertEquals("null", lemmatizer.getLemma("nulls", "NN"));
	}

	@Test
	public void testAngleBracketsHandled() {
		// MorphaLemmatizer used to throw "java.lang.Error: Error: could not match input" for ">"
		final Lemmatizer lemmatizer = new MorphaLemmatizer();
		Assert.assertEquals(">", lemmatizer.getLemma(">", "JJR"));
		Assert.assertEquals("<", lemmatizer.getLemma("<", "JJR"));
	}
}
