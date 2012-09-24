/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * ArrayUtil.java is part of SEMAFOR 2.0.
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

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2010-10-30
 */

public class ArrayUtil {
	/** 
	 * Convert a List<Integer> object to a primitive array of type int[].
	 * From http://stackoverflow.com/questions/718554/how-to-convert-an-arraylist-containing-integers-to-primitive-int-array
	 */
	public static int[] toIntArray(List<Integer> integers)
	{
	    int[] ret = new int[integers.size()];
	    for (int i=0; i < ret.length; i++)
	    {
	        ret[i] = integers.get(i).intValue();
	    }
	    return ret;
	}

	public static List<Integer> toArrayList(int[] ii) {
		List<Integer> list = new ArrayList<Integer>();
		for (int i : ii)
			list.add(i);
		return list;
	}

	public static List<String> toArrayList(String[] ss) {
		List<String> list = new ArrayList<String>();
		for (String s : ss)
			list.add(s);
		return list;
	}
}
