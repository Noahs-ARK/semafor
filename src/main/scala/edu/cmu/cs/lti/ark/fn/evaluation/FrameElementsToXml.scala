package edu.cmu.cs.lti.ark.fn.evaluation

import java.io.File
import java.util.Date

import com.google.common.base.Charsets
import com.google.common.io.Files
import edu.cmu.cs.lti.ark.fn.parsing.RankedScoredRoleAssignment
import edu.cmu.cs.lti.ark.fn.utils.DataPoint.{getCharOffsetsOfTokens, getCharSpan}
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements.FrameElementAndSpan
import edu.cmu.cs.lti.ark.util.ds.Range0Based
import gnu.trove.THashMap
import resource.managed

import scala.io.{Codec, Source}
import scala.xml.{Elem, NodeSeq, PrettyPrinter}


object FrameElementsToXmlApp {
  def main(args: Array[String]) {
    val options = new ParseOptions(args)
    FrameElementsToXml.generateXMLForPrediction(
      options.testFEPredictionsFile,
      new Range0Based(options.startIndex, options.endIndex, false),
      new File(options.testTokenizedFile),
      new File(options.outputFile)
    )
  }
}

object FrameElementsToXml {
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
      feSource <- managed(Source.fromFile(frameElementsFile)(Codec.UTF8));
      output <- managed(Files.newWriter(outputFile, Charsets.UTF_8))
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
      val doc = createXmlDoc(idxs, roleAssignmentsBySentenceIdx, tokenizedLines)
      val docStr = new PrettyPrinter(1000, 2).format(doc)
      output.write("<?xml version='1.0' encoding='UTF-8'?>\n")
      output.write(docStr)
    }
  }

  /**
   * Given several parallel lists of predicted frame instances, including their frame elements, create an XML file
   * for the full-text annotation predicted by the model.
   * @param sentenceIdxs Global sentence number for the sentences being predicted,
   *                     to map FE lines to items in parses/orgLines
   * @param roleAssignmentsBySentenceIdx frames & FEs
   * @param tokenizedLines The original sentences, tokenized
   */
  def createXmlDoc(sentenceIdxs: Range0Based,
                   roleAssignmentsBySentenceIdx: Map[Int, Array[RankedScoredRoleAssignment]],
                   tokenizedLines: Array[String]): Elem = {
    val sentences: NodeSeq = for (
      sentIdx <- sentenceIdxs.start until (sentenceIdxs.start + sentenceIdxs.length)
    ) yield {
      val roleAssignmentsForSentence = roleAssignmentsBySentenceIdx.getOrElse(sentIdx, Array())
      val idxInDoc = sentIdx - sentenceIdxs.start
      val origLine = tokenizedLines(idxInDoc)
      createSentence(idxInDoc, roleAssignmentsForSentence, origLine)
    }

    <corpus ID="100" name="ONE" XMLCreated={new Date().toString}>
      <documents>
        <document ID="1" description="TWO">
          <paragraphs>
            <paragraph ID="2" documentOrder="1">
              <sentences>
                {sentences}
              </sentences>
            </paragraph>
          </paragraphs>
        </document>
      </documents>
    </corpus>
  }

  def createSentence(sentenceIdx: Int,
                     roleAssignmentsForSentence: Array[RankedScoredRoleAssignment],
                     origLine: String): Elem = {
    val tokenElems: NodeSeq = origLine.trim.split(" ").toSeq.zipWithIndex.map { case (token, i) =>
      <token index={i.toString}>{token}</token>
    }
    val charOffsets = getCharOffsetsOfTokens(origLine)
    val roleAssignmentsByTarget = roleAssignmentsForSentence.groupBy(_.targetSpan)
    val annotationSets: NodeSeq = for (
      (roleAssignments, frameIdx) <- roleAssignmentsByTarget.values.toSeq.zipWithIndex
    ) yield {
      // if a target is annotated more than once, use the most fully-annotated roleAssignment
      val roleAssignment = roleAssignments.maxBy(_.fesAndSpans.length)
      createAnnotationSet(
        roleAssignment.frame,
        Array(roleAssignment),
        charOffsets,
        sentenceIdx,
        frameIdx
      )
    }

    <sentence ID={sentenceIdx.toString}>
      <text>{origLine}</text>
      <tokens>
        {tokenElems}
      </tokens>
      <annotationSets>
        {annotationSets}
      </annotationSets>
    </sentence>
  }

  def createAnnotationSet(frameName: String,
                          roleAssignments: Array[RankedScoredRoleAssignment],
                          charOffsets: THashMap[Integer, Range0Based],
                          parentId: Int,
                          frameIdx: Int): Elem = {
    val annotationSetId = parentId * 100 + frameIdx
    val targetLayer = {
      val targetLayerId = annotationSetId * 100 + 1
      val target = new FrameElementAndSpan("Target", roleAssignments(0).targetSpan)
      val targetLabel = createLabel(targetLayerId * 100 + 1, target, charOffsets)

      <layer ID={targetLayerId.toString}
             name="Target">
        <labels>
          {targetLabel}
        </labels>
      </layer>
    }

    val feLayers: NodeSeq = for (roleAssignment <- roleAssignments.toSeq) yield {
      val rank = roleAssignment.rank
      val feLayerId = annotationSetId * 100 + rank + 1
      val feLabels: NodeSeq = for ((namedSpan, i) <- roleAssignment.fesAndSpans.toSeq.zipWithIndex) yield {
        createLabel(feLayerId * 100 + i + 1, namedSpan, charOffsets)
      }

      <layer ID={feLayerId.toString}
             name="FE"
             kbestRank={rank.toString}
             score={roleAssignment.score.toString}>
        <labels>
          {feLabels}
        </labels>
      </layer>
    }

    <annotationSet ID={annotationSetId.toString}
                   frameName={frameName}>
      <layers>
        {targetLayer}
        {feLayers}
      </layers>
    </annotationSet>
  }

  def createLabel(labelIdx: Int,
                  namedSpan: FrameElementAndSpan,
                  charOffsets: THashMap[Integer, Range0Based]): Elem = {
    val tokenSpan = namedSpan.span
    val charSpan = getCharSpan(tokenSpan, charOffsets)

    <label ID={labelIdx.toString}
           name={namedSpan.name}
           start={charSpan.start.toString}
           end={charSpan.end.toString}
           tokenStart={tokenSpan.start.toString}
           tokenEnd={tokenSpan.end.toString}/>
  }
}
