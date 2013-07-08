package edu.cmu.cs.lti.ark.fn.identification;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceTest;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Sets.filter;
import static com.google.common.io.Resources.getResource;
import static edu.cmu.cs.lti.ark.fn.identification.SennaFeatureExtractor.FIVE_WORD_WINDOW_NAMES;
import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author sthomson@cs.cmu.edu
 */
public class SennaFeatureExtractorTest {
	private SennaFeatureExtractor sennaFeatureExtractor;

	private List<Sentence> getSentenceFixtures() throws IOException {
		final InputSupplier<InputStreamReader> input =
				Resources.newReaderSupplier(getResource(SentenceTest.CONNL_FILENAME), UTF_8);
		SentenceCodec.SentenceIterator sentenceIterator = SentenceCodec.ConllCodec.readInput(input.getInput());
		try {
			return ImmutableList.copyOf(sentenceIterator);
		} finally { closeQuietly(sentenceIterator); }
	}

	@Before
	public void setUp() throws Exception {
		sennaFeatureExtractor = SennaFeatureExtractor.load();
	}

	@Test
	public void testGetBaseFeatures() throws Exception {
		final Sentence sentence = getSentenceFixtures().get(0);
		// extract features for "kitchen"
		final Map<String,Double> baseFeatures = sennaFeatureExtractor.getSennaFeatures(new int[]{1}, sentence);
		// should not contain senna features for word two before target head (index is out of bounds)
		assertFalse(any(baseFeatures.keySet(), new Predicate<String>() {
			public boolean apply(String input) {
				return input.startsWith(FIVE_WORD_WINDOW_NAMES[0]);
			}
		}));
		// should contain 50 features for each of "my" and "kitchen"
		for (final int i : xrange(1, 3)) {
			final Set<String> featuresForWord = filter(baseFeatures.keySet(), new Predicate<String>() {
				public boolean apply(String input) { return input.startsWith(FIVE_WORD_WINDOW_NAMES[i]); } });
			assertEquals(Senna.SENNA_VECTOR_DIM, featuresForWord.size());
		}
		// should not contain non-senna features
		final Map<String, Double> nonSennaFeatures = Maps.filterKeys(baseFeatures, new Predicate<String>() {
			public boolean apply(String input) { return !input.contains("senna"); } });
		assertEquals(0, nonSennaFeatures.size());
	}
}
