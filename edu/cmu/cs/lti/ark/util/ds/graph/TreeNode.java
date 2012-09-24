/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * TreeNode.java is part of SEMAFOR 2.0.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.lti.ark.util.ds.Pair;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;

/**
 * Node in a tree structure. 
 * The root node should have index 0 and no parent; all other nodes have index > 0 and one parent.
 * 
 * @param T Node type; same as the name of the subclass being defined (proxy for SELF_TYPE)
 * @author Nathan Schneider (nschneid)
 * @since 2009-04-04
 */

public abstract class TreeNode<T extends TreeNode<T>> extends RootedDAGNode<T> {
	
	private static final long serialVersionUID = -5923423841213967909L;


	public TreeNode() {
		
	}
	
	public void setParentIndex(int i) {
		List<Integer> pIndices = new ArrayList<Integer>(1);
		pIndices.add(i);
		try {
			setParentIndices(pIndices);
		}
		catch (GraphException gex) {
			;
		}
	}

	public int getParentIndex() {
		if (parentIndices==null || parentIndices.size()==0)
			return -1;
		return getParentIndices().get(0);
	}

	public T getParent() {
		if (parents==null || parents.size()==0)
			return null;
		return parents.get(0);
	}

	public void setParent(T p) {
		List<T> plist = new ArrayList<T>(1);
		if (p!=null)
			plist.add(p);
		
		try {
			setParents(plist);
		}
		catch (GraphException gex) {
			;
		}
	}
	
	/**
	 * @throws GraphException if attempting to add multiple parents
	 */
	public void setParents(List<T> p) throws GraphException {
		if (p!=null && p.size()>1)
			throw new GraphException("Multiple parents are not allowed in TreeNode");
		super.setParents(p);
	}
	
	/**
	 * @throws GraphException if attempting to add indices for multiple parents
	 */
	public void setParentIndices(List<Integer> p) throws GraphException {
		if (p!=null && p.size()>1)
			throw new GraphException("Multiple parents are not allowed in TreeNode");
		super.setParentIndices(p);
	}
	
	public void setChildren(List<T> c) {
		try {
			super.setChildren(c);
		}
		catch (GraphException gex) {
			;
		}
	}

	/**
	 * @param includeSelf If true, the current node will be included in the list, along with its descendants
	 * @return A list of all descendants, in no particular order
	 */
	@SuppressWarnings("unchecked")
	public List<T> getDescendants(boolean includeSelf) {
		List<T> nodeList = new ArrayList<T>();
		if (includeSelf)
			nodeList.add((T)this);
		
		if (!this.hasChildren())
			return nodeList;
		
		List<T> plist = this.getChildren();
		for (T node : plist)
			nodeList.addAll(node.getDescendants(true));
		
		return nodeList;
	}

	/**
	 * @return List of indices of ancestor nodes, starting with the current node and ending with the shallowest ancestor (root of the tree).
	 * @author Nathan Schneider (nschneid)
	 */
	public TIntArrayList getAncestry() {
		TIntArrayList ancestry = new TIntArrayList();
		T eldest = getAncestorAtLevel(0, ancestry);
		ancestry.add(eldest.index);
		return ancestry;
	}

	public T getAncestorAtLevel(int alevel) {
		return getAncestorAtLevel(alevel, null);
	}

	public T getAncestorAtLevel(int alevel, TIntArrayList ancPath) {
		return getAncestorAtLevel(alevel, ancPath, null);
	}

	/**
	 * 
	 * @param alevel Level (depth) of the desired ancestor
	 * @param ancPath A list to be populated with indices of nodes searched (starting with the current node), provided that an ancestor node at the specified level exists; or null.
	 * @param alreadySearched Indices of nodes which have already been searched. If non-null, 'this' will be returned if (and only if) a member of this set is encountered in the ancestor path. 
	 * @return Ancestor node of the current node whose level is 'alevel', or null if no such ancestor exists. A node is not considered to be its own ancestor.
	 * @author Nathan Schneider (nschneid)
	 */
	@SuppressWarnings("unchecked")
	public T getAncestorAtLevel(int alevel, TIntArrayList ancPath, TIntHashSet alreadySearched) {
		if (alevel < 0 || alevel >= this.depth)	// A node at this level is not strictly an ancestor
			return null;
		
		TreeNode<T> node = this;
		for (int d=this.depth; d>alevel; d--) {
			if (ancPath!=null)
				ancPath.add(node.index);
			if (alreadySearched!=null && alreadySearched.contains(node.index))
				return (T)this;
			node = node.getParent();
		}
		return (T)node;
	}

	public boolean isAncestor(T anc) {
		return isAncestor(anc, null);
	}

