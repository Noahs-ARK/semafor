package edu.cmu.cs.lti.ark.fn.identification.training;

import gnu.trove.TIntDoubleHashMap;

import java.io.Serializable;

/**
* @author sthomson@cs.cmu.edu
*/
public class FeaturesAndCost implements Serializable {
	private static final long serialVersionUID = 1904327534458351399L;
	public final TIntDoubleHashMap features;
	public final float cost;

	public FeaturesAndCost(TIntDoubleHashMap features, float cost) {
		this.features = features;
		this.cost = cost;
	}
}
