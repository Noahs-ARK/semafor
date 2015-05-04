package edu.cmu.cs.lti.ark.ml.optimization

import breeze.linalg.{SparseVector, Vector => Vec}
import edu.cmu.cs.lti.ark.ml.MultiClassTrainingExample

import scala.math._


trait SubDifferentiableLoss[T] {
  def lossAndGradient(weights: Vec[Double])(example: T): (Double, Vec[Double])
}

object HingeLoss extends SubDifferentiableLoss[MultiClassTrainingExample] {
  override def lossAndGradient(weights: Vec[Double])
                              (example: MultiClassTrainingExample): (Double, Vec[Double]) = {
    val MultiClassTrainingExample(featsByLabel, goldLabelIdx) = example
    // cost-augmented decoding
    val scoresByLabel: Array[Double] = featsByLabel.zipWithIndex.map { case (feats, roleIdx) =>
      val cost = if (roleIdx == goldLabelIdx) 0 else 1
      (feats dot weights) + cost
    }
    val (predictedScore, predictedLabelIdx) = scoresByLabel.zipWithIndex.maxBy(_._1)
    if (predictedLabelIdx == goldLabelIdx) {
      // predicted correct label with sufficient margin. no penalty
      (0, SparseVector.zeros(weights.length))
    } else {
      val loss = predictedScore - scoresByLabel(goldLabelIdx)
      val gradient = featsByLabel(predictedLabelIdx) - featsByLabel(goldLabelIdx)
      (loss, gradient)
    }
  }
}

object SquaredHingeLoss extends SubDifferentiableLoss[MultiClassTrainingExample] {
  override def lossAndGradient(weights: Vec[Double])
                              (example: MultiClassTrainingExample): (Double, Vec[Double]) = {
    val (loss, gradient) = HingeLoss.lossAndGradient(weights)(example)
    (loss * loss, gradient * loss)
  }
}


object LogLoss extends SubDifferentiableLoss[MultiClassTrainingExample] {
  override def lossAndGradient(weights: Vec[Double])
                              (example: MultiClassTrainingExample): (Double, Vec[Double]) = {
    val MultiClassTrainingExample(featsByLabel, goldLabelIdx) = example
    val gradient = SparseVector.zeros[Double](weights.length)
    val scoresByLabel: Array[Double] = featsByLabel.map(_.dot(weights))
    val predictedProbsByLabel = {
      val exponentiatedScores = scoresByLabel.map(exp)
      val partition = exponentiatedScores.sum
      exponentiatedScores.map(_ / partition)
    }
    for (labelIdx <- 0 until featsByLabel.length) {
      val target = if (labelIdx == goldLabelIdx) 1 else 0
      val residual = target - predictedProbsByLabel(labelIdx)
      gradient -= featsByLabel(labelIdx) * residual
    }
    (-log(predictedProbsByLabel(goldLabelIdx)), gradient)
  }
}

/**
 * Proximal mappings for some common functions
 * prox_h(w) = min_x { h(x) + 1/2 ||x - w||^2 }
 */
object ProximalMappings {
  /** Proximal mapping for L1 (a.k.a. "soft-threshold") */
  @inline def lasso(lambda: Double)(x: Double): Double = {
    if (x > lambda) {
      x - lambda
    } else if (x < -lambda) {
      x + lambda
    } else {
      0.0
    }
  }

  /** Proximal mapping for L2 */
  @inline def ridge(lambda: Double)(x: Double): Double = x / (lambda + 1.0)

  /** Proximal mapping for L1 + L2 */
  @inline def elasticNet(lambda1: Double, lambda2: Double)(x: Double): Double = {
    ridge(lambda2)(lasso(lambda1)(x))
  }
}
