package edu.cmu.cs.lti.ark.fn.segmentation;

import com.beust.jcommander.internal.Sets;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Token;
import org.junit.Test;

import java.util.List;
import java.util.Set;


/**
 * @author sthomson@cs.cmu.edu
 */
public class RoteSegmenterTest {
	private static String allLemmaTags = "24\t0.2\tmiles\tlater\tthe\ttrail\tand\tthe\tcreek\trun\tnearly\tlevel\twith\teach\tother\tand\tthe\tsound\tof\tMinnehaha\tFalls\tfills\tthe\tair\t.\tCD\tNNS\tRB\tDT\tNN\tCC\tDT\tNN\tNN\tRB\tNN\tIN\tDT\tJJ\tCC\tDT\tNN\tIN\tNNP\tNNP\tVBZ\tDT\tNN\t.\tNMOD\tAMOD\tADV\tNMOD\tNMOD\tCC\tNMOD\tNMOD\tCOORD\tNMOD\tSBJ\tNMOD\tNMOD\tPMOD\tCC\tNMOD\tCOORD\tNMOD\tNMOD\tPMOD\tROOT\tNMOD\tOBJ\tP\t2\t3\t21\t5\t11\t5\t9\t9\t5\t11\t21\t11\t14\t12\t14\t17\t14\t17\t20\t18\t0\t23\t21\t21\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\t0.2\tmile\tlater\tthe\ttrail\tand\tthe\tcreek\trun\tnearly\tlevel\twith\teach\tother\tand\tthe\tsound\tof\tminnehaha\tfalls\tfill\tthe\tair\t.";
	private static Sentence sentence = Sentence.fromAllLemmaTagsArray(AllLemmaTags.readLine(allLemmaTags));

	@Test
	public void testExcludesPrepositions() {
		// add every lemma in the sentence as a related word
		Set<String> allRelatedWords = Sets.newHashSet();
		final List<Token> tokens = sentence.getTokens();
		for (Token token : tokens) {
			allRelatedWords.add(token.getLemma() + "_" + token.getPostag().substring(0, 1));
		}
		final RoteSegmenter segmenter = new RoteSegmenter(allRelatedWords);
		// System.out.println(sentence.getTokens());
		final List<String> segmentation = segmenter.getSegmentation(sentence);
		System.out.println(segmentation);
		for(String tok : segmentation) {
			final int idx = Integer.parseInt(tok);
			final Token token = tokens.get(idx);
			System.out.println(token.getForm() + "_" + token.getPostag());
		}
	}
}
