package edu.cmu.cs.lti.ark.ml.optimization

import breeze.linalg.{DenseVector, Vector => Vec}

import scala.concurrent.forkjoin.ForkJoinPool
import scala.math.sqrt
import scala.util.Random.nextInt
import scala.collection.parallel.ForkJoinTaskSupport


/**
 * An implementation of the ADADELTA algorithm as described in
 * Zeiler, Matthew D., "ADADELTA: An Adaptive Learning Rate Method"
 * http://arxiv.org/abs/1212.5701
 *
 * @param decay the exponential rate to decay running averages at. 0 < decay < 1
 *              (1 - \rho in the paper)
 * @param smoothing a small positive constant to "better condition the denominator"
 *                  (\epsilon in the paper)
 */
case class AdaDelta(decay: Double = 0.05, smoothing: Double = 1e-6) {

  @inline private[this] val smoothSqrt: Double => Double = x => sqrt(x + smoothing)

  private[this] val oneMinusDecay = 1 - decay
  @inline private[this] def decayingAvg(oldAvg: Double, newVal: Double): Double = {
    oneMinusDecay * oldAvg + decay * newVal
  }

  def start(initialWeights: Vec[Double]): State = {
    val size = initialWeights.length
    new State(initialWeights.copy, DenseVector.zeros(size), DenseVector.zeros(size))
  }

  /** The (mutable) state of a run of AdaDelta */
  class State(val weights: Vec[Double],
              val avgSquaredGradient: Vec[Double],
              val avgSquaredDelta: Vec[Double]) {
    /** Destructively modifies `weights`, `avgSquaredGradient`, `avgSquaredDelta` */
    def step(gradient: Vec[Double]): State = {
      for (i <- 0 until weights.length) {
        val g = gradient(i)
        avgSquaredGradient(i) = decayingAvg(avgSquaredGradient(i), g * g)
        // NB: uses `avgSquaredDelta` from previous step,
        // but `avgSquaredGradient` from current step.
        val delta = (smoothSqrt(avgSquaredDelta(i)) / smoothSqrt(avgSquaredGradient(i))) * g
        avgSquaredDelta(i) = decayingAvg(avgSquaredDelta(i), delta * delta)
        weights(i) = weights(i) - delta
      }
      this
    }
  }
}


case class MiniBatch[T](trainingData: IndexedSeq[T],
                        lossFn: SubDifferentiableLoss[T],
                        l2: Double,
                        batchSize: Int,
                        numThreads: Int) {

  private[this] val n = trainingData.length
  private[this] val lambda = l2 / n
  private[this] val forkJoin = new ForkJoinTaskSupport(new ForkJoinPool(numThreads))
  private[this] val batchRecip = 1.0 / batchSize

  /**
   * Returns an (infinite) iterator of (loss, weights, step-size) triples.
   * Weights get clobbered as you iterate, so make copies if you want to keep them.
   */
  def optimizationPath(initialWeights: Vec[Double]): Iterator[(Double, Vec[Double], Double)] = {
    var state = AdaDelta().start(initialWeights)
    // sample batches with replacement
    val shuffled = Iterator.continually(nextInt(n)).map(trainingData)
    for ((batch, batchNum) <- shuffled.grouped(batchSize).zipWithIndex) yield {
      val weights = state.weights
      // add regularization for each batch
      var loss = lambda * (weights dot weights)
      val gradient = weights * (2 * lambda)
      // compute gradients in parallel
      val batchPar = batch.par
      batchPar.tasksupport = forkJoin
      val lossesAndGrads = batchPar.map(lossFn.lossAndGradient(weights))
      for ((l, grad) <- lossesAndGrads.seq) {
        loss += l
        gradient += grad
      }
      // scale to per-instance
      loss *= batchRecip
      gradient *= batchRecip
      val avgStepSize = sqrt(state.avgSquaredDelta.sum)
      System.err.println(f"i: ${batchNum * batchSize}%6s,\tloss: $loss%20s,\tavgStepSize: $avgStepSize%20s")
      state = state.step(gradient)
      (loss, state.weights, avgStepSize)
    }
  }
}
