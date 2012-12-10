/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * DependencyParse.java is part of SEMAFOR 2.0.
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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import edu.cmu.cs.lti.ark.util.Interner;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;
import edu.cmu.cs.lti.ark.util.ds.graph.IndexComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;



/**
 * @author dipanjan
 * 2009-03-20: Refactored this class to be a subtype of ParseNode
 */
public class DependencyParse extends ParseNode<DependencyParse> {
	private static final long serialVersionUID = 876502819442937067L;
	public static final String NULL_TAG = "NULL-B";
	public static final String NULL_LEMMA = "null_lemma";
	public static final String NULL_WORD = "null_word";
	public static final String DUMMY_ROOT_LABEL = "$$";

	protected String sentence = "";

	public String getSentence() {
		return sentence;
	}	
	
	/**
	 * Sets the member variable 'sentence' to be the string of space-separated tokens
	 */
	public void processSentence() {
		final DependencyParse[] list = getIndexSortedListOfNodes();
		final ArrayList<String> headWords = Lists.newArrayList();
		for(DependencyParse node : list) {
			headWords.add(node.getHeadWord());
		}
		sentence = Joiner.on(" ").join(headWords);
	}	

	/**
	 * @param parts An array of five strings, each having \t separated tokens: <ul>
	 *  <li>parts[0] are the actual words</li>
	 * 	<li>parts[1] are the POS tags</li>
	 * 	<li>parts[2] are the syntactic labels</li>
	 * 	<li>parts[3] are the parents</li>
	 * 	<li>parts[4] are the NE tags</li></ul>
	 *  <li>parts[5] are the lemmas (from WordNet)</li></ul>
	 * @param logProb Pass 0 if you don't want to use it
	 */
	public static DependencyParse[] buildParseTrees(String[] parts, double logProb) {
		String[][][] parseData = initFive(parts);
		return process(parseData, logProb);
	}

	/**
	 * Returns a 3-dimensional array for the sentence:
	 *  - The 1st dimension indexes the word within the sentence
	 *  - The 2nd dimension indexes the type of annotation for the word (0 for the word, 1 for its POS, etc.)
	 *  - The 3rd dimension indexes the annotation series; e.g., if there is a k-best list of POS taggings of the sentence, 
	 *  	[0][1][0] will refer to the first word's POS tag in the best tagging, [0][1][1] in the second-best tagging, etc. 
	 *  	The first form of annotation (words) is assumed to have only 1 series.
	 * @param parts See {@link #buildParseTrees(String[], double)}
	 * @return
	 */
	public static String[][][] initFive(String[] parts) {
		if(parts.length != 6) {
			System.err.println("Problem. Size of input array should be 5.");
			System.exit(0);
		}
		StringTokenizer st;
		st = new StringTokenizer(parts[0],"\t");
		int count = st.countTokens();
		String[][][] parseData = new String[count][6][];
		for(int p = 0; p < 6; p ++) {
			String[] series = parts[p].split("\t\\|\\|\t");	// series are separated by \t|\t
			
			for (int j=0; j<count; j++) parseData[j][p] = new String[series.length];
			
			for (int s=0; s<series.length; s++) {
				st = new StringTokenizer(series[s],"\t");
				if(st.countTokens()!=count) {
					System.err.println("Problem. Count of line "+p+" (" + st.countTokens() + ") not equal to zeroth line (" + count + ").");
					System.exit(0);
				}
				
				// Iterate through words and update with this annotation series
				for(int j=0; j<count; j++) {
					parseData[j][p][s] = (String)Interner.globalIntern(st.nextToken());
				}
			}
		}
		return parseData;
	}

