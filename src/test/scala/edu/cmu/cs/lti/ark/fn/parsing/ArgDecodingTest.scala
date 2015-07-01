package edu.cmu.cs.lti.ark.fn.parsing

import edu.cmu.cs.lti.ark.fn.parsing.CandidateSpanPruner.EMPTY_SPAN
import edu.cmu.cs.lti.ark.util.ds.Range0Based
import edu.cmu.cs.lti.ark.util.ds.Scored.scored
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable

class ArgDecodingTest extends FlatSpec with Matchers {
  val model = Model(Array(0.1, 1.0, -0.01), FeatureIndex(mutable.LinkedHashMap()))
  val roleA = "Test_Role_A"
  val roleB = "Test_Role_B"
  val span0 = new Range0Based(0, 0)
  val span1 = new Range0Based(1, 1)
  val span01 = new Range0Based(0, 1)
  val input = Map[String, Map[Range0Based, Double]](
    roleA -> Map(
      EMPTY_SPAN -> 0 ,
      span0 -> 1 ,
      span1 -> 0.09 ,
      span01 -> 0.99
    ),
    roleB -> Map(
      EMPTY_SPAN -> -0.01 ,
      span0 -> 0.09 ,
      span1 -> 1.1 ,
      span01 -> 1.09
    )
  )
  val expected = List(
    scored(RoleAssignment(Map(roleA -> span0, roleB -> span1), Map()), 2.1),
    scored(RoleAssignment(Map(roleB -> span1), Map()), 1.1),
    scored(RoleAssignment(Map(roleB -> span01), Map()), 1.09),
    scored(RoleAssignment(Map(roleA -> span0), Map()), 0.99),
    scored(RoleAssignment(Map(roleA -> span01), Map()), 0.98)
  )

  //    val input =
  //      new FrameFeatures(
  //        "Test_Frame",
  //        0,
  //        0,
  //        Lists.newArrayList(roleA, roleB),
  //        Lists.newArrayList(
  //          Array(
  //            SpanAndFeatures(EMPTY_SPAN, Array()),     //  0
  //            SpanAndFeatures(span0, Array(1)),         //  1
  //            SpanAndFeatures(span1, Array(0, 2)),      //  0.09
  //            SpanAndFeatures(span01, Array(1, 2))      //  0.99
  //          ),
  //          Array(
  //            SpanAndFeatures(EMPTY_SPAN, Array(2)),    // -0.01
  //            SpanAndFeatures(span0, Array(0, 2)),      //  0.09
  //            SpanAndFeatures(span1, Array(0, 1)),      //  1.1
  //            SpanAndFeatures(span01, Array(0, 1, 2))   //  1.09
  //          )
  //        )
  //      )

  "BeamSearchArgDecoding" should "decode a fixture correctly" in {
    val decoder = BeamSearchArgDecoding(model, 5)
    val result = decoder.decode(input).toList
    result.size should equal (expected.size)
    for ((res, exp) <- result zip expected) {
      res.value should equal (exp.value)
      res.score should equal (exp.score)
    }
  }

  "SemiMarkovArgDecoding" should "1-best decode a fixture correctly" in {
    val decoder = SemiMarkovArgDecoding(model)
    val result = decoder.decode(input).toList
    result.size should equal (1)
    for ((res, exp) <- result zip expected) {
      res.value should equal (exp.value)
      res.score should equal (exp.score)
    }
  }
}
