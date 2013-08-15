package edu.cmu.cs.lti.ark.fn.data.prep

import java.util.Properties
import edu.stanford.nlp.pipeline.{XMLOutputter, StanfordCoreNLP}
import scala.xml.{Elem, XML}

/**
 * Thin wrapper around edu.stanford.nlp.pipeline.StanfordCoreNLP
 *
 * @author sthomson@cs.cmu.edu
 */
class StanfordProcessor(annotators: Iterable[String]) {
  private[this] val processor: StanfordCoreNLP = {
    val props = new Properties()
    props.setProperty("annotators", annotators.mkString(","))
    new StanfordCoreNLP(props)
  }

  def processToXml(input: String): Elem = {
    val annotation = processor.process(input)
    XML.loadString(XMLOutputter.annotationToDoc(annotation, processor).toXML)
  }
}

object StanfordProcessor {
  val defaultAnnotators = Array("tokenize", "cleanxml", "ssplit", "pos")

  def defaultInstance = new StanfordProcessor(defaultAnnotators)
}
