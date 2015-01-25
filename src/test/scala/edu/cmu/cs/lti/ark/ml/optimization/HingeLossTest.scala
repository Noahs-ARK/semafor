package edu.cmu.cs.lti.ark.ml.optimization

import breeze.linalg.SparseVector
import edu.cmu.cs.lti.ark.ml.{MultiClassTrainingExample, FeatureVector}
import org.scalatest.{FlatSpec, Matchers}
import scala.math.max

class HingeLossTest extends FlatSpec with Matchers {
  val featsA = FeatureVector(Array(2), 5)
  val featsB = FeatureVector(Array(3), 5)

  "HingeLoss" should "be positive when margin is less than cost" in {
    val margin = 0.9
    val expected = SparseVector(Array(0.0, 0.0, 1.0, -1.0, 0.0))
    val (loss, grad) = HingeLoss.lossAndGradient(featsB * margin)(MultiClassTrainingExample(Array(featsA, featsB), 1))
    loss should be (max(1 - margin, 0))
    grad should be (expected)
  }

  it should "be zero when margin is greater than cost" in {
    val margin = 1.1
    val expected = SparseVector.zeros[Double](5)
    val (loss, grad) = HingeLoss.lossAndGradient(featsB * margin)(MultiClassTrainingExample(Array(featsA, featsB), 1))
    loss should be (max(1 - margin, 0))
    grad should be (expected)
  }
}
