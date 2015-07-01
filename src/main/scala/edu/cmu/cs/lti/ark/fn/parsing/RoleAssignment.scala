package edu.cmu.cs.lti.ark.fn.parsing

import com.google.common.collect.Ordering
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements
import edu.cmu.cs.lti.ark.util.ds.Range0Based
import RankedScoredRoleAssignment.spanToStr


/** An assignment of spans to roles of a particular frame */
case class RoleAssignment(overtAssignments: Map[String, Range0Based],
                          nullAssignments: Map[String, Range0Based]) extends Comparable[RoleAssignment] {
  def plus(key: String, value: Range0Based): Option[RoleAssignment] = {
    if (overlaps(value)) {
      None
    } else Some(
      if (value.isEmpty) {
        RoleAssignment(overtAssignments, nullAssignments + (key -> value))
      } else {
        RoleAssignment(overtAssignments + (key -> value), nullAssignments)
      }
    )
  }

  /** Determines whether the given span overlaps with any of our spans */
  def overlaps(otherSpan: Range0Based): Boolean = {
    if (otherSpan.isEmpty) {
      false
    } else {
      overtAssignments.values.exists(otherSpan.overlaps)
    }
  }


  override def toString: String = {
    def spanToString(span: Range0Based): String = if (span.start == span.end) {
      span.start.toString
    } else {
      span.start + ":" + span.end
    }

    overtAssignments.map({ case (key, span) => key + "\t" + spanToString(span) }).mkString("\t")
  }

  override def equals(other: Any): Boolean = {
    other != null && other.isInstanceOf[RoleAssignment] && compareTo(other.asInstanceOf[RoleAssignment]) == 0
  }

  override def compareTo(other: RoleAssignment): Int = Ordering.usingToString().compare(this, other)
}
object RoleAssignment {
  def empty(): RoleAssignment = RoleAssignment(Map.empty[String, Range0Based], Map.empty[String, Range0Based])
}


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
      fesAndSpans.map(fe => s"${fe.name}\t${spanToStr(":")(targetSpan)}").mkString("\t")
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

  def spanToStr(delimiter: String)(span: Range0Based): String = s"${span.start}$delimiter${span.end}"
}
