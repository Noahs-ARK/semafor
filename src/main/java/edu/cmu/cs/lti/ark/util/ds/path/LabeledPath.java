/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * LabeledPath.java is part of SEMAFOR 2.0.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.cmu.cs.lti.ark.util.ds.Pair;

/**
 * Subtype of {@link Path} in which each edge between consecutive elements has an associated label.
 * Implemented with each node and its <b>incoming</b> edge label stored as a {@link Pair} in the path.
 * The source node, if included, is stored with {@code null} because it has no incoming label. 
 * 
 * Cycle- and self-loop-checking, if applicable, will be performed only on the <element, label> pairs; 
 * thus, an element may appear twice with different incoming edge labels.
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2010-10-31
 *
 * @param <E> Element type
 * @param <L> Label type
 */

public class LabeledPath<E,L> extends Path<Pair<E,L>> {
	private static final long serialVersionUID = 7407679683275056183L;
	
	public LabeledPath() {
		
	}
	public LabeledPath(List<Pair<E,L>> that) {
		super(that);
		if (_includeSource) {
			assert getStart().second ==null;	// source should not have an associated incoming label
		}
	}
	public LabeledPath(List<Pair<E,L>> entries, boolean allowCycles, boolean allowSelfLoops) {
		super(entries, allowCycles, allowSelfLoops);
		if (_includeSource) {
			assert getStart().second ==null;	// source should not have an associated incoming label
		}
	}
	public LabeledPath(List<Pair<E,L>> entries, boolean includeSource, boolean allowCycles, boolean allowSelfLoops) {
		super(entries, allowCycles, allowSelfLoops);
		this._includeSource = includeSource;
		if (_includeSource) {
			assert getStart().second ==null;	// source should not have an associated incoming label
		}
	}
	public LabeledPath(LabeledPath<E,L> that) {
		super(that);
	}
	public LabeledPath(Iterable<E> items, Iterable<L> labels) {
		this(items,false,labels);
	}
	public LabeledPath(Iterable<E> items, boolean includesSource, Iterable<L> labels) {
		super(mergePathEntriesWithLabels(items,includesSource,labels));
		_includeSource = includesSource;
	}
	
	public List<E> getEntries() {
		List<E> entries = new ArrayList<E>();
		for (Pair<E, L> item : this) {
			entries.add(item.first);
		}
		return entries;
	}
	public List<L> getLabels() {
		List<L> labels = new ArrayList<L>();
		for (Pair<E, L> item : this) {
			labels.add(item.second);
		}
		return labels;
	}
	
	@Override
	public Object getNode(int i) {
		return get(i).first;
	}
	@Override
	public Object getSource() {
		if (_includeSource) {
			return getStart().first;
		}
		return null;
	}
	@Override
	public Object getTarget() {
		return getEnd().first;
	}
	
	/** Return a new LabeledPath which is the suffix of this path of up to length 'maxLength'. 
	 * If this suffix does not include this path's source, 'includeSource' will be set to false. */
	@Override
	public LabeledPath<E,L> getSuffix(int maxLength) {
		assert maxLength>=0;
		List<Pair<E,L>> pre = new ArrayList<Pair<E,L>>(this.subList(Math.max(0, this.size()-maxLength), this.size()));
		return new LabeledPath<E,L>(pre, maxLength>=this.size(), this._allowCycles, this._allowSelfLoops);
	}
	/** Like {@link #getSuffix(int)}, but sets the new source's label to {@code null} if applicable. */
	public LabeledPath<E,L> getSuffixPath(int maxLength) {
		assert maxLength>=0;
		List<Pair<E,L>> pre = new ArrayList<Pair<E,L>>(this.subList(Math.max(0, this.size()-maxLength), this.size()));
		if (_includeSource) {
			pre.set(0, new Pair<E,L>(pre.get(0).first,null));
		}
		return new LabeledPath<E,L>(pre, this._includeSource, this._allowCycles, this._allowSelfLoops);

	}
	@Override
	public LabeledPath<E,L> getPrefix(int maxLength) {
		assert maxLength>=0;
		return new LabeledPath<E,L>(this.subList(0, Math.min(this.size(), this.size()+maxLength)), 
				this._includeSource, this._allowCycles, this._allowSelfLoops);
	}
	
	public boolean add(E newElt, L newLabel) {
		return super.add(new Pair<E,L>(newElt, newLabel));
	}
	
	@Override
	public Path<Pair<E,L>> extend(Pair<E,L> newItem) {
		Path<Pair<E,L>> r = super.extend(newItem);
		return r;
	}
	
	@Override
	public Path<Pair<E,L>> extend(Path<Pair<E,L>> that) {
		Path<Pair<E,L>> r = super.extend(that);
		return r;
	}
	
	/** Returns a copy of the current path but with an additional item appended to the end. */
	public Path<Pair<E,L>> extend(E newElt, L newLabel) {
		return this.extend(new Pair<E,L>(newElt, newLabel));
	}
	
	@Override
	public Path<Pair<E,L>> shift(Pair<E,L> newItem) {
		Path<Pair<E,L>> newPath = this.extend(newItem);
		newPath.remove(0);
		if (this._includeSource) {
			E newSource = newPath.get(0).first;
			newPath.set(0, new Pair<E,L>(newSource,null));
		}
		return newPath;
	}
	
	/** Returns a copy of the current path but with the first item removed and a new item appended to the end. */
	public Path<Pair<E,L>> shift(E newElt, L newLabel) {
		Path<Pair<E,L>> newPath = this.extend(newElt, newLabel);
		newPath.remove(0);
		if (this._includeSource) {
			E newSource = newPath.get(0).first;
			newPath.set(0, new Pair<E,L>(newSource,null));
		}
		return newPath;
	}
	
	@Override
	public boolean sameItem(Pair<E,L> a, Pair<E,L> b) {
		if ((a.first ==null) != (b.first ==null)) return false;
		return (a.first ==null && b.first ==null) || a.first.equals(b.first);
	}
	
	@Override
	public Object clone() {
		return new LabeledPath<E,L>(this);
	}
	
	public static <E,L> List<Pair<E,L>> mergePathEntriesWithLabels(Iterable<E> items, boolean includesSource, Iterable<L> labels) {
		List<Pair<E,L>> list = new ArrayList<Pair<E,L>>();
		Iterator<E> itemsI = items.iterator();
		Iterator<L> labelsI = labels.iterator();
		if (includesSource)
			list.add(new Pair<E,L>(itemsI.next(), null));	// starting point of the path
		while (labelsI.hasNext()) {
			list.add(new Pair<E,L>(itemsI.next(), labelsI.next()));	// non-initial point in the path and the label of the edge between it and the previous point
		}
		assert !itemsI.hasNext();
		return list;
	}
	public Path<E> toUnlabeledPath() {
		return new Path<E>(this.getEntries());
	}
}
