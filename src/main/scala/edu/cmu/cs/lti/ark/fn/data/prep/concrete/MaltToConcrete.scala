package edu.cmu.cs.lti.ark.fn.data.prep.concrete

import scala.collection.JavaConversions._
import Utils.{getAllSentences, setSentences}
import edu.cmu.cs.lti.ark.fn.data.prep.Malt
import edu.cmu.cs.lti.ark.fn.data.prep.formats
import edu.jhu.hlt.concrete.Concrete
import edu.jhu.hlt.concrete.util.IdUtil.generateUUID
import org.joda.time.{DateTimeZone, DateTime}

object MaltToConcrete {
  val toolName = "org.maltparser:maltparser:1.7.2,org.maltparser:engmalt.linear:1.7"

  def addMaltAnnotations(maltParser: Malt, communication: Concrete.Communication): Concrete.Communication = {
    val meta = Concrete.AnnotationMetadata.newBuilder()
      .setTool(toolName)
      .setTimestamp(new DateTime(DateTimeZone.UTC).getMillis)
      .build()
    val concretePosTaggedSentences = getAllSentences(communication).toIterable
    val concreteMaltParsedSentences = concretePosTaggedSentences.map(concreteSentence => {
      val posTaggedSentence = sentenceFromPosTaggedConcrete(concreteSentence)
      val maltParsedSentence = maltParser.parse(posTaggedSentence)
      depParseToConcrete(maltParsedSentence, concreteSentence, meta)
    })
    setSentences(communication, concreteMaltParsedSentences)
  }

  def sentenceFromPosTaggedConcrete(sentence: Concrete.Sentence): formats.Sentence = {
    val tokenization = sentence.getTokenization(0)
    val words = tokenization.getTokenList.map(_.getText)
    val postags = tokenization.getPosTags(0).getTaggedTokenList.map(_.getTag)
    val tokens = (words zip postags).zipWithIndex.map {
      case ((word, postag), i) => new formats.Token(word, postag).withId(i + 1)
    }
    new formats.Sentence(tokens)
  }

  def depParseToConcrete(maltSentence: formats.Sentence,
                         concreteSentence: Concrete.Sentence,
                         meta: Concrete.AnnotationMetadata): Concrete.Sentence = {
    val dependencies = maltSentence.getTokens.zipWithIndex.map({ case (token, i) =>
      val dependency = Concrete.DependencyParse.Dependency.newBuilder()
        .setDep(i)  // 0-indexed
        .setEdgeType(token.getDeprel)
      if (token.getHead > 0) dependency.setGov(token.getHead - 1)  // 0-indexed
      dependency.build()
    })
    val depParse = Concrete.DependencyParse.newBuilder()
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

  def depParseFromConcrete(sentence: Concrete.Sentence): formats.Sentence = {
    val posTaggedTokens: List[formats.Token] = sentenceFromPosTaggedConcrete(sentence).getTokens.toList
    val dependencies = sentence.getTokenization(0).getDependencyParse(0).getDependencyList.sortBy(_.getDep).toList
    val depParsedTokens = dependencies.zip(posTaggedTokens).map({ case (dep, token) => {
      val head = if (dep.hasGov) dep.getGov + 1 else 0 // 1-indexed, 0 signifies root
      new formats.Token(
        token.getId,
        token.getForm,
        token.getLemma,
        token.getCpostag,
        token.getPostag,
        token.getFeats,
        head,
        dep.getEdgeType,
        token.getPhead,
        token.getPdeprel)
    }})
    new formats.Sentence(depParsedTokens)
  }
}
