package edu.cmu.cs.lti.ark.fn.segmentation;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Token;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


/**
 * @author sthomson@cs.cmu.edu
 */
public class RoteSegmenterTest {
	private static String allLemmaTags = "24\t" +
			"0.2\tmiles\tlater\tthe\ttrail\tand\tthe\tcreek\trun\tnearly\tlevel\twith\teach\tother\tand\tthe\tsound\tof\tMinnehaha\tFalls\tfills\tthe\tair\t.\t" +
			"CD\tNNS\tRB\tDT\tNN\tCC\tDT\tNN\tNN\tRB\tNN\tIN\tDT\tJJ\tCC\tDT\tNN\tIN\tNNP\tNNP\tVBZ\tDT\tNN\t.\t" +
			"NMOD\tAMOD\tADV\tNMOD\tNMOD\tCC\tNMOD\tNMOD\tCOORD\tNMOD\tSBJ\tNMOD\tNMOD\tPMOD\tCC\tNMOD\tCOORD\tNMOD\tNMOD\tPMOD\tROOT\tNMOD\tOBJ\tP\t" +
			"2\t3\t21\t5\t11\t5\t9\t9\t5\t11\t21\t11\t14\t12\t14\t17\t14\t17\t20\t18\t0\t23\t21\t21\t" +
			"O\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\t" +
			"0.2\tmile\tlater\tthe\ttrail\tand\tthe\tcreek\trun\tnearly\tlevel\twith\teach\tother\tand\tthe\tsound\tof\tminnehaha\tfalls\tfill\tthe\tair\t.";

	private static Sentence sentence = Sentence.fromAllLemmaTagsArray(AllLemmaTags.readLine(allLemmaTags));

	@Test
	public void testExcludesPrepositions() {
		// add every lemma in the sentence as a related word
		Set<String> allRelatedWords = Sets.newHashSet();
		final List<Token> tokens = sentence.getTokens();
		for (Token token : tokens) {
			allRelatedWords.add(token.getLemma() + "_" + token.getCpostag());
		}
		final RoteSegmenter segmenter = new RoteSegmenter(allRelatedWords);
		final List<List<Integer>> segmentation = segmenter.getSegmentation(sentence);
		// should exclude "the", "and", "with", "of".
		final Integer[] expectedTargets = {0, 1, 2, 4, 7, 8, 9, 10, 12, 13, 16, 18, 19, 20, 22, 23};
		for (int i : xrange(expectedTargets.length)) {
			Integer[] expectedTarget = {expectedTargets[i]};
			assertArrayEquals(expectedTarget, segmentation.get(i).toArray());
		}
	}

	@Test
	public void testIncludesPossessiveHave() {
		final String conll =
				"1\tI\ti\tPRP\tPRP\t_\t2\tnsubj\t_\t_\n" +
				"2\thave\thave\tVBP\tVBP\t_\t0\tnull\t_\t_\n" +
				"3\tmoney\tmoney\tNN\tNN\t_\t2\tdobj\t_\t_\n" +
				"4\t.\t.\t.\t.\t_\t2\tpunct\t_\t_";
		final Sentence sentence = SentenceCodec.ConllCodec.decode(conll);
		Set<String> allRelatedWords = ImmutableSet.of("have_V");
		final RoteSegmenter segmenter = new RoteSegmenter(allRelatedWords);
		final List<List<Integer>> segmentation = segmenter.getSegmentation(sentence);
		assertEquals(1, segmentation.size());
		final Integer[] expected = {1};
		assertArrayEquals(expected, segmentation.get(0).toArray());
	}
}
