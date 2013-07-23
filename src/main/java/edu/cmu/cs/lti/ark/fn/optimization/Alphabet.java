/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * Alphabet.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.optimization;

import gnu.trove.TObjectIdentityHashingStrategy;
import gnu.trove.TObjectIntHashMap;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Maps each unique string to a unique index.
 *
 * @author dipanjan
 */
public class Alphabet implements Serializable {
	private static final long serialVersionUID = -3475498139713667452L;
	private final ArrayList<String> strings;
	private final TObjectIntHashMap<String> reverseIndex;

	public Alphabet() {
		final int ALPHABET_INITIAL_CAPACITY = 2000000;
		strings = new ArrayList<String>(ALPHABET_INITIAL_CAPACITY);
		reverseIndex =
				new TObjectIntHashMap<String>(ALPHABET_INITIAL_CAPACITY, new TObjectIdentityHashingStrategy<String>());
	}

	public int getNumEntries() {
		return strings.size();
	}
	
	/**
	 * Returns String corresponding to given integer i, or "<UNK>" if i is out of range.
	 * @param i the index
	 * @return String corresponding to i
	 */
	public String getString(int i) {
		if (i <= strings.size() && i >= 1)
			return strings.get(i-1);
		return "<UNK>";
	}

	/**
	 * Adds given String to this Alphabet if it has not already been added. It is assigned 
	 * a new unique ID equal to the new size of the Alphabet after performing the addition.  
	 * @param s String to add
	 */
	public void addString(String s) {
		String ss = s.intern();
		if (!reverseIndex.containsKey(ss)) {
			reverseIndex.put(ss, strings.size());
			strings.add(ss);
		}
	}

	public boolean checkString(String s) {
		return reverseIndex.containsKey(s.intern());
	}
	
	/**
	 * Returns int corresponding to given String s. If s has not been seen before,
	 * adds s to strings and reverseIndex and returns the hashed index of s in reverseIndex.
	 * @param s the string to look up
	 * @return
	 */
	public int getInt(String s) {
		String ss = s.intern();
		if(!checkString(s)) {
			strings.add(ss);
			int newIndex = strings.size();
			reverseIndex.put(ss, newIndex);
			return newIndex;
		}
		return reverseIndex.get(ss);
	}

	public ArrayList<String> getEntries() {
		return strings;
	}
} 

