/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * DependencyParses.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.util.nlp.parse;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.Range1Based;

/**
 * Represents a collection of predicted dependency parses for a single sentence.
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2009-06-19
 */
public class DependencyParses implements Iterable<DependencyParse>, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2181832066796195517L;
	protected DependencyParse[] parses;
	protected DependencyParse[][] nodes;
	
	public DependencyParses(DependencyParse[] parses) {
		this.parses = parses;
		this.nodes = new DependencyParse[parses.length][];
	}
	public DependencyParses(DependencyParse parse) {
		this.parses = new DependencyParse[1];
		this.parses[0] = parse;
	}
	public DependencyParse get(int i) {
		return this.parses[i];
	}
	public int size() {
		return this.parses.length;
	}
	
	public Iterator<DependencyParse> iterator() {
		return Arrays.asList(parses).iterator();
	}
	
	public String getSentence() {
		return getBestParse().getSentence();
	}
	
	/**
	 * @return Number of words in the sentence, excluding the root node.
	 */
	public int getNumWords() {
		return getBestParse().getDescendants(false).size();	// exclude the root node
	}
	
	/**
	 * @return The first parse in the list
	 */
	public DependencyParse getBestParse() {
		if (this.parses.length<1 || this.parses[0]==null)
			return null;
		return this.parses[0];
	}
	
	/**
	 * Check whether a given span exactly matches a constituent (i.e. some node and its descendants) 
	 * in at least one parse of the sentence.
	 * @param span
	 * @return
	 */
	public Pair<Integer,DependencyParse> matchesSomeSubtree(Range1Based span)
	{
		return matchesSomeSubtree(span, 0);
	}
	
	public Pair<Integer,DependencyParse> matchesSomeSubtree(Range1Based span, int start)
	{
		loadAllNodes();
		DependencyParse head = null;
		for (int p=start; p<this.parses.length; p++) {	// TODO: this procedure could be optimized
			// See if any constituents of this parse match the span exactly
			boolean possibleConstit = true;
			for (int i : span)
			{
				DependencyParse n = this.nodes[p][i];
				if (!span.contains(n.getParent().getIndex())) {	// n's parent outside of span
					if (head==null)
						head = n;
					else {	// Multiple nodes in the span have a parent outside the span => not a constituent
						head = null;
						possibleConstit = false;
					}
				}
				if (!possibleConstit)
					break;
			}
			if (possibleConstit)
				return new Pair<Integer,DependencyParse>(p,head);
		}
		return null;
	}
	
	
	/**
	 * Check whether a given span exactly matches a constituent (i.e. some node and all of its descendants) 
	 * in at least one parse of the sentence.
	 * @param span
	 * @return
	 */
	public Pair<Integer,DependencyParse> matchesSomeConstituent(Range1Based span)
	{
		return matchesSomeConstituent(span, 0);
	}
	
	public Pair<Integer,DependencyParse> matchesSomeConstituent(Range1Based span, int start)
	{
		loadAllNodes();
		DependencyParse head = null;
		for (int p=start; p<this.parses.length; p++) {	// TODO: this procedure could be optimized
			// See if any constituents of this parse match the span exactly
			boolean possibleConstit = true;
			for (int i : span) {
				DependencyParse n = this.nodes[p][i];
				if (!span.contains(n.getParent().getIndex())) {	// n's parent outside of span
					if (head==null)
						head = n;
					else {	// Multiple nodes in the span have a parent outside the span => not a constituent
						head = null;
						possibleConstit = false;
					}
				}
				
				List<DependencyParse> desc = n.getDescendants(false);
				for (DependencyParse d : desc) {
					if (!span.contains(d.getIndex())) {
						possibleConstit = false;	// Some node in the span has a descendant which is outside of the span
						break;
					}
				}
				if (!possibleConstit)
					break;
			}
			if (possibleConstit)
				return new Pair<Integer,DependencyParse>(p,head);
		}
		return null;
	}
	
	
	private void loadAllNodes() {
		for (int p=0; p<this.parses.length; p++) {
			if (this.nodes[p]==null) {
				this.nodes[p] = DependencyParse.getIndexSortedListOfNodes(this.parses[p]);
			}
		}
	}
}
