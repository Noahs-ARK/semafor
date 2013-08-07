package edu.cmu.cs.lti.ark.fn.data.prep.concrete

import Util.{getAllSentences, setSentences}
import edu.jhu.hlt.concrete.Concrete._
import java.io.File
import edu.jhu.hlt.concrete.util.IdUtil.generateUUID
import scala.collection.JavaConversions._
import edu.cmu.cs.lti.ark.fn.data.prep.formats
import edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec.ConllCodec
import org.joda.time.{DateTimeZone, DateTime}
import edu.jhu.hlt.concrete.Concrete
import edu.jhu.hlt.concrete.Concrete.DependencyParse.Dependency
import com.google.common.io.Files
import com.google.common.base.Charsets

object MaltToConcrete {
  def addMaltAnnotations(communication: Communication, preprocessedDir: File): Communication = {
    val meta = AnnotationMetadata.newBuilder()
      .setTool("org.maltparser:maltparser:1.7.2,org.maltparser:engmalt.linear:1.7")
      .setTimestamp(new DateTime(DateTimeZone.UTC).getMillis)
      .build()
    val concreteSentences = getAllSentences(communication)
    val maltParsedFile = new File(new File(preprocessedDir, communication.getGuid.getCommunicationId), "conll")
    println("malt file: " + maltParsedFile.getCanonicalPath)
    val maltParsedSentences = ConllCodec.readInput(Files.newReader(maltParsedFile, Charsets.UTF_8))
    try {
      val maltParsedSentencesConcrete = (maltParsedSentences.toList zip concreteSentences).map({
        case (depParse, sentence) => depParseToConcrete(depParse, sentence, meta)
      })
      setSentences(communication, maltParsedSentencesConcrete)
    } finally {
      maltParsedSentences.close()
    }
  }

  def depParseToConcrete(maltSentence: formats.Sentence,
                         concreteSentence: Sentence,
                         meta: AnnotationMetadata): Concrete.Sentence = {
    val dependencies = maltSentence.getTokens.zipWithIndex.map({ case (token, i) =>
      val builder = Dependency.newBuilder()
        .setDep(i)
        .setEdgeType(token.getDeprel)
      if (token.getHead > 0) builder.setGov(token.getHead - 1)
      builder.build()
    })
    val depParse = DependencyParse.newBuilder()
      .setUuid(generateUUID())
      .setMetadata(meta)
      .addAllDependency(dependencies)
      .build()
    val newTokenization = concreteSentence.getTokenization(0).toBuilder
      .addDependencyParse(depParse)
      .build()
    concreteSentence.toBuilder
      .setTokenization(0, newTokenization)
      .build()
  }
}
