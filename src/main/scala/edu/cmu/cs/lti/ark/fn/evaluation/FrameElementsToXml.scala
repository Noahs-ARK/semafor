package edu.cmu.cs.lti.ark.fn.evaluation

import java.io.File
import java.util.Date

import edu.cmu.cs.lti.ark.fn.parsing.RankedScoredRoleAssignment
import edu.cmu.cs.lti.ark.fn.utils.DataPoint
import edu.cmu.cs.lti.ark.fn.utils.DataPoint.getCharSpan
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements.FrameElementAndSpan
import edu.cmu.cs.lti.ark.util.XmlUtils
import edu.cmu.cs.lti.ark.util.XmlUtils.{addAttribute, writeXML}
import edu.cmu.cs.lti.ark.util.ds.Range0Based
import gnu.trove.THashMap
import org.w3c.dom.{Document, Element}
import resource.managed

import scala.io.{Codec, Source}


object FrameElementsToXml {
  /**
   * Generates the XML representation of a set of predicted semantic parses so evaluation
   * can be performed (with SemEval Perl scripts)
   *
   * @param args Options to specify:
   *             testFEPredictionsFile
   *             startIndex
   *             endIndex
   *             testParseFile
   *             testTokenizedFile
   *             outputFile
   */
  def main(args: Array[String]) {
    val options = new ParseOptions(args)
    generateXMLForPrediction(
      options.testFEPredictionsFile,
      new Range0Based(options.startIndex, options.endIndex, false),
      new File(options.testTokenizedFile),
      new File(options.outputFile)
    )
  }

  /**
   * Generates the XML representation of a set of predicted semantic parses so evaluation
   * can be performed (with SemEval Perl scripts)
   *
   * @param frameElementsFile frame elements lines
   * @param idxs Range of sentences to include (0-based)
   * @param testTokenizedFile File Original form of each sentence in the data
   * @param outputFile Where to store the resulting XML
   */
  def generateXMLForPrediction(frameElementsFile: String,
                               idxs: Range0Based,
                               testTokenizedFile: File,
                               outputFile: File) {
    for (
      tokenizedSource <- managed(Source.fromFile(testTokenizedFile)(Codec.UTF8));
      feSource <- managed(Source.fromFile(frameElementsFile)(Codec.UTF8))
    ) {
      val tokenizedLines = tokenizedSource.getLines()
          .slice(idxs.start, idxs.start + idxs.length)
          .map(_.trim)
          .toArray
      val roleAssignmentsBySentenceIdx = feSource.getLines()
          .map(RankedScoredRoleAssignment.fromLine)
          .filter(x => idxs.contains(x.sentenceIdx))
          .toArray
          .groupBy(_.sentenceIdx)
      val doc = createXMLDoc(roleAssignmentsBySentenceIdx, idxs, tokenizedLines)
      writeXML(outputFile.getAbsolutePath, doc)
    }
  }

  /**
   * Given several parallel lists of predicted frame instances, including their frame elements, create an XML file
   * for the full-text annotation predicted by the model.
   * @param roleAssignmentsBySentenceIdx frames & FEs
   * @param sentenceNums Global sentence number for the first sentence being predicted, so as to map FE lines to items in parses/orgLines
   * @param tokenizedLines The original sentences, tokenized
   */
  private def createXMLDoc(roleAssignmentsBySentenceIdx: Map[Int, Array[RankedScoredRoleAssignment]],
                           sentenceNums: Range0Based,
                           tokenizedLines: Array[String]): Document = {
    val doc = XmlUtils.getNewDocument
    val corpus = doc.createElement("corpus")
    addAttribute(doc, "ID", corpus, "100")
    addAttribute(doc, "name", corpus, "ONE")
    addAttribute(doc, "XMLCreated", corpus, new Date().toString)
    val documents = doc.createElement("documents")
    corpus.appendChild(documents)
    val documentElt = doc.createElement("document")
    addAttribute(doc, "ID", documentElt, "1")
    addAttribute(doc, "description", documentElt, "TWO")
    documents.appendChild(documentElt)
    val paragraphs = doc.createElement("paragraphs")
    documentElt.appendChild(paragraphs)
    val paragraph = doc.createElement("paragraph")
    addAttribute(doc, "ID", paragraph, "2")
    addAttribute(doc, "documentOrder", paragraph, "1")
    paragraphs.appendChild(paragraph)
    val sentences = doc.createElement("sentences")
    for (sentIdx <- sentenceNums.start until (sentenceNums.start + sentenceNums.length)) {
      val roleAssignmentsForSentence = roleAssignmentsBySentenceIdx.getOrElse(sentIdx, Array())
      val idxInDoc = sentIdx - sentenceNums.start
      val origLine = tokenizedLines(idxInDoc)
      sentences.appendChild(createSentenceElt(doc, idxInDoc, roleAssignmentsForSentence, origLine))
    }
    paragraph.appendChild(sentences)
    doc.appendChild(corpus)
    doc
  }

