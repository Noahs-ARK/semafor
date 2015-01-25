package edu.cmu.cs.lti.ark.ml.optimization

import breeze.linalg.{DenseVector, SparseVector, Vector => Vec}

import scala.collection.parallel.ForkJoinTaskSupport
import scala.concurrent.forkjoin.ForkJoinPool
import scala.math.sqrt
import scala.util.Random


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
      var i = 0
      while (i < weights.length) {
        val g = gradient(i)
        avgSquaredGradient(i) = decayingAvg(avgSquaredGradient(i), g * g)
        // NB: uses `avgSquaredDelta` from previous step,
        // but `avgSquaredGradient` from current step.
        val delta = (smoothSqrt(avgSquaredDelta(i)) / smoothSqrt(avgSquaredGradient(i))) * g
        avgSquaredDelta(i) = decayingAvg(avgSquaredDelta(i), delta * delta)
        weights(i) = weights(i) - delta
	i += 1
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
  private[this] val lambda = l2 * batchSize / n
  private[this] val forkJoin = new ForkJoinTaskSupport(new ForkJoinPool(numThreads))
  private[this] val batchRecip = 1.0 / batchSize

  /**
   * Returns an (infinite) iterator of (loss, weights, step-size) triples.
   * Weights get clobbered as you iterate, so make copies if you want to keep them.
   */
  def optimizationPath(initialWeights: Vec[Double]): Iterator[(Double, Vec[Double], Double)] = {
    var state = AdaDelta().start(initialWeights)
    // sample batches without replacement
    // sample batches without replacement
    for (iteration <- Iterator.from(0);
         shuffled = Random.shuffle(trainingData);
         (batch, batchNum) <- shuffled.grouped(batchSize).zipWithIndex) yield {
//    for (iteration <- Iterator.from(0);
//         shuffled = Random.shuffle(trainingData).iterator;
//         (batch, batchNum) <- shuffled.grouped(batchSize).zipWithIndex) yield {
      val weights = state.weights
      // add regularization for each batch (don't regularize bias)
      val regLoss = lambda * (weights dot weights) //- (weights(biasFeatureIdx) * weights(biasFeatureIdx)))
      val regGradient = weights * (2 * lambda)
//      gradient(biasFeatureIdx) = 0.0
      // compute gradients in parallel
      val batchPar = batch.par
      batchPar.tasksupport = forkJoin
      val lossesAndGrads = batchPar.map(lossFn.lossAndGradient(weights))
//      var loss = 0.0
//      val gradient = SparseVector.zeros[Double](weights.length)
//      for ((l, grad) <- lossesAndGrads.seq) {
//        loss += l
//        gradient += grad
//      }
      var (loss, gradient) = lossesAndGrads.fold((0.0, SparseVector.zeros[Double](weights.length)))({
	      case ((la, ga), (lb, gb)) => (la + lb, ga + gb)
      })
      // scale to per-instance
      loss *= batchRecip
      gradient *= batchRecip
      val avgStepSize = sqrt(state.avgSquaredDelta.sum)
//      System.err.println(f"i: ${batchNum * batchSize}%6s,\tloss: $loss%20s,\tavgStepSize: $avgStepSize%20s")
      val partialIt = iteration + (batchNum.toDouble * batchSize / n)
      System.err.println(f"i: $partialIt%.3f,\tloss: $loss%.5f,\tregLoss: $regLoss%.5f,\tavgStepSize: $avgStepSize%.5f") //,\tbias: ${weights(biasFeatureIdx)}%.5f
      state = state.step(gradient + regGradient)
      (loss + regLoss, state.weights, avgStepSize)
    }
  }
}
