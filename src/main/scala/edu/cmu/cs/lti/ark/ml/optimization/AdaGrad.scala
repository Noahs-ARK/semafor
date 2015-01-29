package edu.cmu.cs.lti.ark.ml.optimization

import breeze.linalg.{DenseVector, Vector => Vec}
import edu.cmu.cs.lti.ark.ml.Vectors._

import scala.math.sqrt


/**
 * An implementation of the AdaGrad algorithm as described in
 * Duchi, John, et al. "Adaptive Subgradient Methods for Online Learning and Stochastic Optimization"
 * http://web.stanford.edu/~jduchi/projects/DuchiHaSi10.html
 *
 * Modified to add L1 + L2 regularization, implemented with
 * proximal steps.
 *
 * @param initialRate the initial learning rate (\eta in the paper). must be positive
 * @param smoothing a small positive constant to
 *                  (\epsilon in the paper)
 * @param l1Strength the L1 regularization strength
 */
case class AdaGrad(initialRate: Double = 1.0,
                   smoothing: Double = 1e-6,
                   l1Strength: Double = 1e-5,
                   l2Strength: Double = 0.0) {
  
  /** The starting state for a run of AdaDelta */
  def start(initialWeights: Vec[Double]): State = {
    val size = initialWeights.length
    new State(initialWeights, DenseVector.zeros(size))
  }

  /** The state of a run of AdaGrad */
  case class State(weights: Vec[Double],
                   totalSqGrad: Vec[Double]) {
    /**
     * Takes a step using per-dimension learning rates, and returns the new
     * State.
     * `gradient` should not include regularization;
     * regularization is handled specially, by applying a proximal mapping.
     * Destructively modifies `weights`, `avgSquaredGradient`, `avgSquaredDelta`,
     * and returns the new State.
     *
     * TODO: Modifying state in place is in order to reduce memory usage. Is it necessary?
     */
    def step(gradient: Vec[Double]): State = {
      val denseGrad = toDenseVectorPar(gradient)
      for (i <- (0 until gradient.length).par) {
        val g = denseGrad(i)
        totalSqGrad(i) = totalSqGrad(i) + g * g
        val rate = initialRate / sqrt(totalSqGrad(i) + smoothing)
        weights(i) = proximalStep(rate)(weights(i) - rate * g)
      }
      this
    }
  }

  @inline private def proximalStep(rate: Double): Double => Double = {
    ProximalMappings.elasticNet(rate * l1Strength, rate * l2Strength)
  }
}
