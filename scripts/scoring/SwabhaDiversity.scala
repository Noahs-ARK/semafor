/*
scala -cp ".:target/Semafor-3.0-alpha-05-SNAPSHOT.jar" -J-Xms4g -J-Xmx4g -J-XX:ParallelGCThreads=2 scripts/scoring/SwabhaDiversity.scala kbest
 */

import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence
import edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec.{ConllCodec, SentenceIterator}
import edu.cmu.cs.lti.ark.fn.Semafor
import edu.cmu.cs.lti.ark.fn.evaluation.PrepareFullAnnotationXML.generateXMLForPrediction
import edu.cmu.cs.lti.ark.util.ds.Range0Based
import com.google.common.io.Files
import com.google.common.base.Charsets.UTF_8
import java.io.File
import scala.collection.JavaConverters.{asScalaBufferConverter, seqAsJavaListConverter, asScalaIteratorConverter}
import scala.io.Source



val K_BEST = 1
val SENTENCE_FIELD = 7

val SEMAFOR_HOME = new File(System.getProperty("user.home"), "code/semafor/semafor")

val TOKENIZED_FILE = new File(SEMAFOR_HOME, "training/data/naacl2012/cv.test.sentences.tokenized")
val FRAME_ID_FILE = new File(SEMAFOR_HOME, "training/data/naacl2012/cv.test.sentences.frames")
val GOLD_DEP_PARSE_FILENAME = new File(SEMAFOR_HOME, "training/data/naacl2012/cv.test.sentences.turboparsed.matsumoto.all.lemma.tags")
val NUM_SENTENCES = Source.fromFile(TOKENIZED_FILE).getLines().length

val EXPERIMENTS_DIR = new File(SEMAFOR_HOME, "experiments/turbo_matsumoto_20140723")
val MODEL_DIR = new File(EXPERIMENTS_DIR, "model")

val DEP_PARSE_BASE_FOLDER = new File(SEMAFOR_HOME, "experiments/swabha/diversekbestdeps")

val diversityType = args(0) // e.g. "grand"

parseAllSwabhasFiles(diversityType)




def setSentenceId(line: String, sentenceId: String): String = {
  val fields = line.split("\t")
  fields(SENTENCE_FIELD) = sentenceId
  fields.mkString("\t")
}


def predictArgsForSentence(sentence: Sentence, frames: List[String], kBest: Int, sem: Semafor): List[String] = {
  // predictArgumentLines needs the sentenceId field to be 0, but we don't want to forget it
  val sentenceId = frames(0).split("\t")(SENTENCE_FIELD)
  System.err.println("\nSentence Id: %s\n".format(sentenceId))
  // set it to 0 and run arg id'ing
  val zeroed = frames.map(setSentenceId(_, "0"))
  val results: List[String] = sem.predictArgumentLines(sentence, zeroed.asJava, kBest).asScala.toList
  // set it back to the right val
  results.flatMap(_.split("\n")).map(setSentenceId(_, sentenceId))
}


def predictAllArgs(frames: List[String], unLemmatized: List[Sentence], sem: Semafor): List[String] = {
  val framesBySentence = frames.groupBy(_.split("\t")(SENTENCE_FIELD).trim.toInt)
  val sentences = unLemmatized.map(sem.addLemmas)
  val sentencesAndFrames = framesBySentence.keys.toList.sorted.map(i => (sentences(i), framesBySentence(i)))
  val argResults = sentencesAndFrames.map({
    case (sentence, frame) =>
      predictArgsForSentence(sentence, frame.toList, K_BEST, sem)
  })
  argResults.flatten
}


def runWithGoldFrames(frameIdFile: File, depParseFile: File, output_file: File, sem: Semafor) {
  System.err.println("\n\nProcessing file: %s\n\n".format(depParseFile))
  // read in frame id output
  val frames = Files.readLines(frameIdFile, UTF_8).asScala.toList
  // read in dep parses
  val sentenceIterator = new SentenceIterator(ConllCodec, depParseFile)
  val unLemmatized = sentenceIterator.asScala.toList
  sentenceIterator.close()
  // run arg id'ing
  val resultLines = predictAllArgs(frames, unLemmatized, sem)
  // write results to file
  Files.write(resultLines.mkString("\n"), output_file, UTF_8)
}

def parseAllSwabhasFiles(diversityType: String) {
  val depParseFolder = new File(DEP_PARSE_BASE_FOLDER, diversityType)
  val baseOutputFolder = new File(new File(EXPERIMENTS_DIR, "output"), diversityType)
  val frameElementsOutputFolder = new File(baseOutputFolder, "frameElements")
  val xmlOutputFolder = new File(baseOutputFolder, "xml")

  // load up the model
  val sem = Semafor.getSemaforInstance(MODEL_DIR.getCanonicalPath)
  // parse each kthBest file
  for (depParseFile <- depParseFolder.listFiles) {
    val name = depParseFile.getName.split('.')(0)
    val feFile = new File(frameElementsOutputFolder, name + ".frame.elements")
    val xmlFile = new File(xmlOutputFolder, name + ".xml")
    runWithGoldFrames(FRAME_ID_FILE, depParseFile, feFile, sem)
    generateXMLForPrediction(
      feFile.getAbsolutePath,
      new Range0Based(0, NUM_SENTENCES, false),
      GOLD_DEP_PARSE_FILENAME.getAbsolutePath,
      TOKENIZED_FILE.getAbsolutePath,
      xmlFile.getAbsolutePath)
  }
}
