package edu.cmu.cs.lti.ark.fn.parsing

import edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec._
import edu.cmu.cs.lti.ark.fn.parsing.CandidateSpanPruner.createSpanRange
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements
import edu.cmu.cs.lti.ark.util.IntRanges._
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
    import scala.collection.JavaConversions._
    for (i <- xrange(sentence.size)) {
      spanList should contain (createSpanRange(i, i))
    }
  }

  it should "include constituents" in {
    val expectedSpans = List(
      createSpanRange(0, 1),
      createSpanRange(2, 3),
      createSpanRange(0, 5)
    )
    for (expectedSpan <- expectedSpans) {
      spanList should contain (expectedSpan)
    }
  }
}
