/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * FrameFeatures.java is part of SEMAFOR 2.0.
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
import java.util.ArrayList;
import java.util.List;

public class FrameFeatures implements Serializable {
	private static final long serialVersionUID = 1628884148325532841L;
	final public String frameName;
	final public int start;
	final public int end;
	final public List<String> fElements;
	final public List<SpanAndCorrespondingFeatures[]> fElementSpansAndFeatures;
	// indexes of fElementSpansAndFeatures that are gold spans (used only in training)
	final public List<Integer> goldSpanIdxs;

	public FrameFeatures(String frameName,
						 int targetStartTokenIdx,
						 int targetEndTokenIdx,
						 List<String> fElements,
						 List<SpanAndCorrespondingFeatures[]> fElementSpansAndFeatures,
						 List<Integer> goldSpanIdxs) {
		this.frameName = frameName;
		this.start = targetStartTokenIdx;
		this.end = targetEndTokenIdx;
		this.fElements = fElements;
		this.fElementSpansAndFeatures = fElementSpansAndFeatures;
		this.goldSpanIdxs = goldSpanIdxs;
	}

	public FrameFeatures(String frameName, int start, int end) {
		this(frameName, start, end, new ArrayList<String>(), new ArrayList<SpanAndCorrespondingFeatures[]>());
	}

	public FrameFeatures(String frameName,
						 int targetStartTokenIdx,
						 int targetEndTokenIdx,
						 List<String> fElements,
						 List<SpanAndCorrespondingFeatures[]> fElementSpansAndFeatures) {
		this(frameName, targetStartTokenIdx, targetEndTokenIdx, fElements, fElementSpansAndFeatures, new ArrayList<Integer>());
	}
}

