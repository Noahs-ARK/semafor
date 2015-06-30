package edu.cmu.cs.lti.ark.fn.parsing

import java.io.File
import java.util

import com.google.common.collect.{Lists, Queues}
import edu.cmu.cs.lti.ark.util.ds.Scored.scored
import edu.cmu.cs.lti.ark.util.ds.{Range0Based, Scored}
import resource._

import scala.collection.JavaConverters._
import scala.collection.{mutable => m}
import scala.io.Source


/**
 * @param weights an array of weights
 * @param index an index from feature name to index
 */
case class Model(weights: Array[Double], index: FeatureIndex) {
  /**
   * Calculates the sum of the weights of firing features.
   *
   * @param feats indexes of firing features
   * @return the sum of the weights of firing features
   */
  def score(feats: Array[Int]): Double = feats.map(weights).sum

  def convertToIdxs(featureSet: util.Iterator[String]): Array[Int] = featureSet.asScala.toArray.flatMap(index.get)
}
object Model {
  def fromFiles(modelFile: File, alphabetFile: File): Model = {
    val weights = managed(Source.fromFile(modelFile)).acquireAndGet(_.getLines().map(_.toDouble).toArray)
    Model(weights, FeatureIndex.fromFile(alphabetFile))
  }
}


/** Predict spans for roles */
trait ArgDecoding {
  def model: Model

  /**
   * Decode, respecting the constraint that arguments do not overlap.
   * Returns the (approximately?) best configurations of non-overlapping role-filling spans, in order
   * of decreasing quality.
   */
  def getPredictions(frameFeatures: FrameFeatures): Iterator[Scored[RoleAssignment]]

  protected def formatPrediction(rank: Int,
                                 initialDecisionLine: String,
                                 assignments: RoleAssignment,
                                 score: Double): String = {
    Seq(
      rank,
      score,
      assignments.nonNullAssignments.size + 1,
      initialDecisionLine,
      assignments.toString
    ).mkString("\t")
  }

  /** Adds 'offset' to the sentence field and discards the 1st two fields. */
  protected def getInitialDecisionLine(frameLine: String, offset: Int): String = {
    val frameTokens: Array[String] = frameLine.split("\t")
    frameTokens(7) = "" + (frameTokens(7).toInt + offset)
    frameTokens.slice(3, frameTokens.length).mkString("\t").trim
  }

  def scoreSpansForRoles(rolesAndSpanFeatures: Map[String, Array[SpanAndFeatures]]): Map[String, Array[Scored[Range0Based]]] = {
    rolesAndSpanFeatures.mapValues(
      _.map(spanAndFeats => scored(spanAndFeats.span, model.score(spanAndFeats.features)))
    )
  }

  // ******** Derived Method *********

  def decodeAll(frameFeaturesList: util.List[FrameFeatures],
                frameLines: util.List[String],
                offset: Int,
                kBestOutput: Int): util.List[String] = {
    val results = Lists.newArrayList[String]
    for ((frameFeatures, frameLine) <- frameFeaturesList.asScala zip frameLines.asScala) {
      val initialDecisionLine: String = getInitialDecisionLine(frameLine, offset)
      val predictions = getPredictions(frameFeatures)
      val predictionLines = predictions.take(kBestOutput).zipWithIndex.map({ case (prediction, i) =>
        formatPrediction(i, initialDecisionLine, prediction.value, prediction.score)
      })
      results.add(predictionLines.mkString("\n"))
    }
    results
  }
}

/** Predict spans for roles using beam search. */
case class BeamSearchArgDecoding(model: Model, beamWidth: Int) extends ArgDecoding {
  override def getPredictions(frameFeatures: FrameFeatures): Iterator[Scored[RoleAssignment]] = {
    val spansAndFeaturesByRole =
      (frameFeatures.fElements.asScala zip frameFeatures.fElementSpansAndFeatures.asScala).toMap
    val scoredSpansByRole = scoreSpansForRoles(spansAndFeaturesByRole)
    scoredSpansByRole.foldLeft (Iterator.single(scored(RoleAssignment.empty(), 0.0))) {
      case (currentBeam, (roleName, candidatesAndScores)) =>
        val ord = Ordering.by((s: Scored[RoleAssignment]) => -s.score)
        val newBeam = new m.PriorityQueue[Scored[RoleAssignment]]()(ord) // put worst predictions in front
        for (
          partialAssignment <- currentBeam;
          candidate <- candidatesAndScores;
          newScore = partialAssignment.score + candidate.score
          if newBeam.size < beamWidth || newScore > newBeam.head.score;
          newAssignment <- partialAssignment.value.plus(roleName, candidate.value)
        ) {
          newBeam.enqueue(scored(newAssignment, newScore))
          if (newBeam.size > beamWidth) newBeam.dequeue()
        }
        newBeam.dequeueAll.reverseIterator // put best predictions in front
    }
  }
}
object BeamSearchArgDecoding {
  val DefaultBeamWidth = 100

  def defaultInstance(model: Model): BeamSearchArgDecoding = BeamSearchArgDecoding(model, DefaultBeamWidth)
}
