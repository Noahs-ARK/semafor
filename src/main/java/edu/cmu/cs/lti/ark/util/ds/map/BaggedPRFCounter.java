/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * BaggedPRFCounter.java is part of SEMAFOR 2.0.
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
 ******************************************************************************/
package edu.cmu.cs.lti.ark.util.ds.map;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.lti.ark.util.Prediction;

/**
 * Computes precision, recall, and F1 for lists of unordered collections of elements.
 * @author Nathan Schneider (nschneid)
 * @since 2009-10-16
 */

public class BaggedPRFCounter<K> extends Object implements Iterable<K> {
	//protected List<IntCounter<T>> goldGroupCounts = new ArrayList<IntCounter<T>>();
	//protected List<IntCounter<T>> testGroupCounts = new ArrayList<IntCounter<T>>();
	
	protected Set<K> keys = new HashSet<K>();
	
	protected IntCounter<K> goldCounts = new IntCounter<K>();
	protected IntCounter<K> testCounts = new IntCounter<K>();
	protected IntCounter<K> correctCounts = new IntCounter<K>();	// divide by counts in 'testCounts' for precision, 'goldCounts' for recall
	
	public BaggedPRFCounter(List<? extends Collection<? extends K>> goldGroups, List<? extends Collection<? extends Prediction<K>>> testGroups) throws Exception {
		this(goldGroups.iterator(), testGroups.iterator());
	}
	
	public BaggedPRFCounter(List<? extends Collection<? extends K>> goldGroups, List<? extends Collection<? extends Prediction<K>>> testGroups, double confidenceThreshold) throws Exception {
		this(goldGroups.iterator(), testGroups.iterator(), confidenceThreshold);
	}
	
	public BaggedPRFCounter(Iterator<? extends Collection<? extends K>> goldGroups, Iterator<? extends Collection<? extends Prediction<K>>> testGroups) throws Exception {
		this(goldGroups, testGroups, Double.NEGATIVE_INFINITY);
	}
	
	protected BaggedPRFCounter(Iterator<? extends Collection<? extends K>> goldGroups, Iterator<? extends Collection<? extends Prediction<K>>> testGroups, double confidenceThreshold) throws Exception {
		int nGold = 0;
		int nTest = 0;
		while (goldGroups.hasNext()) {
			Collection<? extends K> gold = goldGroups.next();
			nGold++;
			if (!testGroups.hasNext())
				throw new Exception("Not enough groups in the test set: only " + nTest + " found");	// TODO: custom Exception subtype
			Collection<? extends Prediction<K>> test = testGroups.next();
			nTest++;
			
			IntCounter<K> localGoldCounts = new IntCounter<K>();
			for (K item : gold) {
				localGoldCounts.increment(item);
				goldCounts.increment(item);
				keys.add(item);
			}
			
			for (Prediction<K> prediction : test) {
				if (prediction.getScore()<confidenceThreshold)
					continue;
				K item = prediction.getItem();
				testCounts.increment(item);
				keys.add(item);
				if (localGoldCounts.getT(item)>0) {
					correctCounts.increment(item);
					localGoldCounts.incrementBy(item, -1);
				}
			}
			
		}
		while (testGroups.hasNext()) {
			testGroups.next();
			nTest++;
		}
		if (nTest > nGold) {
			throw new Exception("Too many groups in the test set: found " + nTest + ", only " + nGold + " in the gold set");	// TODO: custom Exception subtype
		}
	}
	
	/**
	 * Returns the union of keys for the predictions and gold items
	 */
	public Set<K> getKeys() {
		return keys;
	}
	
	public Iterator<K> iterator() {
		return keys.iterator();
	}
	
	/**
	 * Returns a counter with the absolute number of predictions for each element type
	 */
	public IntCounter<K> getPredictionCounts() {
		return testCounts;
	}
	/**
	 * Returns a counter with the absolute number of gold instances of each element type
	 */
	public IntCounter<K> getGoldCounts() {
		return goldCounts;
	}
	/**
	 * Returns a counter with the absolute number of correct predictions for each element type
	 */
	public IntCounter<K> getCorrectCounts() {
		return correctCounts;
	}

