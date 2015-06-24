/** *****************************************************************************
  * Copyright (c) 2011 Dipanjan Das 
  * Language Technologies Institute, 
  * Carnegie Mellon University, 
  * All Rights Reserved.
  *
  * DataPrep.java is part of SEMAFOR 2.0.
  *
  * SEMAFOR 2.0 is free software: you can redistribute it and/or modify  it
  * under the terms of the GNU General Public License as published by the
  * Free Software Foundation, either version 3 of the License, or 
  * (at your option) any later version.
  *
  * SEMAFOR 2.0 is distributed in the hope that it will be useful, but
  * WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  * See the GNU General Public License for more details. 
  *
  * You should have received a copy of the GNU General Public License along
  * with SEMAFOR 2.0.  If not, see <http://www.gnu.org/licenses/>.
  * *****************************************************************************/
package edu.cmu.cs.lti.ark.fn.parsing

import java.io._
import javax.annotation.concurrent.NotThreadSafe

import com.google.common.collect.ImmutableList
import edu.cmu.cs.lti.ark.fn.data.prep.formats.{AllLemmaTags, Sentence}
import edu.cmu.cs.lti.ark.fn.parsing.CandidateSpanPruner.EmptySpan
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements.FrameElementAndSpan
import edu.cmu.cs.lti.ark.util.ds.Range0Based
import resource.managed

import scala.collection.JavaConverters._
import scala.collection.{mutable => m}
import scala.io.Source


case class FeAndSpansWithFeatures(frameElement: String, spans: Array[SpanAndFeatures])

/**
 * A mutable bimap between feature name and index.
 * `forAll feat: String => names.toVector(index(feat)) == feat`
 * `forAll i: Int => index(names.toVector(i)) == i`
 * Expands to add new features on lookup misses.
 * This is useful for "numberizing" features, so model parameters can be stored
 * in a 1d vector.
 * Uses a `LinkedHashMap` so that features are sorted by insertion order.
 */
@NotThreadSafe
case class FeatureIndex(index: m.LinkedHashMap[String, Int])
    extends m.Map.WithDefault[String, Int](index, k => index.getOrElseUpdate(k, index.size + 1)) {

  def names: Iterator[String] = index.keysIterator

  def save(file: File) {
    val numFeatures = size
    for (printStream <- managed(new PrintStream(new FileOutputStream(file)))) {
      printStream.println(numFeatures)
      names.foreach(printStream.println)
    }
  }

  def asJava: java.util.Map[String, Integer] = index.mapValues(Integer.valueOf).asJava
}

object FeatureIndex {
  def empty: FeatureIndex = FeatureIndex(m.LinkedHashMap.empty)

  def fromFile(file: File): FeatureIndex = {
    val result = empty
    managed(Source.fromFile(file)).foreach(_.getLines().drop(1).foreach(result))
    result
  }
}


@NotThreadSafe
class DataPrep(tagLines: Array[String], index: FeatureIndex) {
  val spanPruner = new CandidateSpanPruner
  val extractor = new FeatureExtractor()
  val sentences = tagLines.map(line => Sentence.fromAllLemmaTagsArray(AllLemmaTags.readLine(line)))

  def frameFeatures(feLine: String): FrameFeatures = {
    val roleAssignment = RankedScoredRoleAssignment.fromLine(feLine)
    val sentence = sentences(roleAssignment.sentenceIdx)
    val candidates = spanPruner.candidateSpans(sentence, roleAssignment.targetSpan)
    val goldSpans = roleAssignment.fesAndSpans.map(_.span)
    val spans = (candidates.asScala.toSet ++ goldSpans).toArray.sorted
    val feAndSpansWithFeatsAndGoldIdxs = getFeaturesForFrameLine(feLine, spans, sentence).toArray
    new FrameFeatures(
      roleAssignment.frame,
      roleAssignment.targetSpan.start,
      roleAssignment.targetSpan.end,
      ImmutableList.copyOf(feAndSpansWithFeatsAndGoldIdxs.map(_._1.frameElement)),
      ImmutableList.copyOf(feAndSpansWithFeatsAndGoldIdxs.map(_._1.spans)),
      ImmutableList.copyOf(feAndSpansWithFeatsAndGoldIdxs.map(s => Integer.valueOf(s._2)))
    )
  }

  def getFeaturesForFrameLine(feline: String,
                              candidateSpans: Array[Range0Based],
                              sentence: Sentence): Set[(FeAndSpansWithFeatures, Int)] = {
    val dataPoint = new DataPointWithFrameElements(sentence, feline)
    val frame = dataPoint.getFrameName
    val allFrameElements = FEDict.getInstance.lookupFrameElements(frame)
    val fesAndSpans = dataPoint.getFrameElementsAndSpans
    // overt frame elements
    val realizedFes = dataPoint.getOvertFrameElementNames.asScala.toSet
    // null-instantiated FEs
    val nullFes = allFrameElements.toSet.diff(realizedFes)
    val nullFesAndSpans = nullFes.map(new FrameElementAndSpan(_, EmptySpan))
    for (feAndSpan <- nullFesAndSpans ++ fesAndSpans.asScala) yield {
      val spansAndFeats = getFeaturesForRole(dataPoint, frame, feAndSpan.name, candidateSpans)
      val goldSpanIdx = candidateSpans.indexOf(feAndSpan.span)
      (FeAndSpansWithFeatures(feAndSpan.name, spansAndFeats), goldSpanIdx)
    }
  }

  private def getFeaturesForRole(dp: DataPointWithFrameElements,
                                 frame: String,
                                 frameElement: String,
                                 candidateSpans: Array[Range0Based]): Array[SpanAndFeatures] = {
    val parse = dp.getParse
    candidateSpans.map { span =>
      val feats = extractor.extractFeatures(dp, frame, frameElement, span, parse)
                    .iterator().asScala.map(index).toArray
      SpanAndFeatures(span, feats)
    }
  }
}
