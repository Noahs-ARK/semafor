/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * Path.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.util.ds.path;

import gnu.trove.THashSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Stores a sequence of elements in a path, where edges implicitly connect consecutive elements.
 * The elements may be of any type (not necessarily graph nodes).
 * The path source (starting point) may be included as the first element, or may be omitted.
 * By default, a path will enforce prohibitions on self-loops (consecutive repetitions of an element) 
 * and cycles (non-consecutive repetitions of an element).
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2010-10-31
 *
 * @param <T> Element type
 */
public class Path<T> extends ArrayList<T> {
	private static final long serialVersionUID = 2211480191768461681L;
	
	protected boolean _includeSource = false;
	protected boolean _allowCycles = false;
	protected boolean _allowSelfLoops = false;
	
	public Path() {
		
	}
	public Path(Path<T> that) {
		super(that);
		this._includeSource = that._includeSource;
		this._allowCycles = that._allowCycles;
		this._allowSelfLoops = that._allowSelfLoops;
	}
	public Path(List<T> list) {
		this(list,false);
	}
	
	/** If {@code overrideCycleCheck} is {@code true}, the cycle constraints will not be enforced 
	 * during object construction, though they will (by default) apply to elements that are added later.
	 * 
	 * @see #add(Object, boolean)
	 */
	public Path(List<T> list, boolean overrideCycleCheck) {
		super(list);
		
		if (overrideCycleCheck)
			return;
		
		// enforce cycle rules
		if (_allowCycles && _allowSelfLoops)
			return;
		else if (!_allowCycles && !_allowSelfLoops) {
			Set<T> set = new THashSet<T>(list);
			assert set.size()==list.size();	// ensure uniqueness
		}
		else if (!_allowCycles) {	// self-loops OK
			Set<T> set = new THashSet<T>();
			T prev = null;
			boolean first = true;
			for (T elt : list) {
				if (!first) {
					if (!elt.equals(prev)) {
						assert !set.contains(elt);
						set.add(elt);
					}
				}
				else
					first = false;
				prev = elt;
			}
		}
		else {	// cycles OK, but not self-loops
			T prev = null;
			boolean first = true;
			for (T elt : list) {
				if (!first)
					assert !elt.equals(prev);
				else
					first = false;
				prev = elt;
			}
		}
	}
	public Path(List<T> list, boolean allowCycles, boolean allowSelfLoops) {
		super(list);
		this._allowCycles = allowCycles;
		this._allowSelfLoops = allowSelfLoops;
	}
	public void setIncludeSource(boolean includeSource) {
		assert size()==0;
		_includeSource = includeSource;
	}
	public boolean isSourceIncluded() { return _includeSource; }
	public T getStart() {
		return this.get(0);
	}
	public T getEnd() {
		return this.get(this.size()-1);
	}
	/** Defaults to {@link #get(int)}, but can be overloaded by subclasses. */
	public Object getNode(int i) {
		return this.get(i);
	}
	/** Defaults to {@link #getStart()}, but can be overloaded by subclasses. */
	public Object getSource() {
		return getStart();
	}
	/** Defaults to {@link #getEnd()}, but can be overloaded by subclasses. */
	public Object getTarget() {
		return getEnd();
	}
	public int getNumEdges() {
		return this.size();
	}
	public Path<T> getTail() {
		return getSuffix(this.size()-1);
	}
	public Path<T> getHead() {
		return getPrefix(this.size()-1);
	}
	public Path<T> getSuffix(int maxLength) {
		assert maxLength>=0;	// could be ==0 in call from getTail()
		return new Path<T>(this.subList(Math.max(0, this.size()-maxLength), this.size()));
	}
	public Path<T> getPrefix(int maxLength) {
		assert maxLength>=0;	// could be ==0 in call from getHead()
		return new Path<T>(this.subList(0, Math.min(this.size(), this.size()+maxLength)));
	}
	
	@Override
	public boolean add(T item) {
		return add(item, false);
	}
	