	public static double getPrecision(int nCorrect, int nPredicted) {
		return 1.0 * nCorrect / nPredicted;
	}
	public double getPrecision(K key) {
		return getPrecision(correctCounts.getT(key), testCounts.getT(key));
	}
	/** Returns the overall precision (for all keys) */
	public double getPrecision() {
		return getPrecision(correctCounts.getSum(), testCounts.getSum());  
	}
	public static String getPrecisionString(int nCorrect, int nPredicted) {
		return String.format("P: %s/%s = %.4f", nCorrect, nPredicted, getPrecision(nCorrect, nPredicted));
	}
	public String getPrecisionString(K key) {
		return getPrecisionString(correctCounts.getT(key), testCounts.getT(key));
	}
	public String getPrecisionString() {
		return getPrecisionString(correctCounts.getSum(), testCounts.getSum());
	}
	public Counter<K> getPrecisionCounter() {
		return correctCounts.divideAllBy(testCounts.toCounter());
	}
	/**
	 * Returns a counter with the absolute number of precision errors for each element type, 
	 * i.e. the total number of predictions minus the number of correct predictions 
	 */
	public IntCounter<K> getPrecisionErrorCounts() {
		return getPredictionCounts().addAll(getCorrectCounts().scaleBy(-1));
	}
	
	public static double getRecall(int nCorrect, int nGold) {
		return 1.0 * nCorrect / nGold;
	}
	public double getRecall(K key) {
		return getRecall(correctCounts.getT(key), goldCounts.getT(key));
	}
	/** Returns the overall recall (for all keys) */
	public double getRecall() {
		return getRecall(correctCounts.getSum(), goldCounts.getSum());
	}
	public static String getRecallString(int nCorrect, int nGold) {
		return String.format("R: %s/%s = %.4f", nCorrect, nGold, getRecall(nCorrect, nGold));
	}
	public String getRecallString(K key) {
		return getRecallString(correctCounts.getT(key), goldCounts.getT(key));
	}
	public String getRecallString() {
		return getRecallString(correctCounts.getSum(), goldCounts.getSum());
	}
	public Counter<K> getRecallCounter() {
		return correctCounts.divideAllBy(goldCounts.toCounter());
	}
	/**
	 * Returns a counter with the absolute number of recall errors for each element type, 
	 * i.e. the number of true instances minus the number of correct predictions
	 */
	public IntCounter<K> getRecallErrorCounts() {
		return getGoldCounts().addAll(getCorrectCounts().scaleBy(-1));
	}
	
	public static double getF1(double precision, double recall) {
		return 2.0 * precision * recall / (precision + recall);
	}
	public double getF1(K key) {
		return getF1(getPrecision(key), getRecall(key));
	}
	/** Returns overall F1 (for all keys) */
	public double getF1() {
		return getF1(getPrecision(), getRecall());
	}
	public static String getF1String(double precision, double recall) {
		return String.format("F = %.4f", getF1(precision,recall));
	}
	public static String getF1String(int nCorrect, int nPredicted, int nGold) {
		return getF1String(getPrecision(nCorrect, nPredicted), getRecall(nCorrect, nGold));
	}
	public String getF1String() {
		return getF1String(getPrecision(), getRecall());
	}
	public Counter<K> getF1Counter() {
		Counter<K> pCounter = getPrecisionCounter();
		Counter<K> rCounter = getRecallCounter();
		Counter<K> num = pCounter.scaleAllBy(rCounter).scaleBy(2.0);
		Counter<K> den = pCounter.addAll(rCounter);
		return num.divideAllBy(den);
	}
	
	public static String getPRFString(int nCorrect, int nPredicted, int nGold) {
		return getPrecisionString(nCorrect, nPredicted) + " " + getRecallString(nCorrect, nGold) + " " + getF1String(nCorrect, nPredicted, nGold);
	}
	public String toString() {
		return getPrecisionString() + " " + getRecallString() + " " + getF1String();
	}
}
