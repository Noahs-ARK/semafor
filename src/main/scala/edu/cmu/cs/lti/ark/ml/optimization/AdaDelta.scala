package edu.cmu.cs.lti.ark.ml.optimization

import breeze.linalg.{DenseVector, Vector => Vec}

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
      for (i <- (0 until weights.length).par) {
        val g = gradient(i)
        if (g != 0.0) {
          val oldAvgSqDelta = avgSquaredDelta(i)
          val newAvgSqGrad = decayingAvg(avgSquaredGradient(i), g * g)
          avgSquaredGradient(i) = newAvgSqGrad
          val delta = (smoothSqrt(oldAvgSqDelta) / smoothSqrt(newAvgSqGrad)) * g
          avgSquaredDelta(i) = decayingAvg(oldAvgSqDelta, delta * delta)
          weights(i) = weights(i) - delta
        }
      }
      this
    }
  }
}


case class MiniBatch[T](trainingData: IndexedSeq[T],
                        lossFn: SubDifferentiableLoss[T],
                        lambda: Double,
                        batchSize: Int,
                        numThreads: Int) {

  private[this] val n = trainingData.length
//  private[this] val taskSupport = new ThreadPoolTaskSupport(
//    new ThreadPoolExecutor(numThreads, numThreads, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue[Runnable])
//  )
  private[this] val batchRecip = 1.0 / batchSize

  private def l2(weights: Vec[Double]): (Double, Vec[Double]) = {
    val regLoss = (.5 * lambda) * (0 until weights.length).par.map({i => val w = weights(i); w * w}).sum
    val regGradient = weights * lambda
    (regLoss, regGradient)
  }

  private val addLossAndGradient: ((Double, Vec[Double]), (Double, Vec[Double])) => (Double, Vec[Double]) = {
    case ((la, ga), (lb, gb)) => (la + lb, ga + gb)
  }

  /**
   * Returns an (infinite) iterator of (loss, weights, step-size) triples.
   * Weights get clobbered as you iterate, so make copies if you want to keep them.
   */
  def optimizationPath(initialWeights: Vec[Double]): Iterator[(Double, Vec[Double])] = {
    var state = AdaDelta().start(initialWeights)
    lazy val startTime = System.currentTimeMillis()
    // sample batches without replacement
    for (iteration <- Iterator.from(0);
         (batch, batchNum) <- Random.shuffle(trainingData).grouped(batchSize).zipWithIndex
    ) yield {
      val weights = state.weights
      // add regularization for each batch
      // TODO: use proximal step instead of including reg in gradient
      val (regLoss, regGradient) = l2(weights)
      val parallelBatch = batch.par //.grouped(batchSize / numThreads).toParArray
      // compute gradients in parallel
//      parallelBatch.tasksupport = taskSupport
      var (loss, gradient) = parallelBatch.map(
//        _.map(
          lossFn.lossAndGradient(weights)).reduce(addLossAndGradient)
//      ).reduce(addLossAndGradient)
      // scale to per-instance
      loss = loss * batchRecip + regLoss
//      val avgStepSize = norm(state.avgSquaredDelta, 2)
      val partialIt = iteration + (batchNum.toDouble * batchSize / n)
      val time = (System.currentTimeMillis() - startTime) / 1000.0
      System.err.println(f"i: $partialIt%.3f,  loss: $loss%.5f,  regLoss: $regLoss%.5f,  time:$time%.2fs") //avgStepSize: $avgStepSize%.5f,
      // i think it's important for regGradient to be on the left here,
      // so the result is a DenseVector?
      state = state.step(regGradient + (gradient * batchRecip))
      (loss, state.weights)
    }
  }
}
