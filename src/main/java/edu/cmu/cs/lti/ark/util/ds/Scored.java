package edu.cmu.cs.lti.ark.util.ds;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
* @author sthomson@cs.cmu.edu
*/
public class Scored<T> implements Comparable<Scored<T>>  {
	public final T value;
	public final double score;
	public final Ordering<T> ordering;

	public Scored(T value, double score, Ordering<T> ordering) {
		this.value = value;
		this.score = score;
		this.ordering = ordering;
	}

	public static <V extends Comparable> Scored<V> scored(V val, double score)  {
		return new Scored<V>(val, score, Ordering.<V>natural());
	}

	/** backwards comparison, so that after sorting, high scores come first */
	@Override
	public int compareTo(Scored<T> other) {
		return ComparisonChain.start()
				.compare(other.score, this.score)
				.compare(other.value, this.value, this.ordering)
				.result();
	}
}
