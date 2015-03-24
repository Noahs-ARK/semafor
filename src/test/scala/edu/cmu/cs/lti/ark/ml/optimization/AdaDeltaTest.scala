package edu.cmu.cs.lti.ark.ml.optimization

import breeze.linalg.DenseVector
import org.scalatest.{Matchers, FlatSpec}


class AdaDeltaTest extends FlatSpec with Matchers {
  val weights0 = DenseVector.zeros[Double](4)
  val gradient1 = DenseVector(0.3, -2.5, 1.2, -0.2)
  val gradient2 = DenseVector(0.5, -0.2, -2.5, 1.2)

  "AdaDelta.State.step" should "move each component in the negative gradient direction" in {
    var state = AdaDelta().start(weights0)
    state = state.step(gradient1)
    val weights1 = state.weights.copy
    for ((diff, g) <- (weights1 - weights0).valuesIterator zip gradient1.valuesIterator) {
      (diff * g < 0) should be (true)
    }
    state = state.step(gradient2)
    val weights2 = state.weights.copy
    for ((diff, g) <- (weights2 - weights1).valuesIterator zip gradient2.valuesIterator) {
      (diff * g < 0) should be (true)
    }
  }
}
