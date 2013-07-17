package edu.cmu.cs.lti.ark.fn.parsing;

import com.google.common.collect.ImmutableList;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec.MaltCodec;
import static edu.cmu.cs.lti.ark.fn.parsing.CandidateFrameElementFilters.createSpanRange;
import static edu.cmu.cs.lti.ark.fn.parsing.DataPrep.SpanAndParseIdx;
import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;

/**
 * @author sthomson@cs.cmu.edu
 */
public class DataPrepTest {
	final String maltLine = "My/PRP$/2/NMOD kitchen/NN/5/SBJ no/RB/5/ADV longer/RB/3/AMOD smells/VBZ/0/ROOT ././5/P";
	private Sentence sentence = MaltCodec.decode(maltLine);
	final String frameElementsLine = "0\t1.0\t1\tTemporal_collocation\tno.r\t2_3\tno longer\t0\n";

	private List<SpanAndParseIdx> findSpans() {
		final DataPointWithFrameElements dataPointWithElements =
				new DataPointWithFrameElements(sentence, frameElementsLine);
		final int kBestParses = 1;
		return DataPrep.findSpans(dataPointWithElements, kBestParses);
	}

	@Test
	public void testFindSpansIncludesNullSpan() {
		final List<SpanAndParseIdx> spanList = findSpans();
		Assert.assertTrue(spanList.contains(SpanAndParseIdx.EMPTY_SPAN_AND_PARSE_IDX));
	}

	@Test
	public void testFindSpansIncludesSingletons() {
		final List<SpanAndParseIdx> spanList = findSpans();
		for(int i : xrange(sentence.size())) {
			Assert.assertTrue(spanList.contains(new SpanAndParseIdx(createSpanRange(i, i), 0)));
		}
	}

	@Test
	public void testFindSpansIncludesConstituents() {
		final List<SpanAndParseIdx> spanList = findSpans();
		List<SpanAndParseIdx> expectedSpans = ImmutableList.of(
				new SpanAndParseIdx(createSpanRange(0, 1), 0), // my -> kitchen
				new SpanAndParseIdx(createSpanRange(2, 3), 0), // no <- longer
				new SpanAndParseIdx(createSpanRange(0, 5), 0)  // everything
		);
		for(SpanAndParseIdx expectedSpan : expectedSpans) {
			Assert.assertTrue(String.format("spanList doesn't contain %s", expectedSpan.span),
					spanList.contains(expectedSpan));
		}
	}
}
