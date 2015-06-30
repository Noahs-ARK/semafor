package edu.cmu.cs.lti.ark.fn.parsing

import java.util

import edu.cmu.cs.lti.ark.fn.data.prep.formats.{Token, Sentence}
import edu.cmu.cs.lti.ark.util.ds.Range0Based

import scala.collection.JavaConverters._
import scala.collection.{mutable => m}
import scala.math.{max, min}


object CandidateSpanPruner {
  val EmptySpan: Range0Based = new Range0Based(-1, -1, false)
  val NonBreakingLeftConstituentPos: Set[String] = Set("DT", "JJ")
  val PunctuationPos: Set[String] = Set(".", ",", ":", "''", "-RRB-", "-RSB-")
  val PartsOfSpeechToSplitOn: Set[String] = Set("WDT", "IN", "TO") ++ PunctuationPos
  val StanfordDependencyLabelsNotToSplitOn: Set[String] = Set(
    "advmod",
    "amod",
    "appos",
    "aux",
    "auxpass",
    "cc",
    "conj",
    "dep",
    "det",
    "mwe",
    "neg",
    "nn",
    "npadvmod",
    "num",
    "number",
    "poss",
    "preconj",
    "predet",
    "prep",
    "prt",
    "ps",
    "quantmod",
    "tmod"
  )

  def defaultInstance: CandidateSpanPruner = CandidateSpanPruner(
    doStripPunctuation = true,
    doIncludeContiguousSubspans = true,
    doIncludeTarget = true,
    doIncludeSpansMinusTarget = true,
    doStripPPs = true,
    doFindNNModifiers = true,
    maxLength = Some(20)
  )

  def range(start: Int, end: Int): Range0Based = {
    if (isEmpty(start, end)) EmptySpan else new Range0Based(start, end, true)
  }

  def isEmpty(start: Int, end: Int): Boolean = start < 0 || end < 0 || start > end

  def spanToString(span: Range0Based, sentence: Sentence): String = {
    if (isEmpty(span.start, span.end)) {
      "Empty"
    } else {
      val text = sentence.getTokens.subList(span.start, span.end + 1).asScala.mkString(" ")
      // print 1-based indices to make it easier to collate with conll
      s"${span.start + 1}-${span.end + 1}\t$text"
    }
  }
}

