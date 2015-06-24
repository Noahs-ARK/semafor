package edu.cmu.cs.lti.ark.fn.parsing

import java.util

import edu.cmu.cs.lti.ark.util.ds.Range0Based
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse

import scala.collection.JavaConverters._
import scala.collection.{mutable => m}
import scala.math.{max, min}


object CandidateSpanPruner {
  val EMPTY_SPAN: Range0Based = new Range0Based(-1, -1, false)
  val NON_BREAKING_LEFT_CONSTITUENT_POS: Set[String] = Set("DT", "JJ")
  val PUNCTUATION_POS: Set[String] = Set(".", ",", ":")

  def range(start: Int, end: Int): Range0Based = {
    if ((start < 0 && end < 0) || start > end) EMPTY_SPAN else new Range0Based(start, end, true)
  }

  def groupContiguous[A](xs: Iterable[A], p: A => Boolean): List[Iterable[A]] = {
    val results = m.Buffer.empty[Iterable[A]]
    var remaining = xs
    while (remaining.nonEmpty) {
      val (chunk, rest) = remaining.span(p)
      if (chunk.nonEmpty) results += chunk
      remaining = rest.drop(1)
    }
    results.toList
  }
}

/** Uses a dependency parse to prune the options of candidate spans. */
class CandidateSpanPruner {
  import CandidateSpanPruner._

  /** Calculates a set of candidate spans based on the given dependency parse. */
  def candidateSpans(depParse: DependencyParse,
                     doStripPunctuation: Boolean = true,
                     doIncludeContiguousSubspans: Boolean = false): util.List[Range0Based] = {
    val result = m.Set[Range0Based](EMPTY_SPAN) // always include the empty span
    val nodes = depParse.getIndexSortedListOfNodes.drop(1) // drop the dummy root node
    result ++= nodes.indices.map(i => range(i, i))  // single tokens are always considered
    // full subtrees
    val subtrees = getFullSubtrees(nodes, doStripPunctuation)
    result ++= subtrees
    // contiguous spans of subtrees
    if (doIncludeContiguousSubspans) {
      result ++= contiguousSubspans(nodes, subtrees)
    }
    // heuristics for less-than-full subtrees
    result ++= johanssonFinerGrained(nodes, subtrees)
    result.toList.asJava
  }

  /** For each token, find its left-most and right-most descendant. */
  def getFullSubtrees(nodes: Array[DependencyParse],
                      doStripPunctuation: Boolean = true): Array[Range0Based] = {
    val parents = nodes.map(_.getParentIndex - 1)  // nodes are 1-indexed
    val left = nodes.indices.toArray  // left[i] is the index of the left-most descendant of i
    val right = nodes.indices.toArray  // right[i] is the index of the right-most descendant of i
    for (
      i <- nodes.indices
      // if `doStripPunctuation`, never start or end a span with punctuation
      if !doStripPunctuation || !PUNCTUATION_POS.contains(nodes(i).getPOS);
      // walk up i's ancestors, expanding their boundaries to include i if necessary
      ancestorIdx <- ancestors(parents)(i)
    ) {
      left(ancestorIdx) = min(i, left(ancestorIdx))
      right(ancestorIdx) = max(i, right(ancestorIdx))
    }
    (left zip right).map({ case (startIdx, endIdx) => range(startIdx, endIdx) })
  }

  def ancestors(parents: Int => Int)(i: Int): Iterator[Int] = Iterator.iterate(i)(parents).drop(1).takeWhile(_ >= 0)

  def contiguousSubspans(nodes: Array[DependencyParse], subtrees: Array[Range0Based]): Array[Range0Based] = {
    val parents = nodes.map(_.getParentIndex - 1)
    for (
      (subTree, i) <- subtrees.zipWithIndex;
      span = subTree.start to subTree.end;
      chunk <- groupContiguous(span, (j: Int) => j == i || span.contains(parents(j)))
    ) yield range(chunk.head, chunk.last)
  }

  // heuristics to try to recover finer-grained constituents when a node has descendants on both sides
  def johanssonFinerGrained(nodes: Array[DependencyParse],
                            subtrees: Array[Range0Based]): Set[Range0Based] = {
    val result = m.Set.empty[Range0Based]
    for (
      (subtree, i) <- subtrees.zipWithIndex
      if subtree.start < i && subtree.end > i // node has both left and right children
    ) {
      if (subtrees.contains(range(subtree.start, i - 1)) && // node has exactly one left child
          // never leave a single determiner or adj dangling
          !(i - 1 == subtree.start && NON_BREAKING_LEFT_CONSTITUENT_POS.contains(nodes(i - 1).getPOS))) {
        // include self and right children
        result += range(i, subtree.end)
      }
      if (result.contains(range(i + 1, subtree.end))) { // node has exactly one right child
        // include self and left children
        result += range(subtree.start, i)
      }
    }
    result.toSet
  }
}
