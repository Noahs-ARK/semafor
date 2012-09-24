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
import java.util.Arrays;
import java.util.Comparator;

public class SpanAndCorrespondingFeatures implements Serializable, Comparator<SpanAndCorrespondingFeatures>
{
	private static final long serialVersionUID = 5754166746392268274L;
	public int[] span;
	public int[] features;
		
	public int compare(SpanAndCorrespondingFeatures arg0, SpanAndCorrespondingFeatures arg1)
	{
		String span1 = arg0.span[0]+":"+arg0.span[1];
		String span2 = arg1.span[0]+":"+arg1.span[1];
		if(span1.compareTo(span2)<0)
			return -1;
		else if(span1.compareTo(span2)==0)
			return 0;
		else
			return 1;	
	}
	
	public static void sort(SpanAndCorrespondingFeatures[] arr)
	{
		Arrays.sort(arr,new SpanAndCorrespondingFeatures());
	}
	
	public static int search(SpanAndCorrespondingFeatures[] arr, SpanAndCorrespondingFeatures s)
	{
		return Arrays.binarySearch(arr,s,new SpanAndCorrespondingFeatures());
	}
	
	/*
	 * span with _
	 */
	public static int search(SpanAndCorrespondingFeatures[] arr, String span)
	{
		String[] toks = span.split("_");
		SpanAndCorrespondingFeatures scf = new SpanAndCorrespondingFeatures();
		scf.span=new int[2];
		scf.span[0]=new Integer(toks[0]);
		scf.span[1]=new Integer(toks[1]);
		return search(arr,scf);
	}
}
