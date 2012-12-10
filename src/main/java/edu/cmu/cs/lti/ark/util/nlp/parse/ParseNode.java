/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * ParseNode.java is part of SEMAFOR 2.0.
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

import edu.cmu.cs.lti.ark.util.ds.graph.TreeNode;



/**
 * Information common to dependency parses and phrase structure parses
 * @param T Node type; same as the name of the subclass being defined (proxy for SELF_TYPE)
 * @author Nathan Schneider (nschneid)
 * @since 2009-03-20 (refactored from DependencyParse class)
 */
public abstract class ParseNode<T extends ParseNode<T>> extends TreeNode<T> {
	
	private static final long serialVersionUID = 1791340656040399935L;
	
	/**
	 * actual word at this node, or null if not a terminal (leaf) node
	 */
	protected String word;
	protected String lemma;
	/**
	 * part-of-speech tag at this node (will be null if not a terminal node)
	 */
	protected String pos;
	
	/**
	 * named entity tag at this node (will be null if not a terminal node)
	 */
	protected String ne;
	
	/**
	 * if lexicalization is used, the head word of the subtree anchored by this node
	 */
	protected String headWord = null;
	
	public void setWord(String w) {
		word = w;
	}
	
	public String getWord() {
		return word;
	}
	public void setLemma(String l) {
		lemma = l;
	}
	
	public String getLemma() {
		return lemma;
	}
	public void setHeadWord(String w) {
		headWord = w;
	}
	
	public String getHeadWord() {
		return headWord;
	}
	
	public void setPOS(String pos) {
		this.pos = pos;
	}
	
	public String getPOS() {
		return pos;
	}
	
	public void setNE(String ne) {
		this.ne = ne;
	}
	
	public String getNE() {
		return ne;
	}
	
}
