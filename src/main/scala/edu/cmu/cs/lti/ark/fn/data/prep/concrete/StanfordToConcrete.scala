package edu.cmu.cs.lti.ark.fn.data.prep.concrete

import scala.xml
import scala.collection.JavaConversions._
import edu.jhu.hlt.concrete.Concrete._
import edu.jhu.hlt.concrete.util.IdUtil.generateUUID
import org.joda.time.{DateTimeZone, DateTime}
import edu.cmu.cs.lti.ark.fn.data.prep.StanfordProcessor
import edu.jhu.hlt.concrete.Concrete.TokenTagging.TaggedToken
import scala.collection.immutable.Seq

object StanfordToConcrete {
  val toolName = "edu.stanford.nlp:stanford-corenlp:3.2.0"

  def addStanfordAnnotations(stanfordParser: StanfordProcessor, communication: Communication): Communication = {
    val meta = AnnotationMetadata.newBuilder()
      .setTool(toolName)
      .setTimestamp(new DateTime(DateTimeZone.UTC).getMillis)
      .build()
    val originalText = communication.getText
    val doc: xml.Elem = stanfordParser.processToXml(originalText)
    val sentences = getSentencesFromStanfordXML(doc, meta)
    communication.toBuilder
      .addSectionSegmentation(
        SectionSegmentation.newBuilder()
          .setUuid(generateUUID())
          .addSection(
            Section.newBuilder()
              .setUuid(generateUUID())
              .setKind(Section.Kind.PASSAGE)
              .addSentenceSegmentation(
                SentenceSegmentation.newBuilder()
                  .setUuid(generateUUID())
                  .setMetadata(meta)
                  .addAllSentence(sentences))))
      .build()
  }

  def getSentencesFromStanfordXML(doc: xml.Elem, meta: AnnotationMetadata): Seq[Sentence] = {
    val sentences = doc \\ "sentence"
    sentences.map {sentence =>
      val tokens = sentence \\ "token"
      val concreteTokens = tokens.zipWithIndex.map {case (token, i) => {
        val form = (token \\ "word").text
        // char offsets (relative to start of document, 0-indexed)
        val start = (token \\ "CharacterOffsetBegin").text.toInt
        val end = (token \\ "CharacterOffsetEnd").text.toInt
        Token.newBuilder()
          .setText(form)
          .setTokenIndex(i)  // 0-indexed
          .setTextSpan(TextSpan.newBuilder().setStart(start).setEnd(end).build())
          .build()
      }}
      val taggedTokens = tokens.zipWithIndex.map({ case (token, i) =>
        TaggedToken.newBuilder()
          .setTokenIndex(i)
          .setTag((token \\ "POS").text)
          .build()
      })
      val tagging = TokenTagging.newBuilder()
        .setUuid(generateUUID())
        .setMetadata(meta)
        .addAllTaggedToken(taggedTokens)
        .build()
      val tokenization = Tokenization.newBuilder()
        .setUuid(generateUUID())
        .setMetadata(meta)
        .setKind(Tokenization.Kind.TOKEN_LIST)
        .addAllToken(concreteTokens)
        .addPosTags(tagging)
        .build()
      Sentence.newBuilder()
        .setUuid(generateUUID())
        .addTokenization(tokenization)
        .setTextSpan(TextSpan.newBuilder()
          .setStart(concreteTokens.head.getTextSpan.getStart)
          .setEnd(concreteTokens.last.getTextSpan.getEnd))
        .build()
    }
  }
}
