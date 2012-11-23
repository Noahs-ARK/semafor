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
import java.util.Comparator;

public class FrameFeatures implements Serializable
{
	private static final long serialVersionUID = 1628884148325532841L;
	public String frameName;
	public int start;
	public int end;
	String goldFrameElement;
	
	public ArrayList<String> fElements;
	public ArrayList<SpanAndCorrespondingFeatures[]> fElementSpansAndFeatures;
	public ArrayList<Integer> fGoldSpans;
		
	public FrameFeatures(String fn, int fs, int fe)
	{
		frameName=""+fn;
		fs = start;
		fe = end;
		fElements = new ArrayList<String>();
		fElementSpansAndFeatures = new ArrayList<SpanAndCorrespondingFeatures[]>();
		fGoldSpans = new ArrayList<Integer>();
	}	
}

