package edu.cmu.cs.lti.ark.fn.wordnet;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author sthomson@cs.cmu.edu
 */
public class WordNetRelationsTest {
	@Test
	public void testWordIsLowercased() throws Exception {
		final WordNetRelations wordNetRelations = new WordNetRelations();
		final String lemma = wordNetRelations.getLemmaForWord("Nuclear", "A");
		Assert.assertEquals("nuclear", lemma);
	}
}
