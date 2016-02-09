/*
Runs SEMAFOR using gold targets and preexisting dep parse

to run:

source training/config.sh
scala -cp ${CLASSPATH} -J-Xmx3g scripts/scoring/runWithGoldTargets.scala \
    /path/to/semafor/model \
    /path/to/gold/sentences.frames \
    /path/to/gold/sentences.maltparsed.conll \
    /path/to/outputFile.xml \
    [true]  # use gold frames?
 */
import java.io.File
import java.util

import com.google.common.base.Charsets.UTF_8
import com.google.common.io.Files
import edu.cmu.cs.lti.ark.fn.Semafor
import edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec.{ConllCodec, SentenceIterator}
import edu.cmu.cs.lti.ark.fn.data.prep.formats.{AllLemmaTags, Sentence}
import edu.cmu.cs.lti.ark.fn.evaluation.PrepareFullAnnotationXML.createXMLDoc
import edu.cmu.cs.lti.ark.util.XmlUtils
import edu.cmu.cs.lti.ark.util.ds.{Pair, Range0Based}

import scala.collection.JavaConverters.{asScalaBufferConverter, asScalaIteratorConverter, seqAsJavaListConverter}
import scala.collection.mutable

val TARGET_FIELD = 5
val SENTENCE_FIELD = 7

val modelDir = new File(args(0))
val framesFile = new File(args(1))
val depParseFile = new File(args(2))
val outputFile = new File(args(3))
val useGoldFrames = args.size > 4 && Seq("true", "1").contains(args(4).toLowerCase)


// load up the model
val sem = Semafor.getSemaforInstance(modelDir.getCanonicalPath)

runWithGoldTargets(sem, useGoldFrames = useGoldFrames)


def setSentenceId(line: String, sentenceId: String): String = {
  val fields = line.split("\t")
  fields(SENTENCE_FIELD) = sentenceId
  fields.mkString("\t")
}

def getTargetFromFrameLine(frameLine: String): util.List[Integer] = {
    frameLine.split("\t")(TARGET_FIELD).split("_").map(Integer.valueOf).toList.asJava
}

def predictArgsForSentence(sentence: Sentence,
                           frames: Seq[String],
                           sem: Semafor,
                           useGoldFrames: Boolean = false): util.List[String] = {
  val argumentIdInput: util.List[String] = if (useGoldFrames) {
    frames.asJava
  } else {
    val targets = frames.map(getTargetFromFrameLine).asJava
    val idResults: util.List[Pair[util.List[Integer], String]] = sem.predictFrames(sentence, targets)
    sem.getArgumentIdInput(sentence, idResults)
  }
  sem.predictArgumentLines(sentence, argumentIdInput, 1)
}

def predictAllArgs(frames: Seq[String],
                   unLemmatized: Seq[Sentence],
                   sem: Semafor,
                   useGoldFrames: Boolean = false): Seq[mutable.Buffer[String]] = {
  val framesBySentence = frames.groupBy(_.split("\t")(SENTENCE_FIELD).trim.toInt)
  val sentences = unLemmatized.map(sem.addLemmas)
  val sentencesAndFrames: Seq[(Sentence, Seq[String])] = sentences.zipWithIndex.map({case (s, i) => (s, framesBySentence.getOrElse(i, Nil))})
  sentencesAndFrames.zipWithIndex.map({ case ((sentence, frame), i) =>
      predictArgsForSentence(sentence, frame.toList, sem).asScala.map(line => setSentenceId(line, i.toString))
  })
}

def runWithGoldTargets(sem: Semafor, useGoldFrames: Boolean = false) {
  System.err.println("\n\nProcessing file: %s\n\n".format(framesFile))
  // read in frame id gold standard
  val frameLines = Files.readLines(framesFile, UTF_8).asScala.toList
  // read in dep parses
  val sentenceIterator = new SentenceIterator(ConllCodec, depParseFile)
  val unLemmatized: Seq[Sentence] = sentenceIterator.asScala.toSeq
  val tokenizedLines: util.List[String] = unLemmatized.map(_.getTokens.asScala.map(_.getForm).mkString(" ")).asJava
  val depParseLines: util.List[String] = unLemmatized.map(s => AllLemmaTags.makeLine(s.toAllLemmaTagsArray)).asJava
  // run arg id'ing
  val resultLines: util.List[String] = predictAllArgs(frameLines, unLemmatized, sem).flatten.asJava
  // convert to xml
  val doc = createXMLDoc(resultLines, new Range0Based(0, unLemmatized.size, false), depParseLines, tokenizedLines)
  // write results to file
  XmlUtils.writeXML(outputFile.getCanonicalPath, doc)
  sentenceIterator.close()
  System.err.println("\n\nDone processing file: %s\n\n".format(framesFile))
}
