/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * IndexedPath.java is part of SEMAFOR 2.0.
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

import java.util.List;

import edu.cmu.cs.lti.ark.util.Indexer;

/**
 * {@link Path} subclass in which elements are integers that index a list of objects.
 * The {@link #apply(List)} method retrieves the indexed objects from a list.
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2010-10-31
 *
 * @param <N> Type of objects in the indexed list
 */

public class IndexedPath<N> extends Path<Integer> implements Indexer<List<Integer>,List<N>,Path<N>> {
	private static final long serialVersionUID = -7986750107457067782L;

	public IndexedPath() { }
	public IndexedPath(IndexedPath<N> path) { super(path); }
	public IndexedPath(List<Integer> list) {
		super(list);
	}
	public IndexedPath(List<Integer> list, boolean overrideCycleCheck) {
		super(list, overrideCycleCheck);
	}
	public IndexedPath(List<Integer> list, boolean includeSource, boolean allowCycles, boolean allowSelfLoops) {
		super(list);
		this._includeSource = includeSource;
	}
	@Override
	public Path<N> apply(List<N> nodes) {
		Path<N> path = new Path<N>();
		path._includeSource = this._includeSource;
		for (Integer item : this) {
			path.add(nodes.get(item));
		}
		return path;
	}
	@Override
	public List<Integer> indices() {
		return this;
	}
	
	@Override
	public IndexedPath<N> getSuffix(int maxLength) {
		assert maxLength>0;
		return new IndexedPath<N>(this.subList(Math.max(0, this.size()-maxLength), this.size()), this._includeSource, this._allowCycles, this._allowSelfLoops);
	}
	@Override
	public IndexedPath<N> getPrefix(int maxLength) {
		assert maxLength>0;
		return new IndexedPath<N>(this.subList(0, Math.min(this.size(), this.size()+maxLength)), this._includeSource, this._allowCycles, this._allowSelfLoops);
	}
	
	@Override
	public Object clone() {
		return new IndexedPath<N>(this);
	}
}
