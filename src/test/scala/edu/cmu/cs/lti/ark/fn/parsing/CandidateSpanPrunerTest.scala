package edu.cmu.cs.lti.ark.fn.parsing

import edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec
import edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec._
import edu.cmu.cs.lti.ark.fn.parsing.CandidateSpanPruner.{EmptySpan, range}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

/** @author sthomson@cs.cmu.edu */
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

  it should "strip DT and target from noun targets" in {
    val inputConll =
      """1	None	none	NN	NN	_	6	nsubjpass	_	_
        |2	of	of	IN	IN	_	1	prep	_	_
        |3	the	the	DT	DT	_	4	det	_	_
        |4	workers	worker	NN	NNS	_	2	pobj	_	_
        |5	was	be	VB	VBD	_	6	auxpass	_	_
        |6	injured	injure	VB	VBN	_	0	root	_	_
        |7	in	in	IN	IN	_	6	prep	_	_
        |8	the	the	DT	DT	_	9	det	_	_
        |9	incident	incident	NN	NN	_	7	pobj	_	_
        |10	,	,	,	,	_	9	punct	_	_
        |11	the	the	DT	DT	_	12	det	_	_
        |12	second	second	NN	NN	_	9	appos	_	_
        |13	at	at	IN	IN	_	12	prep	_	_
        |14	the	the	DT	DT	_	18	det	_	_
        |15	Edgewood	edgewood	NN	NNP	_	18	nn	_	_
        |16	Chemical	chemical	NN	NNP	_	18	nn	_	_
        |17	Biological	biological	NN	NNP	_	18	nn	_	_
        |18	Center	center	NN	NNP	_	13	pobj	_	_
        |19	,	,	,	,	_	18	punct	_	_
        |20	a	a	DT	DT	_	27	det	_	_
        |21	1.5	1.5	CD	CD	_	22	number	_	_
        |22	million	million	CD	CD	_	24	dep	_	_
        |23	-	-	:	:	_	24	punct	_	_
        |24	square	square	NN	NN	_	18	appos	_	_
        |25	-	-	:	:	_	24	punct	_	_
        |26	foot	foot	NN	NN	_	27	nn	_	_
        |27	research	research	NN	NN	_	24	dep	_	_
        |28	and	and	CC	CC	_	27	cc	_	_
        |29	engineering	engineering	NN	NN	_	30	nn	_	_
        |30	facility	facility	NN	NN	_	27	conj	_	_
        |31	within	within	IN	IN	_	30	prep	_	_
        |32	APG	apg	NN	NNP	_	31	pobj	_	_
        |33	for	for	IN	IN	_	32	prep	_	_
        |34	chemical	chemical	NN	NN	_	33	pobj	_	_
        |35	and	and	CC	CC	_	34	cc	_	_
        |36	biological	biological	JJ	JJ	_	37	amod	_	_
        |37	defense	defense	NN	NN	_	34	conj	_	_
        |38	.	.	.	.	_	6	punct	_	_""".stripMargin
    val inputSentence = SentenceCodec.ConllCodec.decode(inputConll)
    val target = range(17, 17) // "Center"
    val result = CandidateSpanPruner(doFindNNModifiers = true).candidateSpans(inputSentence, target)
    val expectedModifierSpan = range(14, 16) // "Edgewood Chemical Biological"
    result should contain (expectedModifierSpan)
  }

  "CandidateSpanPruner.groupContiguous" should "group contiguous descendants" in {
    val parents = Array(2, 2, -1, 1) // non-projective
    val subtrees = Array(range(0, 0), range(1, 3), range(0, 3), range(3, 3))
    val result = prunerWithoutPunct.contiguousSubspans(parents, subtrees)
    result should equal (Array(range(0, 0), range(1, 1), range(3, 3), range(0, 3), range(3, 3)))
  }
}