	/**
	 * @return A pair of lists of nodes: the first being all left children of the current node, from left to right; 
	 * and the second being all right children of the current node, also from left to right.
	 */
	public Pair<DependencyParse[],DependencyParse[]> getLeftAndRightChildren() {
		List<DependencyParse> leftChildren = new ArrayList<DependencyParse>();
		List<DependencyParse> rightChildren = new ArrayList<DependencyParse>();
		for (DependencyParse child : children) {
			if (child.getIndex()>this.getIndex())
				rightChildren.add(child);
			else if (child.getIndex()<this.getIndex())
				leftChildren.add(child);
		}
		return new Pair<DependencyParse[],DependencyParse[]>(DependencyParse.sortNodesByIndex(leftChildren), DependencyParse.sortNodesByIndex(rightChildren));
	}

	/**
	 * @return A list of left children of the current node, from left to right.
	 */
	public DependencyParse[] getLeftChildren() {
		return getLeftAndRightChildren().getFirst();
	}

	/**
	 * @return A list of right children of the current node, from left to right.
	 */
	public DependencyParse[] getRightChildren() {
		return getLeftAndRightChildren().getSecond();
	}

	/**
	 * @return A pair of lists of nodes: the first being all left descendants of the current node, from left to right; 
	 * and the second being all right descendants of the current node, also from left to right. If the parse is projective, 
	 * it follows that the left (right) descendants will be children of this node, or descendants of this node's left (right) children.
	 */
	public Pair<DependencyParse[],DependencyParse[]> getLeftAndRightDescendants() {
		DependencyParse[] orderedDescendants = getIndexSortedListOfNodes();
		List<DependencyParse> leftDescendants = new ArrayList<DependencyParse>();
		List<DependencyParse> rightDescendants = new ArrayList<DependencyParse>();
		for (DependencyParse desc : orderedDescendants) {	// orderedDescendants also contains the current node
			if (desc.getIndex()>this.getIndex())
				rightDescendants.add(desc);
			else if (desc.getIndex()<this.getIndex())
				leftDescendants.add(desc);
		}
		DependencyParse[] ldesc = new DependencyParse[0];
		DependencyParse[] rdesc = new DependencyParse[0];
		return new Pair<DependencyParse[],DependencyParse[]>(leftDescendants.toArray(ldesc), rightDescendants.toArray(rdesc));
	}

	/**
	 * @return A list of left descendants of the current node, from left to right. If the parse is projective,
	 * it follows that the left descendants will be children of this node, or descendants of this node's left children.
	 */
	public DependencyParse[] getLeftDescendants() {
		return getLeftAndRightDescendants().getFirst();
	}

	/**
	 * @return A list of right descendants of the current node, from left to right. If the parse is projective,
	 * it follows that the right descendants will be children of this node, or descendants of this node's right children.
	 */
	public DependencyParse[] getRightDescendants() {
		return getLeftAndRightDescendants().getSecond();
	}

	/** 
	 * Generates DependencyParse instances from string representations returned from a parser.
	 * Currently assumes word tokens and POS/NER tags will each have 1 series associated with them, 
	 * whereas dependency types and parent indices may have multiple (parallel) series corresponding to 
	 * a k-best list of parses. 
	 * @param parseData See {@link #initFive(String[])}
	 * @param logProb
	 * @return Array of k parse instances, in descending order of goodness
	 */
	public static DependencyParse[] process(String[][][] parseData, double logProb) {
		final int len = parseData.length;
		
		if (parseData[0][2].length != parseData[0][3].length) {
			System.err.println("Parse information inconsistent: " + parseData[0][2].length + " series of dependency types but " + parseData[0][3].length + " series of parent indices.");
			System.exit(1);
		}
		
		final int numParses = parseData[0][2].length;
		DependencyParse[] parses = new DependencyParse[numParses];
		
		for(int p=0; p<parseData[0][2].length; p++) {
			
			ArrayList<DependencyParse> list = new ArrayList<DependencyParse>();
			ArrayList<DependencyParse> headWords = new ArrayList<DependencyParse>();
			
			DependencyParse dummyRoot = new DependencyParse();
			dummyRoot.setParent(null);
			dummyRoot.setIndex(0);
			dummyRoot.setParentIndex(-1);
			dummyRoot.setLabelType(DependencyParse.DUMMY_ROOT_LABEL);
			dummyRoot.setWord(DependencyParse.NULL_WORD);
			dummyRoot.setLemma(DependencyParse.NULL_LEMMA);
			dummyRoot.setPOS(DependencyParse.NULL_TAG);
			dummyRoot.setNE(DependencyParse.NULL_TAG);
			dummyRoot.setDepth(0);
			dummyRoot.setLogProb(logProb);
			dummyRoot.setHeadWord(DependencyParse.NULL_WORD);	
			
			for(int j = 0; j < len; j ++) {
				DependencyParse dp = new DependencyParse();
				dp.setWord(parseData[j][0][0]);
				dp.setPOS(parseData[j][1][0]);
				dp.setNE(parseData[j][4][0]);
				dp.setLemma(parseData[j][5][0]);
				dp.setIndex(j+1);
				
				int parentIndex = Integer.parseInt(parseData[j][3][p]);
				dp.setParentIndex(parentIndex);
				dp.setLabelType(parseData[j][2][p]);
				
				dp.setHeadWord(parseData[j][0][0]);
				
				if(parentIndex==0) {
					dp.setParent(dummyRoot);
					headWords.add(dp);
				}
				else
					list.add(dp);
			}
			
			dummyRoot.setChildren(headWords);
			if(headWords.size()==0) {
				System.err.println("Head word size cannot be 0. Exiting");
				System.exit(0);
			}
			
			for(DependencyParse head : headWords) {
				head.setDepth(1);
				processChildren(list, head);
			}
				
			parses[p] = dummyRoot;
		}
	
		return parses;
	}
	
