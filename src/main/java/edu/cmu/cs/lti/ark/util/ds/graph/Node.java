/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * Node.java is part of SEMAFOR 2.0.
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

import java.io.Serializable;
import java.util.Collection;

/**
 * Abstract class for graph nodes.
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2009-07-23
 *
 * @param <T>
 */
public abstract class Node<T extends Node<T>> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1327332026854046848L;
	
	/**
	 * node label, e.g. phrase structure nonterminal category or parent edge label (for labeled dependency parses)
	 */
	protected String labelType;
	/**
	 * the node's index in the tree; the only guarantee is that the root node will have index 0
	 */
	protected int index;
	/**
	 * log probability associated with the node
	 */
	protected double mLogProb;

	
	public Node() {
		super();
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int i) {
		index = i;
	}

	public String getLabelType() {
		return labelType;
	}

	public void setLabelType(String l) {
		labelType = l;
	}

	public double getLogProb() {
		return mLogProb;
	}

	public void setLogProb(double logProb) {
		mLogProb = logProb;
	}

	public abstract boolean hasNeighbors();
	public abstract int numNeighbors();
	public abstract Collection<T> getNeighbors();
	public abstract boolean isNeighborOf(T node);
}
