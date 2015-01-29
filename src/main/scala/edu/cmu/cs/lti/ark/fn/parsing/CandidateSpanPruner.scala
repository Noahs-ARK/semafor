package edu.cmu.cs.lti.ark.fn.parsing

import java.util

import edu.cmu.cs.lti.ark.fn.parsing.CandidateFrameElementFilters._
import edu.cmu.cs.lti.ark.fn.parsing.CandidateSpanPruner.NON_BREAKING_LEFT_CONSTITUENT_POS
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse

import scala.collection.JavaConverters._
import scala.collection.mutable


/**
 * Uses a dependency parse to prune the options of candidate spans.
 */
class CandidateSpanPruner {
  /** Finds a set of candidate spans based on a dependency parse */
  def candidateSpansAndGoldSpans(dataPoint: DataPointWithFrameElements): util.List[DataPrep.SpanAndParseIdx] = {
    val candidates = candidateSpans(dataPoint.getParses.getBestParse)
    val goldSpans = dataPoint.getOvertFrameElementFillerSpans.asScala.map(span => (span.start, span.end))
    val result = for ((i, j) <- candidates ++ goldSpans) yield {
      new DataPrep.SpanAndParseIdx(createSpanRange(i, j), 0)
    }
    result.toList.asJava
  }

  /**
   * Calculates an array of constituent spans based on the given dependency parse.
   * A constituent span is a token and all of its descendants.
   * Adds the constituents to spanMatrix and heads
   *
   * @param depParse the dependency parse
   */
  def candidateSpans(depParse: DependencyParse): Set[(Int, Int)] = {
    val result = mutable.Set[(Int, Int)]((-1, -1)) // always include the empty span
    val nodes = depParse.getIndexSortedListOfNodes
    val length = nodes.length - 1
    // single tokens are always considered
    result ++= (0 until length).map(i => (i, i))
    val subTrees = subTreeSpans(depParse)
    // full constituents
    result ++= subTrees
    // heuristics to try to recover finer-grained constituents when a node has multiple descendants
    for (i <- 0 until length) {
      val (leftMost, rightMost) = subTrees(i)
      if (leftMost < i && rightMost > i) {   // node has descendants on both sides
        val justLeft = i - 1
        if (
          justLeft >= 0 &&
            result.contains((leftMost, justLeft)) &&
            !(justLeft == leftMost && NON_BREAKING_LEFT_CONSTITUENT_POS.contains(nodes(justLeft + 1).getPOS))
        ) {
          // include self and right children
          result.add((i, rightMost))
        }
        val justRight = i + 1
        if (justRight <= length - 1 && result.contains((justRight, rightMost))) {
          // include self and left children
          result.add((leftMost, i))
        }
      }
    }
    result.toSet
  }

  def subTreeSpans(depParse: DependencyParse): Array[(Int, Int)] = {
    val nodes = depParse.getIndexSortedListOfNodes
    val length = nodes.length - 1
    // left[i] is the index of the left-most descendant of i
    val left = (0 until length).toArray
    // right[i] is the index of the right-most descendant of i
    val right = (0 until length).toArray
    val parents = nodes.drop(1).map(_.getParentIndex - 1)  // nodes are 1-indexed
    for (i <- 0 until length) {
      // walk up i's ancestors, expanding their boundaries to include i if necessary
      var parentIdx = parents(i)
      while (parentIdx >= 0) {
        if (left(parentIdx) > i) {
          left(parentIdx) = i
        } else if (right(parentIdx) < i) {
          right(parentIdx) = i
        }
        parentIdx = parents(parentIdx)
      }
    }
    left zip right
  }
}
object CandidateSpanPruner {
  val NON_BREAKING_LEFT_CONSTITUENT_POS: Set[String] = Set("DT", "JJ")
}