	/**
	 * 
	 * @param parseData
	 * @param logProb
	 * @return
	 */
	public static DependencyParse processFN(String[][] parseData, double logProb) {
		final int len = parseData[0].length;
		ArrayList<DependencyParse> list = new ArrayList<DependencyParse>();
		ArrayList<DependencyParse> headWords = new ArrayList<DependencyParse>();
		
		DependencyParse dummyRoot = new DependencyParse();
		dummyRoot.setParent(null);
		dummyRoot.setIndex(0);
		dummyRoot.setParentIndex(-1);
		dummyRoot.setLabelType(DependencyParse.DUMMY_ROOT_LABEL);
		dummyRoot.setWord(DependencyParse.NULL_WORD);
		dummyRoot.setPOS(DependencyParse.NULL_TAG);
		dummyRoot.setNE(DependencyParse.NULL_TAG);
		dummyRoot.setDepth(0);
		dummyRoot.setLogProb(logProb);
		dummyRoot.setHeadWord(DependencyParse.NULL_WORD);	
		
		for(int j = 0; j < len; j ++) {
			DependencyParse dp = new DependencyParse();
			dp.setWord(parseData[0][j]);
			dp.setPOS(parseData[1][j]);
			dp.setNE(parseData[4][j]);
			dp.setIndex(j+1);
			int parentIndex = new Integer(parseData[3][j]);
			dp.setParentIndex(parentIndex);
			dp.setLabelType(parseData[2][j]);
			dp.setHeadWord(parseData[0][j]);
			if(parentIndex==0) {
				dp.setParent(dummyRoot);
				headWords.add(dp);
			} else {
				list.add(dp);
			}
		}
		
		dummyRoot.setChildren(headWords);
		if(headWords.isEmpty()) {
			System.err.println("Head word size cannot be 0. Exiting");
			System.exit(0);
		}
		for(DependencyParse head : headWords) {
			head.setDepth(1);
			processChildren(list, head);
		}
		return dummyRoot;
	}

	private static void processChildren(ArrayList<DependencyParse> list, DependencyParse parent) {
		int parentIndex = parent.getIndex();
		int parentLevel = parent.getDepth();
		ArrayList<DependencyParse> children = new ArrayList<DependencyParse>();
		
		for(DependencyParse dp : list) {
			if(dp.getParentIndex() == parentIndex) {
				dp.setParent(parent);
				dp.setDepth(parentLevel+1);
				children.add(dp);
				processChildren(list, dp);
			}
		}
		parent.setChildren(children);
	}

