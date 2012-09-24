/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * IntArrayHashingStrategy.java is part of SEMAFOR 2.0.
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

import java.util.Arrays;

import gnu.trove.TObjectHashingStrategy;

/**
 * Hashing strategy for int[] objects based on their contents. 
 * Necessary because the last expression of:
 * <pre>
 * {@code
 * int[] x = new int[1];
 * x[0] = 1;
 * int[] y = new int[1];
 * y[0] = 1;
 * x.equals(y);
 * }</pre>
 * will be {@code false}.
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2010-10-29
 * @see {@link gnu.trove.THashMap#THashMap(TObjectHashingStrategy)}
 */
public class IntArrayHashingStrategy implements TObjectHashingStrategy<int[]> {
	private static final long serialVersionUID = -5635392346633048334L;

	public int computeHashCode(int[] o) {
		return Arrays.hashCode(o);
	}

	public boolean equals(int[] o1, int[] o2) {
		return Arrays.equals(o1, o2);
	}
}
