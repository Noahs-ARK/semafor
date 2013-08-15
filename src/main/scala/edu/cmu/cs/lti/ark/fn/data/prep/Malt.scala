package edu.cmu.cs.lti.ark.fn.data.prep

import edu.cmu.cs.lti.ark.fn.data.prep.formats.{Token,Sentence}
import org.maltparser.MaltParserService
import scala.collection.JavaConversions._
import java.io.File

/**
 * Thin wrapper around org.maltparser.MaltParserService
 *
 * @author sthomson@cs.cmu.edu
 */
class Malt(modelDir: File) {
  val modelName = "engmalt.linear-1.7"

  val processor: MaltParserService = {
    val malt = new MaltParserService()
    malt.initializeParserModel("-w %s -c %s".format(modelDir.getCanonicalPath, modelName))
    malt
  }

  def parse(input: Sentence): Sentence = {
    val tokens = input.getTokens
    // maltparse
    val outputGraph = processor.parse(tokens.map(_.toConll).toArray)
    // convert back to a formats.Sentence
    val deprelTable = outputGraph.getSymbolTables.getSymbolTable("DEPREL")
    val rootLabel = outputGraph.getDefaultRootEdgeLabelSymbol(deprelTable)
    val dependencyNodes = outputGraph.getTokenIndices.view
      .filter(_ <= tokens.length)
      .map(outputGraph.getDependencyNode(_))
    val parsedTokens = (tokens zip dependencyNodes).map({case (token, node) => {
      val head = node.getHead.getIndex
      val arc = node.getHeadEdge
      val deprel = if (arc.hasLabel(deprelTable)) arc.getLabelSymbol(deprelTable) else rootLabel
      token.withHead(head).withDeprel(deprel)
    }})
    new Sentence(parsedTokens)
  }
}
