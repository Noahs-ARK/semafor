package edu.cmu.cs.lti.ark.fn.parsing

import edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec._
import edu.cmu.cs.lti.ark.fn.parsing.CandidateSpanPruner.range
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

/**
 * @author sthomson@cs.cmu.edu
 */
class CandidateSpanPrunerTest extends FlatSpec with Matchers {
  private val maltLine = "My/PRP$/2/NMOD kitchen/NN/5/SBJ no/RB/5/ADV longer/RB/3/AMOD smells/VBZ/0/ROOT ././5/P"
  private val sentence = MaltCodec.decode(maltLine)
  private val frameElementsLine = "0\t1.0\t1\tTemporal_collocation\tno.r\t2_3\tno longer\t0\n"
  private val dataPointWithElements = new DataPointWithFrameElements(sentence, frameElementsLine)
  private val pruner = new CandidateSpanPruner()
  private val spanList = pruner.candidateSpans(dataPointWithElements.getParse).asScala.toList

  "CandidateSpanPruner.candidateSpans" should "include the null span" in {
    spanList should contain (CandidateSpanPruner.EMPTY_SPAN)
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

  "CandidateSpanPruner.groupContiguous" should "group contiguous descendants" in {
    val result = CandidateSpanPruner.groupContiguous[Int](0 to 12, _ % 3 != 0)
    result should equal (List(1 to 2, 4 to 5, 7 to 8, 10 to 11))
  }
}
