package edu.cmu.cs.lti.ark.fn.data.prep.concrete

import Utils.{getAllSentences, setSentences}
import edu.jhu.hlt.concrete.Concrete._
import java.io.File
import edu.jhu.hlt.concrete.util.IdUtil.generateUUID
import scala.collection.JavaConversions._
import edu.cmu.cs.lti.ark.fn.data.prep.formats
import edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec.PosTaggedCodec
import edu.jhu.hlt.concrete.Concrete.TokenTagging.TaggedToken
import org.joda.time.{DateTimeZone, DateTime}
import scala.io.Source
import edu.jhu.hlt.concrete.Concrete

object MxpostToConcrete {
  def addMxpostAnnotations(communication: Communication, preprocessedDir: File): Communication = {
    val meta = AnnotationMetadata.newBuilder()
      .setTool("mxpost")
      .setTimestamp(new DateTime(DateTimeZone.UTC).getMillis)
      .build()
    val concreteSentences = getAllSentences(communication)
    val posTaggedFile = new File(new File(preprocessedDir, communication.getGuid.getCommunicationId), "pos.tagged")
    val mxpostSentences = Source.fromFile(posTaggedFile, "utf-8").getLines().map(PosTaggedCodec.decode).toList
    val mxpostSentencesConcrete = (mxpostSentences zip concreteSentences).map({
      case (posTagging, sentence) => posTagToConcrete(posTagging, sentence, meta)
    })
    setSentences(communication, mxpostSentencesConcrete)
  }

  def posTagToConcrete(posTagging: formats.Sentence,
                       concreteSentence: Sentence,
                       meta: AnnotationMetadata): Concrete.Sentence = {
    val tokenization = concreteSentence.getTokenization(0)
    val taggedTokens = (posTagging.getTokens zip tokenization.getTokenList).map({
      case (mxToken, concreteToken) => TaggedToken.newBuilder()
        .setTokenIndex(concreteToken.getTokenIndex)
        .setTag(mxToken.getPostag)
        .build()
    })
    val tagging = TokenTagging.newBuilder()
      .setUuid(generateUUID())
      .setMetadata(meta)
      .addAllTaggedToken(taggedTokens)
      .build()
    val newTokenization = tokenization.toBuilder
      .addPosTags(tagging)
      .build()
    concreteSentence.toBuilder
      .setTokenization(0, newTokenization)
      .build()
  }
}
