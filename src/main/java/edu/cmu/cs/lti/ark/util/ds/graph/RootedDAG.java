/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * RootedDAG.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.util.ds.graph;

import java.util.List;


public class RootedDAG<N extends RootedDAGNode<N>> {

	protected boolean allowParallelEdges = false;
	protected N _root;

	public RootedDAG(N root) {
		_root = root;
	}

	public N getRoot() {
		return _root;
	}

	public List<N> topologicalSort() throws GraphException {
		return topologicalSort(this);
	}
	
	public static <N extends RootedDAGNode<N>> List<N> topologicalSort(RootedDAG<N> dag) throws GraphException {
		return RootedDAGNode.topologicalSort(dag.getRoot());
	}
}
