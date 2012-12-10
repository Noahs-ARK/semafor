/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * ParseUtils.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.evaluation;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;


public class ParseUtils {
	public static final String GOLD_TARGET_SUFFIX = "#true";
	/**
	 * @param segments Lines from a .seg.data file ??
	 * @return
	 */
	public static ArrayList<String> getRightInputForFrameIdentification(List<String> segments) {
		final ArrayList<String> result = new ArrayList<String>();
		final int size = segments.size();
		for(int i : xrange(size)) {
			String line = segments.get(i).trim();
			final String[] tokens = line.split("\t");
			for(String token: tokens) {
				if(token.endsWith(GOLD_TARGET_SUFFIX)) {
					// token(s) comprise a gold target
					// the token indices for the potential target
					final String ngramIndices = token.substring(0, token.length() - GOLD_TARGET_SUFFIX.length());
					result.add(Joiner.on("\t").join("Null", ngramIndices, i));
				}
			}
		}		
		return result;
	}
}
