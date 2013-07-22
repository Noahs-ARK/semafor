/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * CandidateFrameElementFilters.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.parsing;

import java.util.List;
import java.util.Set;

import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.ds.Range;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;
import edu.cmu.cs.lti.ark.util.ds.Range1Based;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import gnu.trove.THashSet;
import gnu.trove.TObjectIntIterator;

/**
 * Classes defining heuristic approaches to identifying candidate frame element filler spans 
 * in a sentence given its parse. CandidateFrameElementFiller is the interface for these classes.
 * Training data is passed to the constructor, so it can extract counts of patterns used in filtering.
 * The method getCandidateFillerSpanRanges() is used to identify all candidate spans in a sentence; 
 * these are the only ones for which features will be extracted.
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2009-04-07
 * @see FeatureExtractor
 */
public class CandidateFrameElementFilters {
	public static final Range0Based EMPTY_SPAN = new Range0Based(-1,-1, false);	// indicates that a frame element has no overt filler
	
	public static Range0Based createSpanRange(int start, int end) {
		if (start<0 && end<0)
			return EMPTY_SPAN;
		return new Range0Based(start,end,true);
	}
	
	public static boolean isEmptySpan(Range span) {
		return span==EMPTY_SPAN;
	}
	
	public static interface CandidateFrameElementFilter {
		Set<Range0Based> getCandidateFillerSpanRanges(DependencyParse parse);
		void save(String outFile);
	}
	
	public static class First2POS_Last1POS_MinCount3 extends BoundaryPOSFilter {
		public First2POS_Last1POS_MinCount3(List<DataPointWithFrameElements> data) {
			super(data, 2, 1, 3);
		}
	}
	
	public static class First2POS_Last1POS_MinCount2 extends BoundaryPOSFilter {
		public First2POS_Last1POS_MinCount2(List<DataPointWithFrameElements> data) {
			super(data, 2, 1, 2);
		}
	}
	
	public static class BoundaryPOSFilter implements CandidateFrameElementFilter {
		private Set<String> candidates;
		private List<DataPointWithFrameElements> mData;
		private int mNFirstPOS;
		private int mNLastPOS;
		private int mMinCount;
		
		public BoundaryPOSFilter(List<DataPointWithFrameElements> data, int nFirstPOS, int nLastPOS, int minCount) {
			mData = data;
			mNFirstPOS = nFirstPOS;
			mNLastPOS = nLastPOS;
			mMinCount = minCount;
			itemizeCandidates();
		}
		
		public BoundaryPOSFilter(Set<String> candidates) {
			this.candidates = candidates;
		}
		
		public void itemizeCandidates() {
			candidates = new THashSet<String>();
			IntCounter<String> counts = new IntCounter<String>();
			for (DataPointWithFrameElements p : mData) {
				
				for (Range fillerSpanRange : p.getOvertFrameElementFillerSpans()) {
					String ss = getSufficientStatistics(p.getParses().getBestParse(), fillerSpanRange);
					counts.increment(ss);
				}
			}
			
			for (TObjectIntIterator<String> iter = counts.getIterator(); iter.hasNext();) {
				iter.advance();
				if (iter.value()>=mMinCount)
					candidates.add(iter.key());
			}
		}
		
		public Set<Range0Based> getCandidateFillerSpanRanges(DependencyParse parse) {
			DependencyParse[] nodes = parse.getIndexSortedListOfNodes();
			Set<Range0Based> candidateFillerSpanRanges = new THashSet<Range0Based>();
			for (int start=1; start<nodes.length; start++) {
				for (int end=start; end<nodes.length; end++) {
					if (isCandidate(nodes, start, end))
						candidateFillerSpanRanges.add(new Range0Based(new Range1Based(start,end)));	// for word indices, 0 = first word (not root)
				}
			}
			
			// Allow any frame element to be unfilled
			candidateFillerSpanRanges.add(EMPTY_SPAN);
			
			return candidateFillerSpanRanges;
		}
		
		public boolean isCandidate(DependencyParse[] nodes, int start, int end) {
			String ss = getSufficientStatistics(nodes, start, end);
			return (candidates.contains(ss));
		}
		
		public String getSufficientStatistics(DependencyParse parse, Range fillerSpanRange) {
			DependencyParse[] nodes = parse.getIndexSortedListOfNodes();
			return getSufficientStatistics(nodes, fillerSpanRange.start, fillerSpanRange.end);
		}
		
		/**
		 * "Sufficient statistics" refers to the information about a span (its words, tags, etc.) required 
		 * to judge whether or not it should be a candidate frame element filler. It is stored as a string.
		 * @param sentence
		 * @param start
		 * @param end
		 * @return
		 */
		public String getSufficientStatistics(DependencyParse[] sentence, int start, int end) {
			// TODO: Generalize to nFirstPOS and nLastPOS tags (right now assumes 2 and 1, respectively)
			String s = "";
			s += sentence[start].getPOS();
			if (end-start>0) {
				s += "_" + sentence[start+1].getPOS();
				if (end-start>1) {
					s += "_" + sentence[end].getPOS();
				}
			}
			return s;
		}
		
		public void save(String outFile) {
			SerializedObjects.writeSerializedObject(candidates, outFile);
		}
		
		public static BoundaryPOSFilter load(String inFile) {
			Set<String> cands = (Set<String>)SerializedObjects.readSerializedObject(inFile);
			return new BoundaryPOSFilter(cands);
		}
	}
}
