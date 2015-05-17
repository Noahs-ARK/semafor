package edu.cmu.cs.lti.ark.fn.parsing

import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements
import edu.cmu.cs.lti.ark.util.ds.Range0Based
import RankedScoredRoleAssignment.spanToStr

/** @author sthomson@cs.cmu.edu */
case class RankedScoredRoleAssignment(rank: Int,
                                      score: Double,
                                      frame: String,
                                      targetLemma: String,
                                      targetSpan: Range0Based,
                                      targetTokens: String,
                                      sentenceIdx: Int,
                                      fesAndSpans: Array[DataPointWithFrameElements.FrameElementAndSpan]) {
  def toLine: String = {
    Seq(
      rank,
      score,
      frame,
      targetLemma,
      spanToStr("_")(targetSpan),
      targetTokens,
      sentenceIdx,
      fesAndSpans.map(fe => "%s\t%s".format(fe.name, spanToStr(":")(targetSpan))).mkString("\t")
    ).mkString("\t")
  }
}

object RankedScoredRoleAssignment {
  def fromLine(frameElementsString: String): RankedScoredRoleAssignment = {
    val fields: Array[String] = frameElementsString.split("\t")
    val fesAndSpans = for (
      Array(feName, feSpanStr) <- fields.slice(8, fields.length).grouped(2)
    ) yield {
      new DataPointWithFrameElements.FrameElementAndSpan(feName, parseSpan(":")(feSpanStr))
    }
    RankedScoredRoleAssignment(
      rank = fields(0).toInt,
      score = fields(1).toDouble,
      frame = fields(3),
      targetLemma = fields(4),
      targetSpan = parseSpan("_")(fields(5)),
      targetTokens = fields(6),
      sentenceIdx = fields(7).toInt,
      fesAndSpans = fesAndSpans.toArray
    )
  }

  def parseSpan(delimiter: String)(spanStr: String): Range0Based = {
    val spanStrs = spanStr.split(delimiter)
    new Range0Based(spanStrs(0).toInt, spanStrs(spanStrs.length - 1).toInt)
  }
  
  def spanToStr(delimiter: String)(span: Range0Based): String = "%d%s%d".format(span.start, delimiter, span.end)
}
