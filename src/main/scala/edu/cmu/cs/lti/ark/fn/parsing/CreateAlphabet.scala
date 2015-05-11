package edu.cmu.cs.lti.ark.fn.parsing

import java.io._

import com.google.common.base.Charsets.UTF_8
import edu.cmu.cs.lti.ark.fn.parsing.FrameFeaturesCache.writeFrameFeatures
import resource.{ManagedResource, managed}

import scala.io.{Codec, Source}
import scala.util.Try


object CacheFrameFeaturesApp {
  def main(args: Array[String]) {
    val feFilename = args(0)
    val tagFilename = args(1)
    val frameFeaturesCacheFile = args(2)
    val alphabetFilename = args(3)
    System.err.println("Extracting features...")
    run(tagFilename, feFilename, frameFeaturesCacheFile, alphabetFilename)
  }

  def run(tagFilename: String,
          feFilename: String,
          frameFeaturesCacheFile: String,
          alphabetFilename: String) {
    val time = System.currentTimeMillis
    val tagLines = managed(Source.fromFile(tagFilename)(UTF_8)).acquireAndGet(_.getLines().toArray)
    val featureIndex = FeatureIndex.empty
    val dataPrep = new DataPrep(tagLines, featureIndex)
    for (frameElementSource <- managed(Source.fromFile(feFilename)(Codec.UTF8))) {
      val frameElementLines = frameElementSource.getLines()
      val frameFeatures = frameElementLines.map(dataPrep.frameFeatures)
      writeFrameFeatures(frameFeatures, frameFeaturesCacheFile)
    }
    System.err.println("Extracted features in " + (System.currentTimeMillis - time) + " millis.")
    featureIndex.save(alphabetFilename)
  }
}


object FrameFeaturesCache {
  def writeFrameFeatures(localFeatures: TraversableOnce[FrameFeatures], outputFile: String): Unit = {
    for (
      output <- managed(new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile))));
      (frameFeatures, i) <- localFeatures.toStream.zipWithIndex
    ) {
      if (i % 100 == 0) System.err.print(".")
      output.writeObject(frameFeatures)
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
