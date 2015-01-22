package edu.cmu.cs.lti.ark.fn.parsing

import java.io.{FileInputStream, FileOutputStream, PrintStream}
import java.util
import java.util.Scanner

import breeze.linalg.{DenseVector, SparseVector, Vector => Vec}
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions
import edu.cmu.cs.lti.ark.ml.FeatureVector
import edu.cmu.cs.lti.ark.ml.optimization._
import edu.cmu.cs.lti.ark.util.SerializedObjects._
import resource.managed

import scala.collection.Iterator.continually
import scala.collection.JavaConverters._

/**
 * command-line arguments as follows:
 * frameFeaturesCacheFile: path to file containing a serialized cache of all of the features
 *   extracted from the training data
 * alphabetFile: path to file containing the alphabet
 * lambda: L2 regularization hyperparameter
 * numThreads: the number of parallel threads to run while optimizing
 * modelFile: path to output file to write resulting model to. intermediate models will be written to
 *   modelFile + "_" + i
 */
object TrainArgIdApp extends App {
  val opts: FNModelOptions = new FNModelOptions(args)
  val modelFile: String = opts.modelFile.get
  val alphabetFile: String = opts.alphabetFile.get
  // Read the size of the model from the first line of `alphabetFile`.
  private val numFeatures: Int = {
    managed(new Scanner(new FileInputStream(alphabetFile))).acquireAndGet(_.nextInt + 1)
  }
  val frameFeaturesCacheFile: String = opts.frameFeaturesCacheFile.get
  val lambda: Double = opts.lambda.get
  val numThreads: Int = opts.numThreads.get
  val batchSize: Int = opts.batchSize.get()
  System.err.println("Reading cached training data")
  val frameFeaturesList: util.ArrayList[FrameFeatures] = readObject(frameFeaturesCacheFile)
  System.err.println("Done reading cached training data")
  TrainArgId(
    modelFile,
    numFeatures,
    frameFeaturesList.asScala.toArray,
    HingeLoss,
    lambda,
    numThreads
  ).runAdaDelta()
}

/** Calculates the loss and gradient for all spans of a given frame */
case class FrameLoss(lossType: SubDifferentiableLoss[(Array[FeatureVector], Int)])
    extends SubDifferentiableLoss[FrameFeatures] {

  override def lossAndGradient(weights: Vec[Double])
                              (example: FrameFeatures): (Double, Vec[Double]) = {
    val lossFn = lossType.lossAndGradient(weights)(_)
    val numFeats = weights.length
    val features = example.fElementSpansAndFeatures.asScala
    val goldLabels = example.goldSpanIdxs.asScala
    var loss = 0.0
    val gradient = SparseVector.zeros[Double](numFeats)
    for ((featsByLabelNoBias, goldRoleIdx) <- features zip goldLabels) {
      // FrameFeatures don't explicitly add the bias feature (index=0)
      val featsByLabel = featsByLabelNoBias.map(f =>
        new FeatureVector(0 +: f.features, numFeats)
      )
      val (l, g) = lossFn((featsByLabel, goldRoleIdx))
      loss += l
      gradient += g
    }
    (loss, gradient)
  }
}

