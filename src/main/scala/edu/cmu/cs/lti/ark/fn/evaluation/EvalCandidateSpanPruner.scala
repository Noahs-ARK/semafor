package edu.cmu.cs.lti.ark.fn.evaluation

import com.google.common.base.Charsets.UTF_8
import com.google.common.io.Files

import edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec.ConllCodec
import edu.cmu.cs.lti.ark.fn.parsing.{CandidateSpanPruner, RankedScoredRoleAssignment}
import CandidateSpanPruner.spanToString

import java.io.File

import resource.managed

import scala.collection.JavaConversions._
import scala.io.{Codec, Source}

// "training/data/naacl2012/cv.train.sentences.frame.elements"
// "training/data/naacl2012/cv.train.sentences.turboparsed.conll"
object EvalCandidateSpanPrunerApp extends App {

  private val frameElementsFilename = args(0)
  private val depParseFilename = args(1)

  EvalCandidateSpanPruner.score(new File(frameElementsFilename), new File(depParseFilename), verbose = true)
}

object EvalCandidateSpanPruner {
  def score(frameElementsFile: File, depParseFile: File, verbose: Boolean = true): (Float, Float, Float) = {
    val pruner = new CandidateSpanPruner(true, true)
    val sentenceInput = managed(ConllCodec.readInput(Files.newReaderSupplier(depParseFile, UTF_8).getInput))
    val sentences = sentenceInput.acquireAndGet(_.toArray)
    var currentSentenceIdx = -1
    var numTruePositives = 0
    var numPredicted = 0
    var numGold = 0
    for (
      frameElementsInput <- managed(Source.fromFile(frameElementsFile)(Codec.UTF8));
      roleAssignment <- frameElementsInput.getLines().map(RankedScoredRoleAssignment.fromLine);
      sentenceIdx = roleAssignment.sentenceIdx;
      sentence = sentences(sentenceIdx);
      predictedSpansForFrame = pruner.candidateSpans(sentence, roleAssignment.targetSpan).toSet
    ) {
      if (verbose && sentenceIdx != currentSentenceIdx) {
        // only print dep parse for the first frame of a sentence
        println("Dependency Parse:")
        println(ConllCodec.encode(sentence))
        currentSentenceIdx = sentenceIdx
      }
      numPredicted += predictedSpansForFrame.size - 1  // don't count the null span
      numGold += roleAssignment.fesAndSpans.length
      val (presentSpans, missingSpans) = roleAssignment.fesAndSpans.partition {
        goldSpan => predictedSpansForFrame.contains(goldSpan.span)
      }
      numTruePositives += presentSpans.length
      if (verbose && missingSpans.nonEmpty) {
        println("Frame: " + roleAssignment.frame + "\t" + spanToString(roleAssignment.targetSpan, sentence))
        println("Missing spans:")
        for (goldFeAndSpan <- missingSpans) {
          println(goldFeAndSpan.name + "\t" + spanToString(goldFeAndSpan.span, sentence))
        }
        println("correct: " + presentSpans.length)
        println("predicted: " + (predictedSpansForFrame.size - 1))
        println("gold: " + roleAssignment.fesAndSpans.length)
        println("")
      }
    }
    println("Total correct: " + numTruePositives)
    println("Total predicted: " + numPredicted)
    println("Total gold: " + numGold)
    val (p, r, f) = precisionRecallF1(numTruePositives, numPredicted, numGold)
    println("p: " + p + ", r: " + r + ", f1: " + f)
    (p, r, f)
  }

  def precisionRecallF1(numTruePositives: Float, numPredicted: Float, numGold: Float): (Float, Float, Float) = {
    if (numTruePositives == 0f || numPredicted == 0f || numGold == 0f) {
      (0f, 0f, 0f)
    } else {
      val p = numTruePositives / numPredicted
      val r = numTruePositives / numGold
      val f = (2 * p * r) / (p + r)
      (p, r, f)
    }
  }
}
