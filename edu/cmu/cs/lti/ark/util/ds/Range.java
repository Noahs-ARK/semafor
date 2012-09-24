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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores start and end integer positions for a range. By default, the end position is inclusive.
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2009-04-20
 */
public abstract class Range implements Iterable<Integer> {
	protected int start;
	protected int end;
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
	
	public int getStart() {
		return start;
	}
	
	public int getEnd() {
		return end;
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
	public boolean equals(Object o) {
		try {
			Range r = (Range)o;
			return (this.start==r.start && this.end==r.end && this.endInclusive==r.endInclusive  && this.baseIndex==r.baseIndex);
		} catch (ClassCastException ex) {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return (this.start*19+this.end*419)%29591937;
	}
	
	public boolean isEquivalentTo(Range r) {
		return (this.start==r.start && this.length()==r.length());  
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
	
	/**
	 * Version of classifyByRange which accepts Map entry sets
	 * @param <T>
	 * @param n
	 * @param classes
	 * @return
	 * @see #classifyByRange(int, java.util.Collection)
	 */
	public static <T> T classifyByRange(int n, Set<Map.Entry<T,Range>> classes) {
		for (Map.Entry<T,Range> classAndRange : classes) {
			Range r = classAndRange.getValue();
			if (r.contains(n))
				return classAndRange.getKey();
		}
		return null;
	}
	
	/**
	 * Given a group of (object, Range) pairs, find a group whose range includes 'n' 
	 * and return the accompanying object.
	 * @param <T>
	 * @param n
	 * @param classes
	 * @return
	 */
	public static <T> T classifyByRange(int n, java.util.Collection<Pair<T,Range>> classes) {
		for (Pair<T,Range> classAndRange : classes) {
			Range r = classAndRange.getSecond();
			if (r.contains(n))
				return classAndRange.getFirst();
		}
		return null;
	}
	
	public static Set<Integer> union(List<? extends Range> ranges) {
		Set<Integer> result = new THashSet<Integer>();
		for (Range range : ranges) {
			for (int i : range)
				result.add(i);
		}
		return result;
	}
	public static Set<Integer> intersect(List<? extends Range> ranges) {
		Set<Integer> union = union(ranges);
		Set<Integer> result = new THashSet<Integer>();
		for (int i : union) {
			boolean included = true;
			for (Range range : ranges) {
				if (!range.contains(i)) {
					included = false;
					break;
				}
				if (included)
					result.add(i);
			}
		}
		return result;
	}

	/**
	 * Computes the average number of ranges that include a given index, out of the union of the 
	 * given ranges (i.e. the set of indices covered by some range).
	 * @param ranges
	 * @return
	 */
	public static double averageRangeOverlap(List<? extends Range> ranges) {
		int totalLength = 0;
		for (Range range : ranges) {
			totalLength += range.length();
		}
		return 1.0*totalLength / union(ranges).size();
	}
}