/** Uses a dependency parse to prune the options of candidate spans. */
case class CandidateSpanPruner(doStripPunctuation: Boolean,
                               doIncludeContiguousSubspans: Boolean,
                               doIncludeTarget: Boolean,
                               doIncludeSpansMinusTarget: Boolean,
                               doStripPPs: Boolean,
                               doFindNNModifiers: Boolean,
                               maxLength: Option[Int]) {
  import CandidateSpanPruner._

  /** Calculates a set of candidate spans based on the given dependency parse. */
  def candidateSpans(sentence: Sentence,
                     targetSpan: Range0Based): util.List[Range0Based] = {
    var result = m.Set[Range0Based](EmptySpan) // always include the empty span
    val tokens: Array[Token] = sentence.getTokens.asScala.toArray
    val parents = tokens.map(_.getHead - 1)  // nodes are 1-indexed
    if (doIncludeTarget) {
//      println("target:\n" + spanToString(targetSpan, sentence))
      result += targetSpan
    }
    // single tokens
    val singletons = tokens.indices.map(i => range(i, i))
//    println("singletons:\n" + singletons.map(spanToString(_, sentence)).mkString("\n"))
    result ++= singletons
    // full subtrees
    val subtrees = getFullSubtrees(tokens, parents)
//    println("subtrees:\n" + subtrees.map(spanToString(_, sentence)).mkString("\n"))
    result ++= subtrees
    // contiguous spans of subtrees
    if (doIncludeContiguousSubspans) {
      val contiguous = contiguousSubspans(parents, subtrees)
//      println("contiguous:\n" + contiguous.toSet.diff(result).map(spanToString(_, sentence)).mkString("\n"))
      result ++= contiguous
    }
    // heuristics for less-than-full subtrees
    val halfTrees = getHalfTrees(tokens, subtrees)
//    println("halfTrees:\n" + halfTrees.toSet.diff(result).map(spanToString(_, sentence)).mkString("\n"))
    result ++= halfTrees
    // carve out the target from each span
    if (doIncludeSpansMinusTarget) {
      val minusTarget = spansMinusTarget(result.toSet, targetSpan)
//      println("minusTarget:\n" + minusTarget.toSet.diff(result).map(spanToString(_, sentence)).mkString("\n"))
      result ++= minusTarget
    }
    // strip off PPs, appositives and relative clauses
    if (doStripPPs) {
      val withoutPPs = result.toSet.flatMap(stripPPs(sentence))
//      println("withoutPPs:\n" + withoutPPs.toSet.diff(result).map(spanToString(_, sentence)).mkString("\n"))
      result ++= withoutPPs
    }
    if (doFindNNModifiers && tokens.slice(targetSpan.start, targetSpan.end + 1).exists(_.getPostag.startsWith("NN"))) {
      val nnModifiers = result.flatMap(findNNModifiers(targetSpan, tokens))
//      println("nnModifiers:\n" + nnModifiers.toSet.diff(result).map(spanToString(_, sentence)).mkString("\n"))
      result ++= nnModifiers
    }
    for (maxLen <- maxLength) {
      val (ok, tooLong) = result.partition(_.length <= maxLen)
//      println("tooLong:\n" + tooLong.map(spanToString(_, sentence)).mkString("\n"))
      result = ok
    }
    result.toList.asJava
  }

  def findNNModifiers(target: Range0Based, tokens: Array[Token])(span: Range0Based): Set[Range0Based] = {
    if (
      span.start < target.start &&
          target.end <= span.end &&
          !span.contains(tokens(target.end).getHead - 1) &&
          tokens(span.start).getPostag == "DT"
    ) {
      // target is inside span and head of span, and the first word in the span is a DT
      Set(range(span.start + 1, target.start - 1)) // strip off the determiner
    } else {
      Set()
    }
  }

  def stripPPs(sentence: Sentence)(span: Range0Based): Set[Range0Based] = {
    if (isEmpty(span.start, span.end)) {
      Set()
    } else {
      val partsOfSpeech = sentence.getTokens.asScala.map(_.getPostag)
      val idxsToSplitOn = partsOfSpeech.zipWithIndex.collect {
        case (pos, i) if PartsOfSpeechToSplitOn.contains(pos) => i
      }
      for (i <- idxsToSplitOn.toSet if i > span.start) yield {
        range(span.start, i - 1)
      }
    }
  }

  def spansMinusTarget(spans: Set[Range0Based], target: Range0Based): Set[Range0Based] = {
    spans.flatMap(spanMinusTarget(target))
  }

  def spanMinusTarget(target: Range0Based)(span: Range0Based): List[Range0Based] = {
    var result = List.empty[Range0Based]
    if (span.start < target.start && target.start <= span.end) result ::= range(span.start, target.start - 1)
    if (span.start <= target.end && target.end < span.end) result ::= range(target.end + 1, span.end)
    result
  }

  /** For each token, find its left-most and right-most descendant. */
  def getFullSubtrees(tokens: Array[Token], parents: Array[Int]): Array[Range0Based] = {
    val left = tokens.indices.toArray  // left[i] is the index of the left-most descendant of i
    val right = tokens.indices.toArray  // right[i] is the index of the right-most descendant of i
    for (
      i <- tokens.indices
      // if `doStripPunctuation`, never start or end a span with punctuation
      if !doStripPunctuation || !PunctuationPos.contains(tokens(i).getPostag);
      // walk up i's ancestors, expanding their boundaries to include i if necessary
      ancestorIdx <- ancestors(parents)(i)
    ) {
      left(ancestorIdx) = min(i, left(ancestorIdx))
      right(ancestorIdx) = max(i, right(ancestorIdx))
    }
    (left zip right).map({ case (startIdx, endIdx) => range(startIdx, endIdx) })
  }

  def ancestors(parents: Int => Int)(i: Int): Iterator[Int] = Iterator.iterate(i)(parents).drop(1).takeWhile(_ >= 0)

  def contiguousSubspans(parents: Array[Int], subtrees: Array[Range0Based]): Array[Range0Based] = {
    import edu.cmu.cs.lti.ark.util.TraversableOps._
    def belongs(span: Range, head: Int)(token: Int): Boolean = token == head || span.contains(parents(token))
    for (
      (subTree, i) <- subtrees.zipWithIndex;
      span = subTree.start to subTree.end;
      chunk <- span.groupRunsBy(belongs(span, i)).collect { case (true, run) => run }
    ) yield range(chunk.head, chunk.last)
  }

  /**
   * Heuristics to try to recover finer-grained constituents when a node has descendants on both sides.
   * Originally from Johansson and Nugues 2007
   */
  def getHalfTrees(nodes: Array[Token],
                          subtrees: Array[Range0Based]): Set[Range0Based] = {
    val result = m.Set.empty[Range0Based]
    for (
      (subtree, i) <- subtrees.zipWithIndex
      if subtree.start < i && subtree.end > i // node has both left and right children
    ) {
      if (subtrees.contains(range(subtree.start, i - 1)) && // node has exactly one left child
          // never leave a single determiner or adj dangling
          !(i - 1 == subtree.start && NonBreakingLeftConstituentPos.contains(nodes(i - 1).getPostag))) {
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
