/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * Range1Based.java is part of SEMAFOR 2.0.
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

/**
 * A range of values whose smallest legal index is 1. Setting the start or end position to 0 is an error. 
 * (Negative values may be used for "non-normal" indices.) 
 * This is to distinguish 1-based ranges from 0-based ranges when both are used.
 * @author Nathan Schneider (nschneid)
 * @since 2009-06-25
 */
public class Range1Based extends Range {

	/**
	 * Converts a 0-based range to a 1-based range by adding 1 to the start and end indices
	 * @param r A 0-based range
	 */
	public Range1Based(Range0Based r) {
		this(r.start +1, r.end +1, r.isEndInclusive());
	}
	
	/**
	 * Creates a new range with start and end indices computed relative to those of an existing range.
	 * @param r Range serving as a point of reference
	 * @param deltaStart Amount to add to the start position in the provided range
	 * @param deltaEnd Amount to add to the end position in the provided range
	 */
	public Range1Based(Range r, int deltaStart, int deltaEnd) {
		this(r.start +deltaStart, r.end +deltaEnd, r.isEndInclusive());
	}
	
	/**
	 * @param startPosition
	 * @param endPosition
	 */
	public Range1Based(int startPosition, int endPosition) {
		super(1, startPosition, endPosition);
	}

	/**
	 * @param startPosition
	 * @param endPosition
	 * @param isEndInclusive
	 */
	public Range1Based(int startPosition, int endPosition, boolean isEndInclusive) {
		super(1, startPosition, endPosition, isEndInclusive);
	}
	
	/**
	 * Creates and returns a range immediately following the current range and having the specified length.
	 * @param newLength
	 */
	public Range1Based successor(int newLength) {
		return new Range1Based(this.start+this.length(), this.start+this.length()+newLength + ((this.endInclusive) ? -1 : 0), this.endInclusive);
	}

	public Range1Based clone() {
		return new Range1Based(this.start, this.end, this.endInclusive);
	}
}
