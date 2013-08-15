package edu.cmu.cs.lti.ark.fn.data.prep.concrete

import edu.jhu.hlt.concrete.Concrete._
import java.io.File
import edu.jhu.hlt.concrete.util.IdUtil.generateUUID
import scala.collection.JavaConversions._

object Utils {
  val baseDir = new File("/Users/sam/Documents/CMU/research/DEFT/Firestone_texts_semafor_parsed/")
  val originalDir = new File(baseDir, "original")
  val rawConcreteFile = new File(baseDir, "firestone_all_raw.pb")
  val semaforParsedFile = new File(baseDir, "firestone_all_semafor_parsed.pb")

  def getAllSentences(communication: Communication): List[Sentence] = {
    communication.getSectionSegmentationList
      .flatMap(_.getSectionList)
      .flatMap(_.getSentenceSegmentationList)
      .flatMap(_.getSentenceList).toList
  }

  def setSentences(communication: Communication, newSentences: Iterable[Sentence]): Communication = {
    val sectionSegmentation = communication.getSectionSegmentation(0)
    val section = sectionSegmentation.getSection(0)
    val newSentenceSegmentation = section.getSentenceSegmentation(0).toBuilder
    newSentences.zipWithIndex.foreach { case (sentence, i) => newSentenceSegmentation.setSentence(i, sentence)}
    val newSection = section.toBuilder.setSentenceSegmentation(0, newSentenceSegmentation.build()).build()
    val newSectionSegmentation = sectionSegmentation.toBuilder.setSection(0, newSection).build()
    communication.toBuilder
      .setUuid(generateUUID())
      .setSectionSegmentation(0, newSectionSegmentation)
      .build()
  }
}
