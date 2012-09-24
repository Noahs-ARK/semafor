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


/**
 * Stores two objects.
 * @param <T>
 * @param <T2>
 */
public class Pair<T,T2> implements java.io.Serializable {
	private static final long serialVersionUID = 5036185774790301596L;
	
	private T ob1; // declare an object of type T
	private T2 ob2;
	
	
	public Pair(T o1, T2 o2) {
		ob1 = o1;
		ob2 = o2;
	}

	public T $1() {
		return getFirst();
	}
	public T getFirst() {
		return ob1;
	}

	public T2 $2() {
		return getSecond();
	}
	public T2 getSecond()
	{
		return ob2;
	}
	
	@Override
	public boolean equals(Object that) {
		Pair<T,T2> p2 = (Pair<T,T2>)that;
		if ((ob1==null) != (p2.ob1==null)) return false;
		if ((ob2==null) != (p2.ob2==null)) return false;
		return ((ob1==null && p2.ob1==null) || ob1.equals(p2.ob1)) && ((ob2==null && p2.ob2==null) || ob2.equals(p2.ob2));
	}
	
	@Override
	public int hashCode() {
		if (ob1==null && ob2==null)
			return 0;
		if (ob1==null)
			return ob2.hashCode() % 1499153;
		if (ob2==null)
			return ob1.hashCode() % 1499153;
		return (ob1.hashCode() + ob2.hashCode()) % 1499153;
	}
	
	@Override
	public Pair<T,T2> clone() {
		return new Pair<T,T2>(ob1, ob2);
	}
	
	@Override
	public String toString() {
		return "<" + ((ob1==null) ? "null" : ob1.toString()) + ", " + ((ob2==null) ? "null" : ob2.toString()) + ">";
	}
}
