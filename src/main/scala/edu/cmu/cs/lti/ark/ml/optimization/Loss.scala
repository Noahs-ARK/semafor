package edu.cmu.cs.lti.ark.ml.optimization

import breeze.linalg.{SparseVector, Vector => Vec}
import edu.cmu.cs.lti.ark.ml.FeatureVector

import scala.math._


trait SubDifferentiableLoss[T] {
  def lossAndGradient(weights: Vec[Double])(example: T): (Double, Vec[Double])
}

object HingeLoss extends SubDifferentiableLoss[(Array[FeatureVector], Int)] {
  override def lossAndGradient(weights: Vec[Double])
                              (example: (Array[FeatureVector], Int)): (Double, Vec[Double]) = {
    val (featsByLabel, goldLabelIdx) = example
    // cost-augmented decoding
    val scoresByLabel: Array[Double] = featsByLabel.zipWithIndex.map { case (feats, roleIdx) =>
      val cost = if (roleIdx == goldLabelIdx) 0 else 1
      (feats dot weights) + cost
    }
    val (predictedScore, predictedLabelIdx) = scoresByLabel.zipWithIndex.maxBy(_._1)
    if (predictedLabelIdx == goldLabelIdx) {
      (0, SparseVector.zeros(weights.length))
    } else {
      val loss = predictedScore - scoresByLabel(goldLabelIdx)
      val gradient = featsByLabel(predictedLabelIdx) - featsByLabel(goldLabelIdx)
      (loss, gradient)
    }
  }
}

object LogLoss extends SubDifferentiableLoss[(Array[FeatureVector], Int)] {
  override def lossAndGradient(weights: Vec[Double])
                              (example: (Array[FeatureVector], Int)): (Double, Vec[Double]) = {
    val (featsByLabel, goldLabelIdx) = example
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
