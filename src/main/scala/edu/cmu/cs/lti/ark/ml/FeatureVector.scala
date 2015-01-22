package edu.cmu.cs.lti.ark.ml

import breeze.collection.mutable.SparseArray
import breeze.linalg.SparseVector

/**
 * A binary feature vector. Only stores the indexes of features that fired.
 * assumes trueIdxs is sorted
 */
case class FeatureVector(override val index: Array[Int],
                         override val length: Int)
  extends SparseVector[Double](
    new SparseArray[Double](
      index,
      Array.fill(index.length)(1.0),
      index.length,
      length,
      0.0
    )
  )
