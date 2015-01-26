package edu.cmu.cs.lti.ark.fn.parsing

import java.io._
import java.util
import java.util.Scanner

import breeze.linalg.{DenseVector, Vector => Vec}
import com.google.common.base.Charsets
import com.google.common.io.Files
import edu.cmu.cs.lti.ark.fn.parsing.ArgIdTrainer.{readModel, frameExampleToArgExamples}
import edu.cmu.cs.lti.ark.fn.parsing.FrameFeaturesCache.readFrameFeatures
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions
import edu.cmu.cs.lti.ark.ml.optimization._
import edu.cmu.cs.lti.ark.ml.{FeatureVector, MultiClassTrainingExample}
import resource.{ManagedResource, managed}

import scala.collection.Iterator.continually
import scala.collection.JavaConverters._
import scala.io.{Codec, Source}
import scala.util.Try

object CacheFrameFeaturesApp extends App {
  val opts = new FNModelOptions(args)
  val eventsFile = opts.eventsFile.get
  val spanFile = opts.spansFile.get
  val frFile = opts.trainFrameFile.get
  val outputFile = opts.frameFeaturesCacheFile.get

  val frameLines: util.List[String] = Files.readLines(new File(frFile), Charsets.UTF_8)
  val lfr = new LocalFeatureReading(eventsFile, spanFile, frameLines)
  val frameFeaturesList: util.List[FrameFeatures] = lfr.readLocalFeatures
  FrameFeaturesCache.writeFrameFeatures(frameFeaturesList.asScala, outputFile)
}

object FrameFeaturesCache {
  def writeFrameFeatures(frameFeaturesList: TraversableOnce[FrameFeatures], outputFile: String): Unit = {
    for (output <- managed(new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile))))) {
      for (frameFeatures <- frameFeaturesList) {
        output.writeObject(frameFeatures)
      }
    }
  }

  def readFrameFeatures(inputFile: String): ManagedResource[Iterator[FrameFeatures]] = {
    for (input <- managed(new ObjectInputStream(new BufferedInputStream(new FileInputStream(inputFile))))) yield {
      Iterator.from(1).map(i => {
        if (i % 100 == 0) System.err.print(".")
        Try(input.readObject.asInstanceOf[FrameFeatures])
      }).takeWhile(_.isSuccess).map(_.get)
    }
  }
}

/**
 * Trains the Argument Id model, using pre-cached features
 * command-line arguments as follows:
 * frameFeaturesCacheFile: path to file containing a serialized cache of all of the features
 * extracted from the training data
 * alphabetFile: path to file containing the alphabet
 * lambda: L2 regularization hyperparameter
 * numThreads: the number of parallel threads to run while optimizing
 * modelFile: path to output file to write resulting model to. intermediate models will be written to
 * modelFile + "_" + i
 */
object TrainArgIdApp extends App {
  val opts = new FNModelOptions(args)
  val modelFile = opts.modelFile.get
  val oWarmStartModelFile = if (opts.warmStartModelFile.present) Some(opts.warmStartModelFile.get) else None
  val alphabetFile = opts.alphabetFile.get
  val frameFeaturesCacheFile = opts.frameFeaturesCacheFile.get
  val lambda = opts.lambda.get
  val numThreads = opts.numThreads.get
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
  System.err.println(s"Done reading cached training data. ${trainingData.length} training examples.")
  val initialWeights = oWarmStartModelFile.fold(DenseVector.zeros[Double](numFeats))(readModel)
  ArgIdTrainer(
    modelFile,
    initialWeights,
    numFeats,
    trainingData,
    SquaredHingeLoss,
    lambda,
    numThreads
  ).runAdaDelta(
    batchSize = batchSize,
    saveEveryKBatches = saveEveryKBatches,
    numModelsToSave = numModelsToSave
  )
}

case class ArgIdTrainer(modelFile: String,
                        initialWeights: DenseVector[Double],
                        numFeats: Int,
                        trainingData: Array[MultiClassTrainingExample],
                        lossFn: SubDifferentiableLoss[MultiClassTrainingExample],
                        lambda: Double,
                        numThreads: Int) {
  def runAdaDelta(batchSize: Int = 128,
                  saveEveryKBatches: Int = 500,
                  numModelsToSave: Int = 100) {
    import edu.cmu.cs.lti.ark.fn.parsing.ArgIdTrainer.writeModel
    val optimizer = MiniBatch(trainingData, lossFn, lambda, batchSize, numThreads)
    val optimizationPath = optimizer.optimizationPath(initialWeights)
    val everyK = continually(optimizationPath.drop(saveEveryKBatches - 1).next())
    for (((loss, weights), i) <- everyK.take(numModelsToSave).zipWithIndex) {
      writeModel(weights, f"${modelFile}_$i%04d")
    }
  }
}

object ArgIdTrainer {
  def frameExampleToArgExamples(numFeats: Int)(example: FrameFeatures): Iterator[MultiClassTrainingExample] = {
    val frameFeatures = example.fElementSpansAndFeatures.asScala.iterator
    val goldLabels = example.goldSpanIdxs.asScala.iterator
    for ((argFeats, label) <- frameFeatures zip goldLabels) yield {
      val featureVector = argFeats.map(f => FeatureVector(f.features, numFeats))
      MultiClassTrainingExample(featureVector, label)
    }
  }

  def readModel(warmStartModelFile: String): DenseVector[Double] = {
    System.err.println(s"Reading warm start model from $warmStartModelFile")
    val input = Source.fromFile(warmStartModelFile)(Codec.UTF8)
    val weights = DenseVector(input.getLines().map(_.toDouble).toArray)
    System.err.println(s"Done reading warm start model from $warmStartModelFile")
    weights
  }

  def writeModel(weights: Vec[Double], modelFile: String) {
    for (ps <- managed(new PrintStream(new FileOutputStream(modelFile)))) {
      System.err.println(s"Writing model to $modelFile")
      weights.foreach(ps.println)
      System.err.println(s"Finished writing model $modelFile")
    }
  }
}
