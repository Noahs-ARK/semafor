package edu.cmu.cs.lti.ark.fn.parsing

import java.io._
import java.util.Scanner

import breeze.linalg.{DenseVector, Vector => Vec}
import edu.cmu.cs.lti.ark.fn.parsing.ArgIdTrainer.{frameExampleToArgExamples, readModel}
import edu.cmu.cs.lti.ark.fn.parsing.FrameFeaturesCache.readFrameFeatures
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions
import edu.cmu.cs.lti.ark.ml.optimization._
import edu.cmu.cs.lti.ark.ml.{FeatureVector, MultiClassTrainingExample}
import resource.managed

import scala.collection.Iterator.continually
import scala.collection.JavaConverters._
import scala.io.{Codec, Source}


/**
 * Trains the Argument Id model, using pre-cached features
 * command-line arguments as follows:
 * frameFeaturesCacheFile: path to file containing a serialized cache of all of the features
 * extracted from the training data
 * alphabetFile: path to file containing the alphabet
 * l2Strength: L2 regularization hyperparameter
 * numThreads: the number of parallel threads to run while optimizing
 * modelFile: path to output file to write resulting model to. intermediate models will be written to
 * modelFile + "_" + i
 */
object TrainArgIdApp extends App {
  val opts = new FNModelOptions(args)
  val modelFile = opts.modelFile.get
  val oWarmStartModelFile = Option(opts.warmStartModelFile.get)
  val alphabetFile = opts.alphabetFile.get
  val frameFeaturesCacheFile = opts.frameFeaturesCacheFile.get
  val l1Strength = Option(opts.l1Strength.get).getOrElse(0.0)
  val l2Strength = Option(opts.l2Strength.get).getOrElse(0.0)
  val batchSize = opts.batchSize.get
  val saveEveryKBatches = opts.saveEveryKBatches.get
  val numModelsToSave = opts.numModelsToSave.get

  // Read the size of the model from the first line of `alphabetFile`.
  private val numFeats: Int = {
    managed(new Scanner(new FileInputStream(alphabetFile))).acquireAndGet(_.nextInt + 1)
  }
  System.err.println("Reading cached training data")
  val trainingData: Array[MultiClassTrainingExample] = {
    readFrameFeatures(frameFeaturesCacheFile).acquireAndGet(
      _.flatMap(frameExampleToArgExamples(numFeats)).toArray
    )
  }
  System.err.println("Done reading cached training data. %d training examples.".format(trainingData.length))
  val initialWeights = oWarmStartModelFile match {
    case Some(file) => readModel(file)
    case None => DenseVector.zeros[Double](numFeats)
  }
  ArgIdTrainer.runAdaDelta(
    modelFile = modelFile,
    initialWeights = initialWeights,
    trainingData = trainingData,
    lossFn = SquaredHingeLoss,
    l1Strength = l1Strength,
    l2Strength = l2Strength,
    batchSize = batchSize,
    saveEveryKBatches = saveEveryKBatches,
    numModelsToSave = numModelsToSave
  )
}
object ArgIdTrainer {
  def runAdaDelta(modelFile: String,
                  initialWeights: DenseVector[Double],
                  trainingData: Array[MultiClassTrainingExample],
                  lossFn: SubDifferentiableLoss[MultiClassTrainingExample],
                  l1Strength: Double,
                  l2Strength: Double,
                  batchSize: Int = 4000,
                  saveEveryKBatches: Int = 500,
                  numModelsToSave: Int = 30) {
    val n = trainingData.length
    val optimizer = MiniBatch(trainingData, lossFn, l1Strength, l2Strength, batchSize)
    lazy val startTime = System.currentTimeMillis()
    val optimizationPath = optimizer.optimizationPath(initialWeights).map({
      case (state, loss, iteration, batchNum) =>
        val partialIt = iteration + (batchNum.toDouble * batchSize / n)
        val time = (System.currentTimeMillis() - startTime) / 1000.0
        System.err.println("i: %.3f,  loss: %.5f,  regLoss: %.5f,  time:%.2fs".format(partialIt, loss, state.regLoss, time))
        state
    })
    val everyK = continually(optimizationPath.drop(saveEveryKBatches - 1).next())
    for ((state, i) <- everyK.take(numModelsToSave).zipWithIndex) {
      writeModel(state.weights, "%s_%04d".format(modelFile, i))
    }
  }

  def frameExampleToArgExamples(numFeats: Int)(example: FrameFeatures): Iterator[MultiClassTrainingExample] = {
    val frameFeatures = example.fElementSpansAndFeatures.asScala.iterator
    val goldLabels = example.goldSpanIdxs.asScala.iterator
    for ((argFeats, label) <- frameFeatures zip goldLabels) yield {
      val featureVector = argFeats.map(f => FeatureVector(f.features, numFeats))
      MultiClassTrainingExample(featureVector, label)
    }
  }

  def readModel(warmStartModelFile: String): DenseVector[Double] = {
    System.err.println("Reading warm start model from %s".format(warmStartModelFile))
    val input = Source.fromFile(warmStartModelFile)(Codec.UTF8)
    val weights = DenseVector(input.getLines().map(_.toDouble).toArray)
    System.err.println("Done reading warm start model from %s".format(warmStartModelFile))
    weights
  }

  def writeModel(weights: Vec[Double], modelFile: String) {
    for (ps <- managed(new PrintStream(new FileOutputStream(modelFile)))) {
      System.err.println("Writing model to %s".format(modelFile))
      weights.foreach(ps.println)
      System.err.println("Finished writing model %s".format(modelFile))
    }
  }
}
