package edu.cmu.cs.lti.ark.fn.parsing

import edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec._
import edu.cmu.cs.lti.ark.fn.parsing.CandidateSpanPruner.{EmptySpan, range}
import edu.cmu.cs.lti.ark.util.ds.Range0Based
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

/**
 * @author sthomson@cs.cmu.edu
 */
class CandidateSpanPrunerTest extends FlatSpec with Matchers {
  private val maltLine = "My/PRP$/2/NMOD kitchen/NN/5/SBJ no/RB/5/ADV longer/RB/3/AMOD smells/VBZ/0/ROOT ././5/P"
  private val sentence = MaltCodec.decode(maltLine)
  private val prunerWithPunct = CandidateSpanPruner(doStripPunctuation = false)
  private val prunerWithoutPunct = CandidateSpanPruner(doStripPunctuation = true)
  private lazy val spanList = prunerWithPunct.candidateSpans(sentence, EmptySpan).asScala.toList

  "CandidateSpanPruner.candidateSpans" should "include the null span" in {
    spanList should contain (EmptySpan)
  }

  it should "include singletons" in {
    for (i <- 0 until sentence.size) {
      spanList should contain (range(i, i))
    }
  }

  it should "include constituents" in {
    val expectedSpans = List(
      range(0, 1),
      range(2, 3),
      range(0, 5)
    )
    for (expectedSpan <- expectedSpans) {
      spanList should contain (expectedSpan)
    }
  }

  it should "strip punctuation" in {
    val spans = prunerWithoutPunct.candidateSpans(sentence, EmptySpan).asScala.toList
    val expectedSpans = List(
      range(0, 1),
      range(2, 3),
      range(0, 4)
    )
    for (expectedSpan <- expectedSpans) {
      spans should contain (expectedSpan)
    }
    spans should not contain range(0, 5)
  }

  it should "remove the target" in {
    val before = Set(range(0, 5), range(1, 3), range(3, 6))
    val target = range(2, 3)
    val expected = Set(range(0, 1), range(4, 5), range(1, 1), range(4, 6))
    val result = prunerWithoutPunct.spansMinusTarget(before, target)
    result should equal (expected)
  }

  it should "strip PPs, commas and WDTs" in {
    val sentenceWithPPs = "My/,/2/NMOD kitchen/NN/5/SBJ no/IN/5/ADV longer/RB/3/AMOD smells/WDT/0/ROOT ././5/P"
    val sentence = MaltCodec.decode(sentenceWithPPs)
    val span = range(0, 5)
    val result = prunerWithoutPunct.stripPPs(sentence)(span)
    val expected = Set(range(0, 4), range(0, 3), range(0, 1))
    result should equal (expected)
  }

  "CandidateSpanPruner.groupContiguous" should "group contiguous descendants" in {
    val parents = Array(2, 2, -1, 1) // non-projective
    val subtrees = Array(range(0, 0), range(1, 3), range(0, 3), range(3, 3))
    val result = prunerWithoutPunct.contiguousSubspans(parents, subtrees)
    result should equal (Array(range(0, 0), range(1, 1), range(3, 3), range(0, 3), range(3, 3)))
  }
}
