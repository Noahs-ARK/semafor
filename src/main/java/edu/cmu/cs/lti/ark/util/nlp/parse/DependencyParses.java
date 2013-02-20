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

import com.google.common.base.Optional;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.Range1Based;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;

/**
 * Represents a collection of predicted dependency parses for a single sentence.
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2009-06-19
 */
public class DependencyParses implements Iterable<DependencyParse>, Serializable {
	private static final long serialVersionUID = -2181832066796195517L;
	protected DependencyParse[] parses;
	protected DependencyParse[][] nodes;
	
	public DependencyParses(DependencyParse[] parses) {
		this.parses = parses;
		this.nodes = new DependencyParse[parses.length][];
	}

	public DependencyParses(DependencyParse parse) {
		this.parses = new DependencyParse[] {parse};
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
	 * Check whether a given span exactly matches a constituent (i.e. some node and all of its descendants) 
	 * in at least one parse of the sentence.
	 * @param span the span to check
	 * @return the parse that matches and its index, or absent if none matches
	 */
	public Optional<Pair<Integer,DependencyParse>> matchesSomeConstituent(Range1Based span) {
		loadAllNodes();
		return matchesSomeConstituent(span, 0);
	}
	
	private Optional<Pair<Integer,DependencyParse>> matchesSomeConstituent(Range1Based span, int start) {
		DependencyParse head = null;
		for (int p : xrange(start, parses.length)) {
			// See if any constituents of this parse match the span exactly
			boolean possibleConstituent = true;
			for (int i : span) {
				final DependencyParse node = nodes[p][i];
				final int parentIndex = node.getParent().getIndex();
				if (!span.contains(parentIndex)) {
					// n's parent outside of span
					if (head == null) {
						head = node;
					} else {
						// Multiple nodes in the span have a parent outside the span => not a constituent
						head = null;
						possibleConstituent = false;
					}
				}
				final List<DependencyParse> descendants = node.getDescendants(false);
				for (DependencyParse descendant : descendants) {
					if (!span.contains(descendant.getIndex())) {
						possibleConstituent = false;	// Some node in the span has a descendant which is outside of the span
						break;
					}
				}
				if (!possibleConstituent)
					break;
			}
			if (possibleConstituent)
				return Optional.of(new Pair<Integer,DependencyParse>(p, head));
		}
		return Optional.absent();
	}

	private void loadAllNodes() {
		for (int p = 0; p < parses.length; p++) {
			if (nodes[p] == null) {
				nodes[p] = parses[p].getIndexSortedListOfNodes();
			}
		}
	}
}