case class TrainArgId(modelFile: String,
                      numFeats: Int,
                      trainingData: Array[FrameFeatures],
                      lossFn: SubDifferentiableLoss[(Array[FeatureVector], Int)],
                      lambda: Double,
                      numThreads: Int) {
//  /** all of these arrays get reused to conserve memory */
//  private final var weights: Vec[Double] = DenseVector.zeros(numFeats)
//  private final val gradient: DenseVector[Double] = DenseVector.zeros(numFeats)
//  private final val threadLosses: Array[Double] = new Array[Double](numThreads)
//  private final val threadGradients: Array[Array[Double]] = Array.ofDim[Double](numFeats, numThreads)
  private final val frameLoss = FrameLoss(lossFn)

//  /** clobbers `gradient`, `threadLosses`, `threadGradients` as a side-effect */
//  private def lossAndGradient(weights: Vec[Double],
//                              batchSize: Int = 10): (Double, Vec[Double]) = {
//    // zero out thread objs and grads
//    util.Arrays.fill(threadLosses, 0.0)
//    for (grads <- threadGradients) {
//      util.Arrays.fill(grads, 0.0)
//    }
//    // spawn threads to calculate new objs and grads
//    val threadPool = new ThreadPool(numThreads)
//    for ((start, batchNum) <- (0 until trainingData.size by batchSize).zipWithIndex) {
//      val range: Range = start until min(start + batchSize, trainingData.size)
//      threadPool.runTask(processBatch(weights, batchNum, range))
//    }
//    // wait for them to finish
//    threadPool.join()
//
//    // sum them all up and add regularization
//    val loss = threadLosses.sum + lambda * weights.map(w => w * w).sum
//    for (j <- 0 until numFeats) {
//      gradient(j) = threadGradients(j).sum + 2 * lambda * weights(j)
//    }
//    println("Finished value and gradient computation.")
//    (loss, gradient)
//  }

//  def processBatch(weights: Vec[Double],
//                   taskId: Int,
//                   indexes: Iterable[Int]): Runnable = new Runnable {
//    override def run() {
//      val threadId = taskId % numThreads
//      System.err.println(s"Processing taskId: $taskId, threadId: $threadId")
//      for (index <- indexes) {
//        val frameFeatures = trainingData(index)
//        val (loss, gradient) = frameLoss.lossAndGradient(weights)(frameFeatures)
//        threadLosses(threadId) += loss
//        for (i <- 0 until numFeats) {
//          threadGradients(i)(threadId) += gradient(i)
//        }
//      }
//      System.err.println(s"taskId $taskId end")
//    }
//  }

  def runAdaDelta(batchSize: Int = 128,
                  saveEveryKBatches: Int = 100,
                  maxSaves: Int = 30) {
    import edu.cmu.cs.lti.ark.fn.parsing.TrainArgId.writeModel
    val optimizer = MiniBatch(trainingData, frameLoss, lambda, batchSize, numThreads)
    val optimizationPath = optimizer.optimizationPath(DenseVector.zeros(numFeats))
    val everyK = continually(optimizationPath.drop(saveEveryKBatches - 1).next())
    for (((loss, weights, gradNorm), i) <- everyK.take(maxSaves).zipWithIndex) {
      writeModel(weights, modelFile + "_" + i)
    }
  }

//  def runLbfgs() {
//    import edu.cmu.cs.lti.ark.fn.parsing.TrainArgId.writeModel
//    val diagco = new Array[Double](numFeats)
//    val iprint = Array(if (Lbfgs.DEBUG) 1 else -1, 0)
//    val iflag = Array(0)
//    var iteration = 0
//    do {
//      System.err.println("Starting iteration:" + iteration)
//      val (loss, gradients) = lossAndGradient(weights)
//      val gradNorm = gradNorm(DenseVector(gradients))
//      System.err.println(f"iteration: $iteration%4s,\tloss: $loss%20s,\tgradNorm: $gradNorm%20s")
//      LBFGS.lbfgs(
//        numFeats,
//        Lbfgs.NUM_CORRECTIONS,
//        weights.toArray,
//        loss,
//        gradients.toArray,
//        false,
//        diagco,
//        iprint,
//        Lbfgs.STOPPING_THRESHOLD,
//        Lbfgs.XTOL,
//        iflag
//      )
//      System.err.println("Finished iteration:" + iteration)
//      iteration += 1
//      if (iteration % Lbfgs.SAVE_EVERY_K == 0) writeModel(weights, modelFile + "_" + iteration)
//    } while (iteration <= Lbfgs.MAX_ITERATIONS && iflag(0) != 0)
//    writeModel(weights, modelFile)
//  }
}
object TrainArgId {
  def writeModel(weights: Vec[Double], modelFile: String) {
    for (ps <- managed(new PrintStream(new FileOutputStream(modelFile)))) {
      System.err.println(s"Writing model to $modelFile")
      weights.foreach(ps.println)
      System.err.println(s"Finished writing model $modelFile")
    }
  }
}
