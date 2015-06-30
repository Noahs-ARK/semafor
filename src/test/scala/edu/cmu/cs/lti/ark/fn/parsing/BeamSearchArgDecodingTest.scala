package edu.cmu.cs.lti.ark.fn.parsing

import com.google.common.collect.Lists
import edu.cmu.cs.lti.ark.fn.parsing.CandidateSpanPruner.EMPTY_SPAN
import edu.cmu.cs.lti.ark.util.ds.{Range0Based, Scored}
import org.scalatest.{Matchers, FlatSpec}

import scala.collection.mutable

class BeamSearchArgDecodingTest extends FlatSpec with Matchers {

  "BeamSearchArgDecoding" should "getPredictions" in {
    val model = Model(Array(0.1, 1.0, -0.01), FeatureIndex(mutable.LinkedHashMap()))
    val decoder = BeamSearchArgDecoding(model, 5)
    val roleA = "Test_Role_A"
    val roleB = "Test_Role_B"
    val span0 = new Range0Based(0, 0)
    val span1 = new Range0Based(1, 1)
    val span01 = new Range0Based(0, 1)
    val input =
      new FrameFeatures(
        "Test_Frame",
        0,
        0,
        Lists.newArrayList(roleA, roleB),
        Lists.newArrayList(
          Array(
            SpanAndFeatures(EMPTY_SPAN, Array()),     //  0
            SpanAndFeatures(span0, Array(1)),         //  1
            SpanAndFeatures(span1, Array(0, 2)),      //  0.09
            SpanAndFeatures(span01, Array(1, 2))      //  0.99
          ),
          Array(
            SpanAndFeatures(EMPTY_SPAN, Array(2)),    // -0.01
            SpanAndFeatures(span0, Array(0, 2)),      //  0.09
            SpanAndFeatures(span1, Array(0, 1)),      //  1.1
            SpanAndFeatures(span01, Array(0, 1, 2))   //  1.09
          )
        )
      )
    val expected = List(
      Scored.scored(RoleAssignment(Map(roleA -> span0, roleB -> span1), Map()), 2.1),
      Scored.scored(RoleAssignment(Map(roleB -> span1), Map()), 1.1),
      Scored.scored(RoleAssignment(Map(roleB -> span01), Map()), 1.09),
      Scored.scored(RoleAssignment(Map(roleA -> span0), Map()), 0.99),
      Scored.scored(RoleAssignment(Map(roleA -> span01), Map()), 0.98)
    )
    val result = decoder.getPredictions(input).toList
    println(result)
    result.size should equal (expected.size)
    for ((res, exp) <- result zip expected) {
      res.value should equal (exp.value)
      res.score should equal (exp.score)
    }
  }
}
