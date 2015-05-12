/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * Range0Based.java is part of SEMAFOR 2.0.
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
/**
 * 
 */
package edu.cmu.cs.lti.ark.util.ds;

import com.google.common.collect.ComparisonChain;


/**
 * A range of values whose smallest normal index is 0. (Negative values may be used for "non-normal" indices.) 
 * This is to distinguish 0-based ranges from 1-based ranges when both are used.
 * @author Nathan Schneider (nschneid)
 * @since 2009-06-25
 */
public class Range0Based extends Range implements Comparable<Range0Based> {
	
	/**
	 * Converts a 1-based range to a 0-based range by subtracting 1 from the start and end indices
	 * @param r A 1-based range
	 */
	public Range0Based(Range1Based r) {
		this(r.start -1, r.end -1, r.isEndInclusive());
	}

	public Range0Based(int startPosition, int endPosition) {
		super(0, startPosition, endPosition);
	}

	public Range0Based(int startPosition, int endPosition, boolean isEndInclusive) {
		super(0, startPosition, endPosition, isEndInclusive);
	}

	private int endIdx() {
		return end + (isEndInclusive() ? 1 : 0);
	}

	/** Determines whether this and the other span overlap */
	public boolean overlaps(Range0Based other) {
		// empty spans can't overlap with anything
		if(isEmpty() || other.isEmpty()) return false;
		if(start < other.start) {
			return endIdx() > other.start;
		} else {
			return other.endIdx() > start;
		}
	}

	public Range0Based clone() {
		return new Range0Based(this.start, this.end, this.endInclusive);
	}

	@Override
	public int compareTo(Range0Based other) {
		return ComparisonChain.start()
				.compare(start, other.start)
				.compare(length(), other.length()).result();
	}
}