	public boolean add(T item, boolean overrideCycleCheck) {
		if (!overrideCycleCheck) {
			if (!_allowCycles && !_allowSelfLoops)
				assert !this.contains(item);
			else if (!_allowSelfLoops)	// cycles OK, but there must be an intervening non-equal element
				assert this.size()==0 || !this.get(this.size()-1).equals(item);
			else if (!_allowCycles) {	// self loops OK, but there must be no intervening non-equal elements
				int i = this.indexOf(item);
				if (i>-1) {
					for (T elt : this.subList(i, this.size()))
						assert elt.equals(item);
				}
			}
		}
			
		return super.add(item);
	}
	
	@Override
	public boolean addAll(Collection<? extends T> items) {
		boolean result = true;
		for (T item : items)
			result = result && add(item);	// this will ensure the cycle properties are enforced
		return result;
	}
	
	/** Returns a copy of the current path but with an additional item appended to the end. */
	@SuppressWarnings("unchecked")
	public Path<T> extend(T newItem) {
		Path<T> newPath = (Path<T>)this.clone();
		newPath.add(newItem);
		return newPath;
	}
	
	/** Returns a copy of the current path but with the elements of the provided path concatenated onto the end. */
	@SuppressWarnings("unchecked")
	public Path<T> extend(Path<T> that) {
		Path<T> newPath = (Path<T>)clone();
		if (that._includeSource) {	// the source of the second path should match the endpoint of the first
			assert sameItem(this.getStart(), that.getStart());
			newPath.addAll(that.subList(1, that.size()));
		}
		else	// simply concatentate the two lists
			newPath.addAll(that);
		return newPath;
	}
	
	/** Returns a copy of the current path but with the first item removed and a new item appended to the end. 
	 * Note: any active cycle constraints will be enforced on the extended path BEFORE the first element is removed.
	 */
	public Path<T> shift(T newItem) {
		Path<T> newPath = this.extend(newItem);
		newPath.remove(0);
		return newPath;
	}
	
	/** Determine whether two items are the same for the purpose of concatenating paths together */
	public boolean sameItem(T a, T b) {
		return a.equals(b);
	}
	
	@Override
	public Object clone() {
		return new Path<T>(this);
	}
	
	@Override
	public boolean equals(Object that) {
		Path<?> p = (Path<?>)that;
		return super.equals(that) && p._allowCycles==this._allowCycles && 
			p._allowSelfLoops==this._allowSelfLoops && p._includeSource==this._includeSource;
	}
	
	@Override
	public int hashCode() {
		int x = super.hashCode();
		if (this._allowCycles)
			x += 16;
		if (this._allowSelfLoops)
			x += 64;
		if (this._includeSource)
			x += 256;
		return x;
	}
	
	/** Unit tests focused on cycle-checking. Run with -ea to enable assertions in the VM. */
	public static void main(String[] args) {
		Path<String> p;
		p = new Path<String>(Arrays.asList(new String[]{"x","y","z"}));
		try {
			p = p.shift("x");
		} catch (AssertionError ex) {
			System.out.println("OK0");
		}
		p = new Path<String>(Arrays.asList(new String[]{"x","y","z","x","g"}), true, false);
		p = p.extend("y");
		try {
			p = p.extend("y");
		} catch (AssertionError ex) {
			System.out.println("OK1");
		}
		p = new Path<String>(Arrays.asList(new String[]{"x","y","y","x","g"}), true, true);
		p = new Path<String>(Arrays.asList(new String[]{"x","x","y","z","g","g"}), false, true);
		p.add("g");
		try {
			p.add("x");
		} catch (AssertionError ex) {
			System.out.println("OK2");
		}
		try {
			p = new Path<String>(Arrays.asList(new String[]{"x","y","z","x","g"}));	// cycle: x
		} catch (AssertionError ex) {
			System.out.println("OK3");
		}
		try {
			p = new Path<String>(Arrays.asList(new String[]{"x","y","y","g"}));	// self-loop: y
		} catch (AssertionError ex) {
			System.out.println("OK4");
		}
		try {
			p = new Path<String>(Arrays.asList(new String[]{"x","y","y","x","g"}));	// self-loop: y; cycle: x
		} catch (AssertionError ex) {
			System.out.println("OK5");
		}
	}
	
}