	/**
	 * @return A list of nodes in this parse, sorted ascending by index
	 */
	public DependencyParse[] getIndexSortedListOfNodes() {
		final List<DependencyParse> nodeList = getDescendants(true);
		final int size = nodeList.size();
		final DependencyParse[] parseArray = new DependencyParse[size];
		nodeList.toArray(parseArray);
		Arrays.sort(parseArray, new IndexComparator());
		return parseArray;
	}
	
	public static DependencyParse[] sortNodesByIndex(List<DependencyParse> nodes) {
		DependencyParse[] nodeArray = new DependencyParse[nodes.size()];
		nodes.toArray(nodeArray);
		Arrays.sort(nodeArray, new IndexComparator());
		return nodeArray;
	}

	/**
	 * Uses heuristics to find the "head" node from among a span of nodes in the parse.
	 * If exactly one of these ("internal") nodes has a parent outside the span, that internal node will be returned.
	 * Otherwise, the leftmost or rightmost node having an external parent will be returned, based on POS heuristics.
	 *
	 * @param parseNodes All the nodes in the parse
	 * @param span Span of nodes whose common "head" is to be found
	 * @return "Head" node of the span
	 * @see #getHeuristicHead(DependencyParse[], int[])
	 */
	public static DependencyParse getHeuristicHead(DependencyParse[] parseNodes, Range0Based span) {
		int[] tokenNums = new int[span.length()];
		for(int i = 0; i < tokenNums.length; i++) {
			tokenNums[i] = span.getStart() + i;
		}
		return DependencyParse.getHeuristicHead(parseNodes, tokenNums);
	}
	
	/**
	 * @param parseNodes All the nodes in the parse
	 * @param tokenNums Numbers indexing nodes whose common "head" is to be found
	 * @return "Head" node of the set
	 * @see #getHeuristicHead(DependencyParse[], Range0Based)
	 */
	public static DependencyParse getHeuristicHead(DependencyParse[] parseNodes, int[] tokenNums) {
		if(tokenNums.length==1)
		{
			return parseNodes[tokenNums[0]+1];
		}
		ArrayList<Integer> listOfParents = new ArrayList<Integer>();
		String fragment = "";
		for(int tokenNum : tokenNums) {
			DependencyParse internalNode = parseNodes[tokenNum + 1];
			listOfParents.add(internalNode.getParentIndex());
			fragment += internalNode.getWord() + " ";
		}			
		fragment=fragment.trim();
		int size = listOfParents.size();
		ArrayList<Integer> internalNodesWithExternalParents = new ArrayList<Integer>();
		for(int j = 0; j < size; j ++) {
			int parentIndex = listOfParents.get(j)-1;
			
			// Check whether the candidate "head" is outside the span
			boolean isExternalParent = false;
			if(parentIndex>tokenNums[tokenNums.length-1])
				isExternalParent = true;
			else if(parentIndex<tokenNums[0])
				isExternalParent = true;
			
			if(isExternalParent)
				internalNodesWithExternalParents.add(tokenNums[j]+1);
		}
		int outsideSize = internalNodesWithExternalParents.size();
		if(outsideSize==1) {
			int outwardLookingInd = internalNodesWithExternalParents.get(0);
			return parseNodes[outwardLookingInd];
		}
		if(outsideSize==0) {
			System.err.println("Problem in DependencyParse.getHeuristicHead(). Exiting");
			System.exit(1);
		}
		DependencyParse firstNode = parseNodes[internalNodesWithExternalParents.get(0)];
		DependencyParse lastNode = parseNodes[internalNodesWithExternalParents.get(outsideSize-1)];
		if(firstNode.getPOS().startsWith("V")) return firstNode;
		if(firstNode.getPOS().startsWith("J")) return lastNode;
		if(fragment.contains("of")&&firstNode.getPOS().startsWith("N")) return firstNode;
		return lastNode;
	}
	
	public String toString() {
		return Integer.toString(index) + ":" + word + "(^=" + Integer.toString(getParent().index) + ":" + getParent().word + ")";
	}
}

