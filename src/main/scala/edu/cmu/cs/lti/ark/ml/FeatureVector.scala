package edu.cmu.cs.lti.ark.ml

import java.util

import breeze.collection.mutable.SparseArray
import breeze.linalg.{Vector => Vec, DenseVector, SparseVector}

import scala.reflect.ClassTag

/**
 * A binary feature vector. Only stores the indexes of features that fired.
 */
case class FeatureVector(indexes: Array[Int], length: Int) {
  def *(d: Double): SparseVector[Double] = new SparseVector(sparseArray(d, 0))

  def -(other: FeatureVector) = this.sparseVector - other.sparseVector

  def dot(other: Vec[Double]): Double = {
    var result = 0.0
    var i = 0
    while (i < indexes.length) {
      result += other(indexes(i))
      i += 1
    }
    result
  }

  def sparseVector: SparseVector[Double] = this * 1.0

  private lazy val sorted: Array[Int] = {
    util.Arrays.sort(indexes)
    indexes
  }

  private def sparseArray[T: ClassTag](t: T, f: T): SparseArray[T] = {
    new SparseArray[T](sorted, Array.fill(sorted.length)(t), sorted.length, length, f)
  }
}

case class MultiClassTrainingExample(featuresByLabel: Array[FeatureVector],
                                     correctLabel: Int)

object Vectors {
  /** Converts a SparseVector to a DenseVector using all cores */
  def toDenseVector(vec: Vec[Double]): DenseVector[Double] = vec match {
    case v: SparseVector[Double] =>
      val result = Array.ofDim[Double](vec.length)
      (0 until v.activeSize).par.map(i =>
        result(v.index(i)) = v.data(i)
      )
      DenseVector(result)
    case grad: DenseVector[Double] => grad
    case grad => grad.toDenseVector
  }
}
