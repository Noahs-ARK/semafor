package edu.cmu.cs.lti.ark.fn.parsing;

import com.google.common.collect.ImmutableList;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec.MaltCodec;
import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;

/**
 * @author sthomson@cs.cmu.edu
 */
public class DataPrepTest {
	final String maltLine = "My/PRP$/2/NMOD kitchen/NN/5/SBJ no/RB/5/ADV longer/RB/3/AMOD smells/VBZ/0/ROOT ././5/P";
	private Sentence sentence = MaltCodec.decode(maltLine);
	final String frameElementsLine = "1\tTemporal_collocation\tno.r\t2_3\tno longer\t0\n"; // +
	//"2\tBuilding_subparts\tkitchen.n\t1\tkitchen\t0\tBuilding_part\t1\n" +
			//"1\tSensation\tsmells.v\t4\tsmells\t0\n";

	private boolean contains(Iterable<int[]> collection, int[] item) {
		for(int[] t : collection) {
			if(Arrays.equals(t, item)) return true;
		}
		return false;
	}

	private ArrayList<int[]> findSpans() {
		final DataPointWithFrameElements dataPointWithElements =
				new DataPointWithFrameElements(sentence, frameElementsLine);
		final boolean useOracleSpans = false;
		final int kBestParses = 1;
		return DataPrep.findSpans(dataPointWithElements, useOracleSpans, kBestParses);
	}

	@Test
	public void testFindSpansIncludesNullSpan() {
		final ArrayList<int[]> spanList = findSpans();
		Assert.assertTrue(contains(spanList, new int[]{-1, -1, 0}));
	}

	@Test
	public void testFindSpansIncludesSingletons() {
		final ArrayList<int[]> spanList = findSpans();
		for(int i : xrange(sentence.size())) {
			Assert.assertTrue(contains(spanList, new int[]{i, i, 0}));
		}
	}

	@Test
	public void testFindSpansIncludesConstituents() {
		final ArrayList<int[]> spanList = findSpans();
		List<int[]> expectedSpans = ImmutableList.of(
				new int[] {0, 1, 0}, // my -> kitchen
				new int[] {2, 3, 0}, // no <- longer
				new int[] {0, 5, 0}  // everything
		);
		for(int[] expectedSpan : expectedSpans) {
			Assert.assertTrue(String.format("spanList doesn't contain %s", Arrays.toString(expectedSpan)),
					contains(spanList, expectedSpan));
		}
	}

	@Test
	public void testFindSpansIncludesLeftConstituents() {
		// TODO
	}
}
