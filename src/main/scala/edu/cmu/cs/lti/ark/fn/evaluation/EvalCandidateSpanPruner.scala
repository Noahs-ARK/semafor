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

/**
 * For example:
 * > java -classpath ${CLASSPATH} \
 *     edu.cmu.cs.lti.ark.fn.evaluation.EvalCandidateSpanPrunerApp \
 *     ${training_dir}/cv.train.sentences.frame.elements \
 *     ${training_dir}/cv.train.sentences.turbo3rdparsed.conll \
 *     1 \
 *     1 \
 *     1 \
 *     1 \
 *     0 \
 *     1
 */
object EvalCandidateSpanPrunerApp extends App {
  val trueOptions = Set("true", "1")

  private val frameElementsFile = new File(args(0))
  private val depParseFile = new File(args(1))
  private val doStripPunctuation: Boolean = trueOptions.contains(args(2))
  private val doIncludeContiguousSubspans: Boolean = trueOptions.contains(args(3))
  private val doIncludeTarget: Boolean = trueOptions.contains(args(4))
  private val doIncludeSpansMinusTarget: Boolean = trueOptions.contains(args(5))
  private val doStripPPs: Boolean = trueOptions.contains(args(6))
  private val doFindNNModifiers: Boolean = trueOptions.contains(args(7))
  private val maxLength: Option[Int] = if (args.length > 8) Some(Integer.parseInt(args(8))) else None

  val pruner: CandidateSpanPruner = new CandidateSpanPruner(
    doStripPunctuation = doStripPunctuation,
    doIncludeContiguousSubspans = doIncludeContiguousSubspans,
    doIncludeTarget = doIncludeTarget,
    doIncludeSpansMinusTarget = doIncludeSpansMinusTarget,
    doStripPPs = doStripPPs,
    doFindNNModifiers = doFindNNModifiers,
    maxLength = maxLength
  )

  EvalCandidateSpanPruner.score(
    frameElementsFile,
    depParseFile,
    pruner,
    verbose = true
  )
}

object EvalCandidateSpanPruner {
  def score(frameElementsFile: File,
            depParseFile: File,
            pruner: CandidateSpanPruner,
            verbose: Boolean): (Float, Float, Float) = {
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