  def createSentenceElt(doc: Document, 
                        sentenceIdx: Int,
                        roleAssignmentsForSentence: Array[RankedScoredRoleAssignment], 
                        origLine: String): Element = {
    val sentence = doc.createElement("sentence")
    addAttribute(doc, "ID", sentence, "" + sentenceIdx)
    val text = doc.createElement("text")
    val textNode = doc.createTextNode(origLine)
    text.appendChild(textNode)
    sentence.appendChild(text)
    sentence.appendChild(createTokensElt(doc, origLine))

    val annotationSets = doc.createElement("annotationSets")
    val charOffsets = DataPoint.getCharOffsetsOfTokens(origLine)

    val roleAssignmentsByTarget = roleAssignmentsForSentence.groupBy(_.targetSpan)
    for ((roleAssignments, frameIdx) <- roleAssignmentsByTarget.values.zipWithIndex) {
      // if a target is annotated more than once, use the most fully-annotated roleAssignment
      val roleAssignment = roleAssignments.maxBy(_.fesAndSpans.length)
      val annotationSet =
        buildAnnotationSet(
          roleAssignment.frame,
          Array(roleAssignment),
          charOffsets,
          doc,
          sentenceIdx,
          frameIdx
        )
      annotationSets.appendChild(annotationSet)
    }
    sentence.appendChild(annotationSets)
    sentence
  }

  def createTokensElt(doc: Document, origLine: String): Element = {
    val result = doc.createElement("tokens")
    val tokens = origLine.trim.split(" ")
    for (i <- 0 until tokens.length) {
      val tokenElt = doc.createElement("token")
      addAttribute(doc, "index", tokenElt, "" + i)
      val tokenNode = doc.createTextNode(tokens(i))
      tokenElt.appendChild(tokenNode)
      result.appendChild(tokenElt)
    }
    result
  }

  def buildAnnotationSet(frameName: String,
                         roleAssignments: Array[RankedScoredRoleAssignment],
                         charOffsets: THashMap[Integer, Range0Based],
                         doc: Document,
                         parentId: Int,
                         frameIdx: Int): Element = {
    val result = doc.createElement("annotationSet")
    val annotationSetId = parentId * 100 + frameIdx
    addAttribute(doc, "ID", result, "" + annotationSetId)
    addAttribute(doc, "frameName", result, frameName)
    val layers = doc.createElement("layers")

    val targetLayer = doc.createElement("layer")
    val targetLayerId = annotationSetId * 100 + 1
    addAttribute(doc, "ID", targetLayer, "" + targetLayerId)
    addAttribute(doc, "name", targetLayer, "Target")
    val targetLabels = doc.createElement("labels")
    val firstRoleAssignment = roleAssignments(0)
    val target = new FrameElementAndSpan("Target", firstRoleAssignment.targetSpan)
    for ((namedSpan, i) <- Array(target).zipWithIndex) {
      val labelId = targetLayerId * 100 + (i * 3) + 1
      targetLabels.appendChild(createLabelElt(doc, labelId, namedSpan, charOffsets))
    }
    targetLayer.appendChild(targetLabels)
    layers.appendChild(targetLayer)

    for (roleAssignment <- roleAssignments) {
      val rank = 1 // NB: fnSemScore.pl ignores anything with rank != 1
      val layerId = annotationSetId * 100 + rank + 1
      val feLayer = doc.createElement("layer")
      addAttribute(doc, "ID", feLayer, "" + layerId)
      addAttribute(doc, "name", feLayer, "FE")
      addAttribute(doc, "kbestRank", feLayer, "" + rank)
      addAttribute(doc, "score", feLayer, "" + roleAssignment.score)
      layers.appendChild(feLayer)
      val labels = doc.createElement("labels")
      feLayer.appendChild(labels)

      val fesAndSpans = roleAssignment.fesAndSpans
      for ((namedSpan, i) <- fesAndSpans.zipWithIndex) {
        val labelId = layerId * 100 + i + 1
        labels.appendChild(createLabelElt(doc, labelId, namedSpan, charOffsets))
      }
    }
    result.appendChild(layers)
    result
  }

  def createLabelElt(doc: Document, labelId: Int, namedSpan: FrameElementAndSpan, charOffsets: THashMap[Integer, Range0Based]): Element = {
    val tokenSpan = namedSpan.span
    val charSpan = getCharSpan(tokenSpan, charOffsets)
    val labelElt: Element = doc.createElement("label")
    addAttribute(doc, "ID", labelElt, "" + labelId)
    addAttribute(doc, "name", labelElt, namedSpan.name)
    addAttribute(doc, "start", labelElt, "" + charSpan.start)
    addAttribute(doc, "end", labelElt, "" + charSpan.end)
    addAttribute(doc, "tokenStart", labelElt, "" + tokenSpan.start)
    addAttribute(doc, "tokenEnd", labelElt, "" + tokenSpan.end)
    labelElt
  }
}
