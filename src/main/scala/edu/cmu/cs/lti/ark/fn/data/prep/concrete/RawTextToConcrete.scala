package edu.cmu.cs.lti.ark.fn.data.prep.concrete

import java.io.{File, FileOutputStream}

import edu.jhu.hlt.concrete.Concrete._
import edu.jhu.hlt.concrete.util.IdUtil.generateUUID
import resource.managed

import scala.io.Source

object RawTextToConcrete {
  def main(args: Array[String]) {
    val Array(inputDir, corpusName, outFile) = args

    convertAndWriteAll(new File(inputDir), corpusName, new File(outFile))
  }

  def convertAndWriteAll(inputDir: File, corpusName: String, outFile: File) {
    val files = inputDir.listFiles
    val communications = files.toIterator.map { file =>
      getCommunicationFromTextFile(file, corpusName, stripExtension(file.getName))
    }
    for (outputStream <- managed(new FileOutputStream(outFile))) {
      communications.foreach(_.writeDelimitedTo(outputStream))
    }
  }

  def stripExtension(fileName: String): String = fileName.substring(0, fileName.lastIndexOf("."))

  def getCommunicationFromTextFile(inputFile: File, corpusName: String, communicationId: String): Communication = {
    val originalText = Source.fromFile(inputFile, "utf-8").mkString
    val guid = CommunicationGUID.newBuilder()
      .setCorpusName(corpusName)
      .setCommunicationId(communicationId)
      .build()
    Communication.newBuilder()
      .setUuid(generateUUID())
      .setGuid(guid)
      .setKind(Communication.Kind.NEWS)
      .setText(originalText)
      .build()
  }
}
