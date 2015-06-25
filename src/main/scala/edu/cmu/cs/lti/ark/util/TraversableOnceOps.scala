package edu.cmu.cs.lti.ark.util

import scala.collection.{mutable => m, immutable => i}

object TraversableOnceOps {
  implicit class GroupByOps[A](xs: TraversableOnce[A]) {
    def groupBy[K, Repr](f: A => K)(implicit newBuilder: m.Builder[A, Repr]): i.Map[K, Repr] = {
      val builderByKey = m.Map.empty[K, m.Builder[A, Repr]].withDefault(_ => newBuilder)
      for (elem <- xs) {
        builderByKey(f(elem)) += elem
      }
      Map() ++ builderByKey.mapValues(_.result())
    }
  }
}

object TraversableOps {
  implicit class GroupRunsOps[A](xs: Traversable[A]) {
    def groupRuns: Stream[Traversable[A]] = groupRunsBy(identity[A]).map(_._2)

    def groupRunsBy[B](f: A => B): Stream[(B, Traversable[A])] = {
      if (xs.isEmpty) {
        Stream.empty[(B, Traversable[A])]
      } else {
        val fHead = f(xs.head)
        val (same, rest) = xs.span { f(_) == fHead }
        (fHead, same) #:: rest.groupRunsBy(f)
      }
    }
  }
}
