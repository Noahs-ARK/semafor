/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * SpanAndCorrespondingFeatures.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.parsing;

import java.io.Serializable;

public class SpanAndCorrespondingFeatures implements Serializable, Comparable<SpanAndCorrespondingFeatures> {
	private static final long serialVersionUID = 5754166746392268274L;
	public int[] span;
	public int[] features;

	public SpanAndCorrespondingFeatures(int[] span, int[] features) {
		this.span = span;
		this.features = features;
	}

	public SpanAndCorrespondingFeatures(int[] span) {
		this.span = span;
	}

	@Override
	public int compareTo(SpanAndCorrespondingFeatures o) {
		final String span1 = span[0]+":"+ span[1];
		final String span2 = o.span[0]+":"+ o.span[1];
		return  span1.compareTo(span2);
	}
}
