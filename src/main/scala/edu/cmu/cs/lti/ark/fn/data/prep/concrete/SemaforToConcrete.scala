package edu.cmu.cs.lti.ark.fn.data.prep.concrete

import Util.getAllSentences
import java.io.{FileInputStream, FileOutputStream, File}
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.io.Source
import resource.managed
import edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult
import edu.cmu.cs.lti.ark.util.ds.Range0Based
import edu.jhu.hlt.concrete.Concrete._
import edu.jhu.hlt.concrete.util.IdUtil.generateUUID
import org.joda.time.{DateTimeZone, DateTime}

object SemaforToConcrete {
  def main(args: Array[String]) {
    val Array(concreteFile, baseDir, outFile) = args

    addAllAnnotations(new File(concreteFile), new File(baseDir), new File(outFile))
  }

  /* Takes a file with multiple Communications, adds Semafor annotations, and writes to a new file */
  def addAllAnnotations(concreteFile: File, baseDir: File, outFile: File) {
    val meta = AnnotationMetadata.newBuilder()
      .setTool("edu.cmu.cs.lti.ark:Semafor:3.0-alpha-05-SNAPSHOT")
      .setTimestamp(new DateTime(DateTimeZone.UTC).getMillis)
      .build()
    val semaforDir = new File(baseDir, "semafor_parsed/concrete")
    val preprocessedDir = new File(baseDir, "preprocessed")
    for (inputStream <- managed(new FileInputStream(concreteFile));
         outputStream <- managed(new FileOutputStream(outFile))) {
      val communications = Stream.continually(Communication.parseDelimitedFrom(inputStream)).takeWhile(_ != null)
      val withAllAnnotations = communications.map(comm => {
        val withMxpost = MxpostToConcrete.addMxpostAnnotations(comm, preprocessedDir)
        val withMalt = MaltToConcrete.addMaltAnnotations(withMxpost, preprocessedDir)
        addSemaforAnnotations(withMalt, semaforDir, meta)
      })
      withAllAnnotations.foreach(_.writeDelimitedTo(outputStream))
    }
  }

  /* Adds one SituationMentionSet per sentence */
  def addSemaforAnnotations(communication: Communication,
                            semaforDir: File,
                            meta: AnnotationMetadata): Communication = {
    val concreteSentences = getAllSentences(communication)
    val semaforFile = new File(semaforDir, communication.getGuid.getCommunicationId + ".json")
    val semaforResults = Source.fromFile(semaforFile).getLines().map(SemaforParseResult.fromJson).toList
    val semaforResultsConcrete = (semaforResults zip concreteSentences).map({
      case (parse, sentence) => semaforParseToConcrete(parse, sentence, meta)
    })
    communication.toBuilder
      .setUuid(generateUUID())
      .addAllSituationMentionSet(semaforResultsConcrete)
      .build()
  }

  def semaforParseToConcrete(parseResult: SemaforParseResult,
                             sentence: Sentence,
                             meta: AnnotationMetadata): SituationMentionSet = {
    val tokenization = sentence.getTokenization(0)
    val mentions = parseResult.frames.flatMap(frameToConcrete(_, tokenization))
    SituationMentionSet.newBuilder()
      .setUuid(generateUUID())
      .setMetadata(meta)
      .addAllMention(mentions)
      .build()
  }

  def frameToConcrete(frameParse: SemaforParseResult.Frame, tokenization: Tokenization): List[SituationMention] = {
    val situationMentions = ListBuffer[SituationMention]()
    val targetTokens = spansToConcrete(frameParse.target.spans, tokenization)
    for (scoredRoleAssignment <- frameParse.annotationSets) {
      val frameName = frameParse.target.name
      val predicateBuilder = SituationMention.newBuilder()
        .setUuid(generateUUID())
        .setTokens(targetTokens)
        .setConfidence(scoredRoleAssignment.score.toFloat)
        .setSituationKindLemma("FrameNet:" + frameName)
      for (frameElement <- scoredRoleAssignment.frameElements) {
        val roleSituation = SituationMention.newBuilder()
          .setUuid(generateUUID())
          .setTokens(spansToConcrete(frameElement.spans, tokenization))
          .build()
        situationMentions.append(roleSituation)
        val argument = SituationMention.Argument.newBuilder()
          .setRoleLabel("FrameNet:" + frameName + ":" + frameElement.name)
          .setSituationMentionId(roleSituation.getUuid)
          .build()
        predicateBuilder.addArgument(argument)
      }
      situationMentions.append(predicateBuilder.build())
    }
    situationMentions.toList
  }

  def spansToConcrete(spans: TraversableOnce[SemaforParseResult.Frame.Span],
                      tokenization: Tokenization): TokenRefSequence = {
    val tokenSequenceBuilder = TokenRefSequence.newBuilder()
      .setTokenizationId(tokenization.getUuid)
    spans.foreach(span => tokenSequenceBuilder.addAllTokenIndex(new Range0Based(span.start, span.end)))
    tokenSequenceBuilder.build()
  }
}
