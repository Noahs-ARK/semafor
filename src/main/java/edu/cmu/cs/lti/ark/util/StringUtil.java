/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * StringUtil.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.util;

import java.util.Iterator;

/**
 * Utilities for working with strings.
 * @author Nathan Schneider (nschneid)
 * @since 2009-09-24
 */
public class StringUtil {
	
	/**
	 * Source: http://snippets.dzone.com/posts/show/91
	 * @param <T>
	 * @param objs
	 * @param delimiter
	 * @return String representations of the Iterable object's elements joined together with the given delimiter
	 */
	public static <T> String join(final Iterable<T> objs, final String delimiter) {
	    Iterator<T> iter = objs.iterator();
	    if (!iter.hasNext())
	        return "";
	    StringBuffer buffer = new StringBuffer(String.valueOf(iter.next()));
	    while (iter.hasNext())
	        buffer.append(delimiter).append(String.valueOf(iter.next()));
	    return buffer.toString();
	}

	public static String join(Object[] items, String delimiter) {
		StringBuffer buffer = new StringBuffer("");
		boolean first = true;
		for (Object item : items) {
			if (!first)
				buffer.append(delimiter);
			buffer.append(String.valueOf(item));
			first = false;
		}
		return buffer.toString();
	}
}
