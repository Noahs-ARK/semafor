/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * Range.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.util.ds;

import gnu.trove.THashSet;

import java.util.*;

/**
 * Stores start and end integer positions for a range. By default, the end position is inclusive.
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2009-04-20
 */
public abstract class Range implements Iterable<Integer> {
	public final int start;
	public final int end;
	protected boolean endInclusive;

	/** The smallest valid index: 0 for 0-based ranges, 1 for 1-based ranges */
	private int baseIndex;
	
	public Range(int baseIndex, int startPosition, int endPosition) {
		this(baseIndex, startPosition, endPosition, true);
	}
	
	public Range(int baseIndex, int startPosition, int endPosition, boolean isEndInclusive) {
		this.baseIndex = baseIndex;
		
		if (startPosition>0 && startPosition<baseIndex)
			throw new RuntimeException("Invalid Range: startPosition cannot be " + startPosition + " with a base index of " + baseIndex);
		else
			start = startPosition;
		
		if (endPosition>0 && endPosition<baseIndex)
			throw new RuntimeException("Invalid Range: endPosition cannot be " + endPosition + " with a base index of " + baseIndex);
		else
			end = endPosition;
		
		endInclusive = isEndInclusive;
		
	}

	public int getBaseIndex() {
		return baseIndex;
	}
	
	public boolean isZeroBased() {
		return getBaseIndex()==0;
	}
	
	public boolean isEndInclusive() {
		return endInclusive;
	}
	
	public int length() {
		return ((endInclusive) ? end-start+1 : end-start);
	}
	
	public<T> List<T> applyToList(List<T> l) {
		return l.subList(start, ((endInclusive) ? end+1 : end));
	}
	
	public<T> T[] applyToArray(T[] a) {
		return Arrays.copyOfRange(a, start, ((endInclusive) ? end+1 : end));
	}
	
	public String applyToString(String s) {
		return s.substring(start, start+length());
	}
	
	/**
	 * @param i
	 * @return Whether the value of 'i' is included in the range
	 */
	public boolean contains(int i) {
		return (i>=start && i<start+length());
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof Range)) return false;
		final Range o = (Range) other;
		return (this.start == o.start &&
				this.end == o.end &&
				this.endInclusive == o.endInclusive &&
				this.baseIndex == o.baseIndex);
	}
	
	@Override
	public int hashCode() {
		return (this.start*19+this.end*419)%29591937;
	}

	public Iterator<Integer> iterator() {
		List<Integer> list = new ArrayList<Integer>();
		for (int i=start; i<start+length(); i++)
			list.add(i);
		return list.iterator();
	}
	
	@Override
	public abstract Range clone();
	
	@Override
	public String toString() {
		String s = "[" + start + "," + end;
		s += ((endInclusive) ? "]" : ")");
		return s;
	}

	public static Set<Integer> union(List<? extends Range> ranges) {
		Set<Integer> result = new THashSet<Integer>();
		for (Range range : ranges) {
			for (int i : range)
				result.add(i);
		}
		return result;
	}
}
