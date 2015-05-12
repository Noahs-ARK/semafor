package edu.cmu.cs.lti.ark.fn.parsing

import java.util

import edu.cmu.cs.lti.ark.fn.parsing.CandidateSpanPruner.NON_BREAKING_LEFT_CONSTITUENT_POS
import edu.cmu.cs.lti.ark.util.ds.Range0Based
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse

import scala.collection.JavaConverters._
import scala.collection.mutable


object CandidateSpanPruner {
  val EMPTY_SPAN: Range0Based = new Range0Based(-1, -1, false)
  val NON_BREAKING_LEFT_CONSTITUENT_POS: Set[String] = Set("DT", "JJ")

  def createSpanRange(start: Int, end: Int): Range0Based = {
    if (start < 0 && end < 0) EMPTY_SPAN else new Range0Based(start, end, true)
  }
}
/** Uses a dependency parse to prune the options of candidate spans. */
class CandidateSpanPruner {
  import CandidateSpanPruner.EMPTY_SPAN

  /**
   * Calculates a set of constituent spans based on the given dependency parse.
   * A constituent span is a token and all of its descendants.
   *
   * @param depParse the dependency parse
   */
  def candidateSpans(depParse: DependencyParse): util.List[Range0Based] = {
    val result = mutable.Set[Range0Based](EMPTY_SPAN) // always include the empty span
    val nodes = depParse.getIndexSortedListOfNodes
    val length = nodes.length - 1
    // single tokens are always considered
    result ++= (0 until length).map(i => new Range0Based(i, i))
    val subTrees = subTreeSpans(depParse)
    // full constituents
    result ++= subTrees
    // heuristics to try to recover finer-grained constituents when a node has multiple descendants
    for (i <- 0 until length) {
      val (leftMost, rightMost) = (subTrees(i).start, subTrees(i).end)
      if (leftMost < i && rightMost > i) {   // node has descendants on both sides
        val justLeft = i - 1
        if (
          justLeft >= 0 &&
            result.contains(new Range0Based(leftMost, justLeft)) &&
            !(justLeft == leftMost && NON_BREAKING_LEFT_CONSTITUENT_POS.contains(nodes(justLeft + 1).getPOS))
        ) {
          // include self and right children
          result.add(new Range0Based(i, rightMost))
        }
        val justRight = i + 1
        if (justRight <= length - 1 && result.contains(new Range0Based(justRight, rightMost))) {
          // include self and left children
          result.add(new Range0Based(leftMost, i))
        }
      }
    }
    result.toList.asJava
  }

  def subTreeSpans(depParse: DependencyParse): Array[Range0Based] = {
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
    (left zip right).map({ case (startIdx, endIdx) => new Range0Based(startIdx, endIdx) })
  }
}
