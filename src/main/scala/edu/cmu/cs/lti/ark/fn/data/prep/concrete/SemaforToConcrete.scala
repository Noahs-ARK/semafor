package edu.cmu.cs.lti.ark.fn.data.prep.concrete

import java.io.{FileInputStream, FileOutputStream, File}
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import edu.cmu.cs.lti.ark.fn.data.prep.concrete.MaltToConcrete.{depParseFromConcrete, addMaltAnnotations}
import edu.cmu.cs.lti.ark.fn.data.prep.concrete.StanfordToConcrete.addStanfordAnnotations
import edu.cmu.cs.lti.ark.fn.data.prep.concrete.Utils.getAllSentences
import edu.cmu.cs.lti.ark.fn.data.prep.Malt
import edu.cmu.cs.lti.ark.fn.data.prep.StanfordProcessor
import edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult
import edu.cmu.cs.lti.ark.fn.Semafor
import edu.cmu.cs.lti.ark.util.ds.Range0Based
import resource.managed
import edu.jhu.hlt.concrete.Concrete
import edu.jhu.hlt.concrete.util.IdUtil.generateUUID
import org.joda.time.{DateTimeZone, DateTime}

object SemaforToConcrete {
  val toolName = "edu.cmu.cs.lti.ark:Semafor:3.0-alpha-05-SNAPSHOT"
  val modelDir = new File("/Users/sam/code/semafor/models/semafor_malt_model_20121129")

  def main(args: Array[String]) {
    val Array(concreteFile, outFile) = args

    addAllAnnotations(new File(concreteFile), new File(outFile))
  }

  /** Takes a file containing multiple Communications, adds Stanford, Malt and
    * Semafor annotations, and writes to a new file
    */
  def addAllAnnotations(rawConcreteFile: File, outFile: File) {
    // store intermediate results so that we don't have to have stanford, malt and semafor
    // models loaded in memory all at once
    val stanfordTempFile = new File(outFile.getCanonicalPath + ".stanford")
    val maltTempFile = new File(outFile.getCanonicalPath + ".malt")

    // add stanford annotations
    for (inputStream <- managed(new FileInputStream(rawConcreteFile));
           stanfordOutStream <- managed(new FileOutputStream(stanfordTempFile))) {
      val stanfordProcessor = StanfordProcessor.defaultInstance
      val rawCommunications =
        Iterator.continually(Concrete.Communication.parseDelimitedFrom(inputStream)).takeWhile(_ != null)
      val withStanford = rawCommunications.map(addStanfordAnnotations(stanfordProcessor, _))
      withStanford.foreach(_.writeDelimitedTo(stanfordOutStream))
    }
    // add malt annotations
    for (inputStream <- managed(new FileInputStream(stanfordTempFile));
           maltOutStream <- managed(new FileOutputStream(maltTempFile))) {
      val maltParser = new Malt(modelDir)
      val withStanford =
        Iterator.continually(Concrete.Communication.parseDelimitedFrom(inputStream)).takeWhile(_ != null)
      val withMalt = withStanford.map(addMaltAnnotations(maltParser, _))
      withMalt.foreach(_.writeDelimitedTo(maltOutStream))
    }
    // add semafor annotations
    for (inputStream <- managed(new FileInputStream(maltTempFile));
           outStream <- managed(new FileOutputStream(outFile))) {
      val semafor = Semafor.getSemaforInstance(modelDir.getCanonicalPath)
      val withMalt =
        Iterator.continually(Concrete.Communication.parseDelimitedFrom(inputStream)).takeWhile(_ != null)
      val withAllAnnotations = withMalt.map(addSemaforAnnotations(semafor, _))
      withAllAnnotations.foreach(_.writeDelimitedTo(outStream))
    }
  }

  /* Adds one SituationMentionSet per sentence */
  def addSemaforAnnotations(semafor: Semafor,
                            communication: Concrete.Communication): Concrete.Communication = {
    val meta = Concrete.AnnotationMetadata.newBuilder()
      .setTool(toolName)
      .setTimestamp(new DateTime(DateTimeZone.UTC).getMillis)
      .build()
    val concreteSentences = getAllSentences(communication)
    val semaforResultsConcrete = concreteSentences.view.map(concreteSentence => {
      val depParsedSentence = depParseFromConcrete(concreteSentence)
      val semaforResult = semafor.parseSentence(depParsedSentence)
      semaforParseToConcrete(semaforResult, concreteSentence, meta)
    })
    communication.toBuilder
      .setUuid(generateUUID())
      .addAllSituationMentionSet(semaforResultsConcrete)
      .build()
  }

  def semaforParseToConcrete(parseResult: SemaforParseResult,
                             sentence: Concrete.Sentence,
                             meta: Concrete.AnnotationMetadata): Concrete.SituationMentionSet = {
    val tokenization = sentence.getTokenization(0)
    val mentions = parseResult.frames.flatMap(frameToConcrete(_, tokenization))
    Concrete.SituationMentionSet.newBuilder()
      .setUuid(generateUUID())
      .setMetadata(meta)
      .addAllMention(mentions)
      .build()
  }

  def frameToConcrete(frameParse: SemaforParseResult.Frame,
                      tokenization: Concrete.Tokenization): List[Concrete.SituationMention] = {
    val situationMentions = ListBuffer[Concrete.SituationMention]()
    val targetTokens = spansToConcrete(frameParse.target.spans, tokenization)
    for (scoredRoleAssignment <- frameParse.annotationSets) {
      val frameName = frameParse.target.name
      val situationKind = "FrameNet:" + frameName
      val predicateBuilder = Concrete.SituationMention.newBuilder()
        .setUuid(generateUUID())
        .setTokens(targetTokens)
        .setConfidence(scoredRoleAssignment.score.toFloat)
        .setSituationKindLemma(situationKind)
      for (frameElement <- scoredRoleAssignment.frameElements) {
        val roleLabel = situationKind + ":" + frameElement.name
        val roleSituation = Concrete.SituationMention.newBuilder()
          .setUuid(generateUUID())
          .setTokens(spansToConcrete(frameElement.spans, tokenization))
          .setSituationKindLemma(roleLabel)
          .build()
        situationMentions.append(roleSituation)
        val argument = Concrete.SituationMention.Argument.newBuilder()
          .setRoleLabel(roleLabel)
          .setSituationMentionId(roleSituation.getUuid)
          .build()
        predicateBuilder.addArgument(argument)
      }
      situationMentions.append(predicateBuilder.build())
    }
    situationMentions.toList
  }

  def spansToConcrete(spans: TraversableOnce[SemaforParseResult.Frame.Span],
                      tokenization: Concrete.Tokenization): Concrete.TokenRefSequence = {
    val tokenSequenceBuilder = Concrete.TokenRefSequence.newBuilder()
      .setTokenizationId(tokenization.getUuid)
    spans.foreach(span => tokenSequenceBuilder.addAllTokenIndex(new Range0Based(span.start, span.end)))
    tokenSequenceBuilder.build()
  }
}
