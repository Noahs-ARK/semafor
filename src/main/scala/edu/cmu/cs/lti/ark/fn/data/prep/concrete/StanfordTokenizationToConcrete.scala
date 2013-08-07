package edu.cmu.cs.lti.ark.fn.data.prep.concrete

import scala.xml.{XML, Elem}
import scala.io.Source
import scala.collection.JavaConversions._
import java.io.FileOutputStream
import java.io.File
import edu.jhu.hlt.concrete.Concrete._
import edu.jhu.hlt.concrete.util.IdUtil.generateUUID
import resource.managed
import org.joda.time.{DateTimeZone, DateTime}

object StanfordTokenizationToConcrete {
  def main(args: Array[String]) {
    val Array(originalFile, stanfordXmlFile, corpusName, communicationId, outFile) = args

    val communication = getCommunicationFromFile(new File(originalFile), new File(stanfordXmlFile), corpusName, communicationId)
    for (outputStream <- managed(new FileOutputStream(outFile))) {
      communication.writeDelimitedTo(outputStream)
    }
  }

  def convertAll() {
    val baseDir = new File("/Users/sam/Documents/CMU/research/DEFT/Firestone_texts_semafor_parsed/")
    val originalDir = new File(baseDir, "original")
    val outFile = new File(baseDir, "firestone_all_tokenized.pb")
    val preprocessedDir = new File(baseDir, "preprocessed")
    for (outputStream <- managed(new FileOutputStream(outFile))) {
      for (dir <- preprocessedDir.listFiles) {
        val originalFile = new File(originalDir, dir.getName + ".xml")
        val stanfordFile = new File(dir, "stanford.xml")
        val c = getCommunicationFromFile(originalFile, stanfordFile, "Firestone", dir.getName)
        c.writeDelimitedTo(outputStream)
      }
    }
  }

  def getCommunicationFromFile(originalFile: File,
                               stanfordFile: File,
                               corpusName: String,
                               communicationId: String): Communication = {
    val originalText = Source.fromFile(originalFile, "utf-8").mkString
    val doc = XML.loadFile(stanfordFile)
    val guid = CommunicationGUID.newBuilder()
      .setCorpusName(corpusName)
      .setCommunicationId(communicationId)
      .build()
    val meta = AnnotationMetadata.newBuilder()
      .setTool("edu.stanford.nlp:stanford-corenlp:3.2.0")
      .setTimestamp(new DateTime(DateTimeZone.UTC).getMillis)
      .build()
    val sentenceSegmentation = SentenceSegmentation.newBuilder()
      .setUuid(generateUUID())
      .setMetadata(meta)
      .addAllSentence(getSentencesFromStanfordXML(doc, meta))
      .build()
    val section = Section.newBuilder()
      .setUuid(generateUUID())
      .setKind(Section.Kind.PASSAGE)
      .addSentenceSegmentation(sentenceSegmentation)
      .build()
    val sectionSegmentation = SectionSegmentation.newBuilder()
      .setUuid(generateUUID())
      .addSection(section)
      .build()
    Communication.newBuilder()
      .setUuid(generateUUID())
      .setGuid(guid)
      .setKind(Communication.Kind.NEWS)
      .setText(originalText)
      .addSectionSegmentation(sectionSegmentation)
      .build()
  }

  def getSentencesFromStanfordXML(doc: Elem, meta: AnnotationMetadata): List[Sentence] = {
    val sentences = doc \\ "sentence"
    sentences.map {sentence =>
      val tokens = sentence \\ "token"
      val concreteTokens = tokens.zipWithIndex.map {case (token, i) => {
        val form = (token \\ "word").text
        val start = (token \\ "CharacterOffsetBegin").text.toInt
        val end = (token \\ "CharacterOffsetEnd").text.toInt
        Token.newBuilder()
          .setText(form)
          .setTokenIndex(i)
          .setTextSpan(TextSpan.newBuilder().setStart(start).setEnd(end).build())
          .build()
      }}
      val tokenization = Tokenization.newBuilder()
        .setUuid(generateUUID())
        .setMetadata(meta)
        .setKind(Tokenization.Kind.TOKEN_LIST)
        .addAllToken(concreteTokens)
        .build()
      Sentence.newBuilder()
        .setUuid(generateUUID())
        .addTokenization(tokenization)
        .setTextSpan(TextSpan.newBuilder()
          .setStart(concreteTokens.head.getTextSpan.getStart)
          .setEnd(concreteTokens.last.getTextSpan.getEnd))
        .build()
    }.toList
  }
}
