package edu.cmu.cs.lti.ark.fn.parsing

import java.io.File
import java.util

import com.google.common.collect.Lists
import edu.cmu.cs.lti.ark.fn.parsing.CandidateSpanPruner.EMPTY_SPAN
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
  type Role = String

  def model: Model

  /**
   * Decode, respecting the constraint that arguments do not overlap.
   * Returns the (approximately?) best configurations of non-overlapping role-filling spans, in order
   * of decreasing quality.
   */
  def decode(scoredSpans: Map[Role, Map[Range0Based, Double]]): Iterator[Scored[RoleAssignment]]

  protected def formatPrediction(rank: Int,
                                 initialDecisionLine: String,
                                 assignments: RoleAssignment,
                                 score: Double): String = {
    Seq(
      rank,
      score,
      assignments.overtAssignments.size + 1,
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

  def scoreSpansForRoles(rolesAndSpanFeatures: Map[String, Array[SpanAndFeatures]]): Map[Role, Map[Range0Based, Double]] = {
    rolesAndSpanFeatures.mapValues(
      _.map(spanAndFeats => spanAndFeats.span -> model.score(spanAndFeats.features)).toMap
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
      val spansAndFeaturesByRole =
        (frameFeatures.fElements.asScala zip frameFeatures.fElementSpansAndFeatures.asScala).toMap
      val scoredSpansByRole = scoreSpansForRoles(spansAndFeaturesByRole)
      val predictions = decode(scoredSpansByRole)
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
  /** Enforce the constraint that spans don't overlap using beam search */
  override def decode(scoredSpans: Map[Role, Map[Range0Based, Double]]): Iterator[Scored[RoleAssignment]] = {
    // put worst predictions in front so they can fall off the beam
    val ord = Ordering.by((s: Scored[RoleAssignment]) => -s.score)
    val initialBeam = m.PriorityQueue(scored(RoleAssignment.empty(), 0.0))(ord)
    val finalBeam = scoredSpans.foldLeft(initialBeam) {
      case (currentBeam, (role, spans)) =>
        val newBeam = m.PriorityQueue()(ord)
        for (
          partialAssignment <- currentBeam;
          (span, score) <- spans;
          newScore = partialAssignment.score + score
          if newBeam.size < beamWidth || newScore > newBeam.head.score;
          newAssignment <- partialAssignment.value.plus(role, span)
        ) {
          newBeam.enqueue(scored(newAssignment, newScore))
          if (newBeam.size > beamWidth) newBeam.dequeue() // keep the beam size tidy
        }
        newBeam
    }
    finalBeam.dequeueAll.reverseIterator // put best predictions in front
  }
}
object BeamSearchArgDecoding {
  val DefaultBeamWidth = 100

  def defaultInstance(model: Model): BeamSearchArgDecoding = BeamSearchArgDecoding(model, DefaultBeamWidth)
}


/** Predict spans for roles using a dynamic program */
case class SemiMarkovArgDecoding(model: Model) extends ArgDecoding {

  case class Item(score: Double, backPointer: Option[(Role, Range0Based)])

  /**
   * Enforce the constraint that spans don't overlap using a (0th-order semi-markov) dynamic program.
   * We assume that `scoredSpansByRole.keySet` contains every possible role.
   * Currently only returns the 1-best prediction.
   */
  override def decode(scoredSpans: Map[Role, Map[Range0Based, Double]]): Iterator[Scored[RoleAssignment]] = {
    // arrange role/span pairs by start and end token instead of by role
    val candidates: Seq[(Range0Based, Role, Double)] = scoredSpans.toSeq.flatMap {
      case (role, spans) => spans.filterKeys(_ != EMPTY_SPAN).map({ case (span, score) => (span, role, score) })
    }

    if (candidates.isEmpty) {
      val nullAssignments = scoredSpans.mapValues(_ => EMPTY_SPAN)
      val score = scoredSpans.values.map(_(EMPTY_SPAN)).sum
      return Iterator.single(scored(RoleAssignment(Map(), nullAssignments), score))
    }

    val candidatesByEndToken = candidates.groupBy(_._1.end).withDefaultValue(Seq())
    val lastToken = candidatesByEndToken.keys.max
    // map from token index `i` to best partial assignments that don't extend past `i`
    // We remember the best partial assignment for each subset of roles covered.
    val chart: m.Map.WithDefault[Int, Map[Set[Role], Item]] =
      new m.Map.WithDefault[Int, Map[Set[Role], Item]](m.Map(), (i: Int) => Map(Set.empty[Role] -> Item(0.0, None)))
    for (i <- 0 to lastToken) {
      chart(i) = chart(i - 1) // moving ahead one token without filling any roles is always an option
      for ((span, role, score) <- candidatesByEndToken(i)) {
        // partial assignments that don't overlap `span` and don't already contain `role`
        val nonConflicting = chart(span.start - 1).filterKeys(!_.contains(role))
        for ((rolesCovered, best) <- nonConflicting) {
          // if adding argument improves the best (for any subset of roles covered), update the chart
          val includingMe = rolesCovered + role
          val newScore = best.score + score
          if (!chart(i).contains(includingMe) || chart(i)(includingMe).score < newScore) {
            chart(i) += (includingMe -> Item(newScore, Some((role, span))))
          }
        }
      }
    }
    // null spans get added in at the end
    val allRoles = scoredSpans.keySet
    val withNonOvert = for ((covered, item) <- chart(lastToken)) yield {
      val nullRoles = allRoles.diff(covered)
      val nonOvert = Map() ++ nullRoles.map(_ -> EMPTY_SPAN)
      val overt = {
        var result = List.empty[(Role, Range0Based)]
        var remaining = covered
        var oLatest: Option[(Role, Range0Based)] = item.backPointer
        while (oLatest.isDefined) {
          val latest = oLatest.get
          result ::= latest
          val (role, span) = latest
          remaining = remaining - role
          oLatest = if (span.start > 0) {
            chart(span.start - 1).get(remaining).flatMap(_.backPointer)
          } else None
        }
        Map() ++ result
      }
      val score =
        (overt ++ nonOvert).map({ case (role, span) => scoredSpans(role)(span) }).sum
      scored(RoleAssignment(overt, nonOvert), score)
    }

    Iterator.single(withNonOvert.maxBy(_.score))
  }
}
