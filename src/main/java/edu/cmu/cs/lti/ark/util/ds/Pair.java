/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * Pair.java is part of SEMAFOR 2.0.
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


import com.google.common.base.Objects;

/**
 * Stores two objects.
 * @param <T> the type of the first object
 * @param <T2> the type of the second object
 */
public class Pair<T,T2> implements java.io.Serializable {
	private static final long serialVersionUID = 5036185774790301596L;
	
	public final T first;
	public final T2 second;
	
	
	public Pair(T o1, T2 o2) {
		first = o1;
		second = o2;
	}

	public static <U, V> Pair<U, V> of(U u, V v) {
		return new Pair<U, V>(u, v);
	}

	@Override
	public boolean equals(Object that) {
		if (that == null || !(that instanceof Pair)) return false;
		Pair thatPair = (Pair) that;
		return Objects.equal(first, thatPair.first) && Objects.equal(second, thatPair.second);
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(first, second);
	}
	
	@Override
	public Pair<T,T2> clone() throws CloneNotSupportedException {
		return new Pair<T,T2>(first, second);
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("first", first)
				.add("second", second).toString();
	}
}
