package edu.cmu.cs.lti.ark.util;

/**
 * @author sthomson@cs.cmu.edu
 */
public class Math2 {
	public static double sum(double... values) {
		double result = 0;
		for (double val : values) {
			result += val;
		}
		return result;
	}
}
