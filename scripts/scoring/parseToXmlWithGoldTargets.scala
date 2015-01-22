/*
scala \
  -cp "${classpath}" \
  -J-Xms4g \
  -J-Xmx4g \
  -J-XX:ParallelGCThreads=2 \
  scripts/scoring/parseToXmlWithGoldTargets.scala \
  ${model_name} \
  ${cv}
 */

import java.io.File

import com.google.common.base.Charsets.UTF_8
import com.google.common.io.Files
import com.google.common.io.Files._
import edu.cmu.cs.lti.ark.fn.Semafor
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence
import edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec.{ConllCodec, SentenceIterator}
import edu.cmu.cs.lti.ark.fn.evaluation.PrepareFullAnnotationXML._
import edu.cmu.cs.lti.ark.util.ds.Range0Based
import resource._

import scala.collection.JavaConverters.{asScalaBufferConverter, asScalaIteratorConverter, seqAsJavaListConverter}
import scala.io.Source

val K_BEST = 1
val FRAME_NAME_FIELD = 3
val TARGET_FIELD = 5
val SENTENCE_FIELD = 7

val HOME_DIR = new File(System.getProperty("user.home"))
val BASE_DIR = new File(HOME_DIR, "code/semafor")
val SEMAFOR_HOME = new File(BASE_DIR, "semafor")
val DATA_DIR = new File(SEMAFOR_HOME, "training/data/naacl2012")
val TOKENIZED_FILE_TEMPLATE = "cv.%s.sentences.tokenized"
val FRAME_ID_FILE_TEMPLATE = "cv.%s.sentences.frames"
val DEP_PARSE_FILE_TEMPLATE = "cv.%s.sentences.maltparsed.conll"
val ALL_LEMMA_TAGS_FILE_TEMPLATE = "cv.%s.sentences.all.lemma.tags"


// CLI args:
val modelName = args(0) // "adadelta_20150122" // "turbo_matsumoto_20140702"
val cvFold = args(1) // "dev" or "test"

val experimentsDir = new File(new File(SEMAFOR_HOME, "experiments"), modelName)
val modelDir = new File(experimentsDir, "model")
val resultsDir = new File(experimentsDir, "output")

// load up the model
val sem = Semafor.getSemaforInstance(modelDir.getCanonicalPath)

// parse
parseToXmlWithGoldTargets(cvFold, sem)




def setSentenceId(line: String, sentenceId: String): String = {
  val fields = line.split("\t")
  fields(SENTENCE_FIELD) = sentenceId
  fields.mkString("\t")
}

def getTargetFromFrameLine(frameLine: String): List[Integer] = {
  frameLine.split("\t")(TARGET_FIELD).split("_").map(new Integer(_)).toList
}

def parseToXmlWithGoldTargets(infix: String, sem: Semafor) {
  val frameIdFile = new File(DATA_DIR, FRAME_ID_FILE_TEMPLATE.format(infix))
  val depParseFile = new File(DATA_DIR, DEP_PARSE_FILE_TEMPLATE.format(infix))
  val outputFeFile = new File(resultsDir, infix + ".full.predict.frame.elements")

  System.err.println("\n\nParsing file: %s\n\n".format(depParseFile))

  parseToFeFormatWithGoldTargets(frameIdFile, depParseFile, outputFeFile, sem)

  val tokenizedFile = new File(DATA_DIR, TOKENIZED_FILE_TEMPLATE.format(infix))
  val numSentences = Source.fromFile(tokenizedFile).getLines().length
  val allLemmaTagsFile = new File(DATA_DIR, ALL_LEMMA_TAGS_FILE_TEMPLATE.format(infix))
  val outputXmlFile = new File(resultsDir, infix + ".full.predict.xml")

  System.err.println("\n\nGenerating xml: %s\n\n".format(outputXmlFile.getAbsolutePath))

  generateXMLForPrediction(
    outputFeFile.getAbsolutePath,
    new Range0Based(0, numSentences, false),
    allLemmaTagsFile.getAbsolutePath,
    tokenizedFile.getAbsolutePath,
    outputXmlFile.getAbsolutePath)
}

private def parseToFeFormatWithGoldTargets(frameIdFile: File,
                                           depParseFile: File,
                                           outputFile: File,
                                           sem: Semafor) {
  // read in dep parses
  val sentences = {
    val sentenceIterator = new SentenceIterator(ConllCodec, depParseFile)
    val unLemmatized = sentenceIterator.asScala.toList
    sentenceIterator.close()
    unLemmatized.map(sem.addLemmas)
  }
  // read in gold targets and collate with sentences
  val sentencesAndTargets: List[(Int, Sentence, List[List[Integer]])] = {
    val frameLines = Files.readLines(frameIdFile, UTF_8).asScala.toList
    val framesBySentence = frameLines.groupBy(_.split("\t")(SENTENCE_FIELD).trim.toInt)
    framesBySentence.keys.toList.sorted.map(i => (i, sentences(i), framesBySentence(i).map(getTargetFromFrameLine)))
  }
  // parse
  val resultLines = for ((sentenceId, sentence, targets) <- sentencesAndTargets.iterator) yield {
    System.err.println(s"Sentence Id: $sentenceId")
    // frame identification
    val idResult = sem.predictFrames(sentence, targets.map(_.asJava).asJava)
    // argument identification
    val idResultLines = sem.getArgumentIdInput(sentence, idResult)
    val results = sem.predictArgumentLines(sentence, idResultLines, 1).asScala
    results.flatMap(_.split("\n")).map(line => setSentenceId(line, sentenceId.toString))
  }
  // write results to file
  for (out <- managed(newWriter(outputFile, UTF_8));
       line <- resultLines.flatten) {
    out.write(line)
    out.write('\n')
  }
}