	/**
	 * 
	 * @param anc Alleged ancestor node
	 * @param ancPath A list in which to store the indices of nodes that have been searched, or null
	 * @return true if 'anc' is an ancestor of this node, otherwise false. A node is not considered its own ancestor.
	 * @author Nathan Schneider (nschneid)
	 */
	public boolean isAncestor(T anc, TIntArrayList ancPath) {
		T trueAncestor = getAncestorAtLevel(anc.depth, ancPath);
		return (trueAncestor==anc);
	}

	public T nearestCommonAncestorWith(T that, TIntHashSet searched) {
		return nearestCommonAncestorWith(that, searched, null);
	}

	/**
	 * 
	 * @param that Second node
	 * @param searched If non-null, a list that will be populated with indices of nodes that have been searched
	 * @param alreadySearched If non-null, 'this' will be returned as soon as any ancestors of 'that' are encountered whose indices belong to this set
	 * @return The nearest (deepest) common ancestor node between 'this' and 'that': either 'this', 'that', some third node, or null (if they have 
	 * no common ancestor, though this should not occur if it is a tree)
	 * @author Nathan Schneider (nschneid)
	 */
	@SuppressWarnings("unchecked")
	public T nearestCommonAncestorWith(T that, TIntHashSet searched,
			TIntHashSet alreadySearched) {
				if (alreadySearched!=null && alreadySearched.contains(that.index))
					return (T)this;
				
				TreeNode<T> n1 = this;
				T n2 = that;
				if (this.depth < that.depth) {
					TIntArrayList ancPath = new TIntArrayList();
					n1 = this;
					n2 = that.getAncestorAtLevel(this.depth, ancPath, alreadySearched);
					if (searched!=null)
						searched.addAll(ancPath.toNativeArray());
					if (alreadySearched!=null && (n2==that || alreadySearched.contains(n2.index)))
						return (T)this;
				}
				else if (that.depth < this.depth) {
					TIntArrayList ancPath = new TIntArrayList();
					n1 = that;
					n2 = this.getAncestorAtLevel(that.depth, ancPath);
					if (searched!=null)
						searched.addAll(ancPath.toNativeArray());
				}
				
				while (n1 != n2) {
					if (searched!=null) {
						searched.add(n1.index);
						searched.add(n2.index);
					}
					n1 = n1.getParent();
					n2 = n2.getParent();
				}
				
				return (T)n1;
			}

	public static <T extends TreeNode<T>> T nearestCommonAncestor(
			Set<T> nodes) {
				return nearestCommonAncestor(nodes, null);
			}
	
	/**
	 * 
	 * @param nodes A set of nodes in a tree
	 * @param searched If non-null, a list which will be populated with indices of the nodes that have been searched
	 * @return The nearest (deepest) common ancestor among a set of nodes, which may belong to that set; or null if there is no common ancestor (should not occur if a tree)
	 * @author Nathan Schneider (nschneid)
	 */
	public static <T extends TreeNode<T>> T nearestCommonAncestor(Set<T> nodes, TIntHashSet searched) {
		TIntHashSet alreadySearched = searched;
		if (searched==null)
			alreadySearched = new TIntHashSet();
		
		T best = null;
		int i = 0;
		for (T next : nodes) {
			if (i++==0) {
				best = next;
				continue;
			}
			TIntHashSet srched = new TIntHashSet();
			best = best.nearestCommonAncestorWith(next, srched, alreadySearched);
			if (best==null)
				break;
			alreadySearched.addAll(srched.toArray());
		}
		return best;
	}


	/**
	 * @param <T> TreeNode subtype
	 * @param from Origin node in the tree
	 * @param to Destination node in the tree
	 * @return Path from the origin to the destination, represented as a list of pairs with the node in the path 
	 * and the direction of traversal: "^" for moving upwards (to the parent) and "!" for moving downwards (to a child). 
	 * The first node in this list is 'from', marked with "="; the last node in the list is 'to'.
	 */
	public static <T extends TreeNode<T>> List<Pair<String,T>> getPath(T from, T to) {
		List<Pair<String,T>> result = new ArrayList<Pair<String,T>>();
		result.add(new Pair<String,T>("=",from));
		if (to==from)
			return result;
		T nca = from.nearestCommonAncestorWith(to, null);
		
		{
			T node = from;
			while (node.getDepth()>nca.getDepth()) {
				node = node.getParent();
				result.add(new Pair<String,T>("^",node));
			}
			if (node!=nca) {
				System.err.println("Error finding path between tree nodes (1)");
				System.exit(1);
			}
		}
		
		{
			List<Pair<String,T>> tail = new ArrayList<Pair<String,T>>();
			T node2 = to;
			while (node2.getDepth()>nca.getDepth()) {
				tail.add(0, new Pair<String,T>("!",node2));
				node2 = node2.getParent();
			}
			if (node2!=nca) {
				System.err.println("Error finding path between tree nodes (2)");
				System.exit(1);
			}
			result.addAll(tail);
		}
		return result;
	}
}
