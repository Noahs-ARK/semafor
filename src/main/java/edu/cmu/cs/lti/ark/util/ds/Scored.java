package edu.cmu.cs.lti.ark.util.ds;

import edu.cmu.cs.lti.ark.fn.optimization.LDouble;

/**
* @author sthomson@cs.cmu.edu
*/
public class Scored<T> implements Comparable<Scored<?>>  {
	public final T value;
	public final LDouble score;

	public Scored(T value, LDouble score) {
		this.value = value;
		this.score = score;
	}

	public static <V> Scored<V> scored(V val, LDouble score)  {
		return new Scored<V>(val, score);
	}

	/** backwards comparison, so that after sorting, high scores come first */
	@Override
	public int compareTo(Scored<?> other) {
		return other.score.compareTo(score);
	}
}
