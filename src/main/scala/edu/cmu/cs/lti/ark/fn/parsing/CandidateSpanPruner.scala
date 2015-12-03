package edu.cmu.cs.lti.ark.fn.parsing

import java.util

import edu.cmu.cs.lti.ark.fn.data.prep.formats.{Token, Sentence}
import edu.cmu.cs.lti.ark.util.ds.Range0Based

import scala.collection.JavaConverters._
import scala.collection.{mutable => m}
import scala.math.{abs, max, min}


object CandidateSpanPruner {
  val EmptySpan: Range0Based = new Range0Based(-1, -1, false)
  val NonBreakingLeftConstituentPos: Set[String] = Set("DT", "JJ")
  val PunctuationPos: Set[String] = Set(".", ",", ":", "''", "-RRB-", "-RSB-")
  val PartsOfSpeechToSplitOn: Set[String] = Set("WDT", "IN", "TO") ++ PunctuationPos
  // see Täckström et al., TACL '15
  // http://static.googleusercontent.com/media/research.google.com/en//pubs/archive/43251.pdf
  val OffensiveStanfordDependencyLabels: Set[String] = Set(
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
    doIncludeTarget = true,
    doIncludeSpansMinusTarget = true,
    doUseTackstrom = true,
    doFindNNModifiers = true,
    doIncludeContiguousSubspans = true,
    doStripPPs = true,
    maxLength = Some(20),
    maxDistance = Some(20),
    maxDepPathLength = Some(5)
  )

  /* heuristics used in SEMAFOR 2.1 */
  def baseline: CandidateSpanPruner = CandidateSpanPruner(
    doStripPunctuation = false,
    doIncludeTarget = false,
    doIncludeSpansMinusTarget = false,
    doUseTackstrom = true,
    doFindNNModifiers = false,
    doIncludeContiguousSubspans = false,
    doStripPPs = false,
    maxLength = None,
    maxDistance = None,
    maxDepPathLength = None
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
                               doIncludeTarget: Boolean,
                               doIncludeSpansMinusTarget: Boolean,
                               doUseTackstrom: Boolean,
                               doFindNNModifiers: Boolean,
                               doIncludeContiguousSubspans: Boolean,
                               doStripPPs: Boolean,
                               maxLength: Option[Int],
                               maxDistance: Option[Int],
                               maxDepPathLength: Option[Int]) {
  import CandidateSpanPruner._

  /** Calculates a set of candidate spans based on the given dependency parse. */
  def candidateSpans(sentence: Sentence,
                     targetSpan: Range0Based): util.List[Range0Based] = {
    var result = m.Set[Range0Based](EmptySpan) // always include the empty span
    val tokens: Array[Token] = sentence.getTokens.asScala.toArray
    val parents = tokens.map(_.getHead - 1)  // nodes are 1-indexed
    val deprels = tokens.map(_.getDeprel)

    if (doIncludeTarget) {
//      println("target:\n" + spanToString(targetSpan, sentence))
      result += targetSpan
    }
    // always include single tokens
    val singletons = tokens.indices.map(i => range(i, i))
//    println("singletons:\n" + singletons.map(spanToString(_, sentence)).mkString("\n"))
    result ++= singletons
    // always include full subtrees
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
    if (doUseTackstrom) {
      val subHalfTrees = getSubHalfTrees(tokens, subtrees, parents, deprels)
//      println("subHalfTrees:\n" + subHalfTrees.toSet.diff(result).map(spanToString(_, sentence)).mkString("\n"))
      result ++= subHalfTrees
    } else {
      val halfTrees = getHalfTrees(tokens, subtrees)
      //    println("halfTrees:\n" + halfTrees.toSet.diff(result).map(spanToString(_, sentence)).mkString("\n"))
      result ++= halfTrees
    }
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
    for (maxDist <- maxDistance) {
      val (ok, tooFar) = result.partition(s => dist(s, targetSpan) <= maxDist)
      //      println("tooFar:\n" + tooFar.map(spanToString(_, sentence)).mkString("\n"))
      result = ok
    }
    for (maxDepLen <- maxDepPathLength) {
      val (ok, tooFar) = result.partition(s => depLen(parents)(s, targetSpan) <= maxDepLen)
//      if (tooFar.nonEmpty) {
//        println("dep path too long:\n" + tooFar.map(spanToString(_, sentence)).mkString("\n"))
//      }
      result = ok
    }

    result.toList.asJava
  }

  // TODO: reuse code for feature extraction based on dep-path
  def depLen(parents: Int => Int)(s: Range0Based, t: Range0Based): Int = {
    if (s.isEmpty || t.isEmpty) return 0
    val sAncestors = s.iterator().asScala.map(i => ancestors(parents)(i).toVector.reverse).toVector
    val tAncestors = t.iterator().asScala.map(i => ancestors(parents)(i).toVector.reverse).toVector

    (for (sA <- sAncestors; tA <- tAncestors) yield {
      val commonPathLen = sA.zip(tA).takeWhile({ case (si, ti) => si == ti }).size
      sAncestors.size + tAncestors.size + 1 - commonPathLen
    }).min
  }

  def dist(s: Range0Based, t: Range0Based): Int = {
    (for (x <- Seq(s.start, s.end); y <- Seq(t.start, t.end)) yield abs(x - y)).min
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
    * Originally from Johansson and Nugues 2007.
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
  /**
    * Heuristics to try to recover finer-grained constituents when a node has multiple descendants on a side.
    * From Täckström et al., TACL '15
    * http://static.googleusercontent.com/media/research.google.com/en//pubs/archive/43251.pdf
    */
  def getSubHalfTrees(nodes: Array[Token],
                      subtrees: Array[Range0Based],
                      parents: Int => Int,
                      deprels: Int => String): Set[Range0Based] = {
    val result = m.Set.empty[Range0Based]
    for (
      (subtree, i) <- subtrees.zipWithIndex
      if subtree.start < i && subtree.end > i // node has both left and right children
    ) {
      val inoffensiveLeftChildren =
        (subtree.start until i).reverse
            .filter(j => parents(j) == i)
            .takeWhile(j => !OffensiveStanfordDependencyLabels.contains(deprels(j)))
      for (child <- inoffensiveLeftChildren) {
        result += range(subtrees(child).start, i)
      }
      // never leave a single determiner or adj dangling
      val start = if (NonBreakingLeftConstituentPos.contains(nodes(i - 1).getPostag)) i - 1 else i
      // include self and right children
      val inoffensiveRightChildren =
        (i + 1 to subtree.end)
            .filter(j => parents(j) == i)
            .takeWhile(j => !OffensiveStanfordDependencyLabels.contains(deprels(j)))
      for (child <- inoffensiveRightChildren) {
        result += range(start, subtrees(child).end)
      }
    }
    result.toSet
  }
}
