package edu.cmu.cs.lti.ark.fn.parsing;

import com.google.common.collect.ImmutableList;
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;

import java.util.List;

import static edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements.FrameElementAndSpan;
import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

/**
* @author sthomson@cs.cmu.edu
*/
public class RankedScoredRoleAssignment {
	public final int rank;
	public final double score;
	public final String frame;
	public final String targetLemma;
	public final Range0Based targetSpan;
	public final String targetTokens;
	public final int sentenceIdx;
	public final List<FrameElementAndSpan> fesAndSpans;

	public RankedScoredRoleAssignment(int rank,
									  double score,
									  String frame,
									  String targetLemma,
									  Range0Based targetSpan,
									  String targetTokens,
									  int sentenceIdx,
									  List<FrameElementAndSpan> fesAndSpans) {
		this.rank = rank;
		this.score = score;
		this.frame = frame;
		this.targetLemma = targetLemma;
		this.targetSpan = targetSpan;
		this.targetTokens = targetTokens;
		this.sentenceIdx = sentenceIdx;
		this.fesAndSpans = fesAndSpans;
	}

	public static RankedScoredRoleAssignment fromPredictionLine(String frameElementsString) {
		final String[] fields = frameElementsString.split("\t");
		final int rank = parseInt(fields[0]);
		final double score = parseDouble(fields[1]);
		final int count = parseInt(fields[2]);
		final String frame = fields[3];
		final String targetLemma = fields[4];
		final String[] targetSpanStr = fields[5].split("_");
		final Range0Based targetSpan = new Range0Based(parseInt(targetSpanStr[0]), parseInt(targetSpanStr[targetSpanStr.length-1]));
		final String targetTokens = fields[6];
		final int sentenceIdx = parseInt(fields[7]);
		// Frame elements
		ImmutableList.Builder<FrameElementAndSpan> fesAndSpans = ImmutableList.builder();
		for(int i : xrange(count - 1)) {
			final String feName = fields[8+2*i];
			final Range0Based feSpan = DataPointWithFrameElements.getSpan(fields[9 + 2 * i]);
			fesAndSpans.add(new FrameElementAndSpan(feName, feSpan));
		}
		return new RankedScoredRoleAssignment(rank, score, frame, targetLemma, targetSpan, targetTokens, sentenceIdx, fesAndSpans.build());
	}
}
