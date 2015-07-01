package edu.cmu.cs.lti.ark.fn.parsing

import java.io._

import com.google.common.base.Charsets.UTF_8
import com.google.common.io.Files
import edu.cmu.cs.lti.ark.fn.data.prep.formats.{Sentence, SentenceCodec}
import edu.cmu.cs.lti.ark.fn.parsing.FrameFeaturesCache.writeFrameFeatures
import resource.{ManagedResource, managed}

import scala.collection.JavaConverters._
import scala.io.{Codec, Source}
import scala.util.Try


object CacheFrameFeaturesApp {
  def main(args: Array[String]) {
    val feFile = new File(args(0))
    val depParsedFile = new File(args(1))
    val frameFeaturesCacheFile = new File(args(2))
    val alphabetFile = new File(args(3))
    System.err.println("Extracting features...")
    run(depParsedFile, feFile, frameFeaturesCacheFile, alphabetFile)
  }

  def run(depParsedFile: File,
          feFile: File,
          frameFeaturesCacheFile: File,
          alphabetFile: File) {
    val time = System.currentTimeMillis
    val sentences: Array[Sentence] =
      managed(Files.newReader(depParsedFile, UTF_8)).acquireAndGet {
        source => SentenceCodec.ConllCodec.readInput(source).asScala.toArray
      }
    val featureIndex = FeatureIndex.empty
    val dataPrep = new DataPrep(sentences, featureIndex)
    for (frameElementSource <- managed(Source.fromFile(feFile)(Codec.UTF8))) {
      val frameElementLines = frameElementSource.getLines()
      val frameFeatures = frameElementLines.map(dataPrep.frameFeatures)
      writeFrameFeatures(frameFeatures, frameFeaturesCacheFile)
    }
    System.err.println("Extracted features in " + (System.currentTimeMillis - time) + " millis.")
    featureIndex.save(alphabetFile)
  }
}


object FrameFeaturesCache {
  def writeFrameFeatures(localFeatures: TraversableOnce[FrameFeatures], outputFile: File): Unit = {
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
