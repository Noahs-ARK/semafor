/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * DataPointWithElements.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.utils;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.parsing.CandidateFrameElementFilters;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParses;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import static edu.cmu.cs.lti.ark.fn.parsing.CandidateFrameElementFilters.createSpanRange;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;

public class DataPointWithFrameElements extends DataPoint {
	private final static Joiner TAB_JOINER = Joiner.on("\t");

	private final int numSpans;	// includes the target span and any frame elements
	private final List<FrameElementAndSpan> frameElementsAndSpans;
	public String target;
	public final int rank;
	public final double score;

	public static class FrameElementAndSpan {
		public final String name;
		public final Range0Based span;

		public FrameElementAndSpan(String name, Range0Based span) {
			this.name = name;
			this.span = span;
		}
	}

	/**
	 * Frame line that's only been partly broken down
	 */
	protected static class CountFrameAndElements {
		public final int rank;
		public final double score;
		public final int count;
		public final String frame;
		public final String elements;

		public CountFrameAndElements(int rank, double score, int count, String frame, String elements) {
			this.rank = rank;
			this.score = score;
			this.count = count;
			this.frame = frame;
			this.elements = elements;
		}
	}

	public DataPointWithFrameElements(String parseLine, String frameElementsLine) {
		this(new DependencyParses(buildParsesForLine(parseLine)), frameElementsLine, null);
	}

	public DataPointWithFrameElements(Sentence sentence, String frameElementsLine) {
		this(AllLemmaTags.makeLine(sentence.toAllLemmaTagsArray()), frameElementsLine);
	}

	public DataPointWithFrameElements(DependencyParses parses, String frameElementsLine, String dataSet) {
		super(parses, dataSet);
		final CountFrameAndElements parts = decomposeFELine(frameElementsLine);
		numSpans = parts.count;
		rank = parts.rank;
		score = parts.score;
		processFrameLine(parts.frame);
		frameElementsAndSpans = processFrameElements(parts.elements);
	}

	/**
	 * @param frameElementsLine a string encoded in the frame.elements format
	 * @return @code{(numSpans, (mainFramePortion, fePortion))}
	 */
	protected static CountFrameAndElements decomposeFELine(String frameElementsLine) {
		String[] tokens = frameElementsLine.trim().split("\t");
		int rank = parseInt(tokens[0]);
		double score = parseDouble(tokens[1]);
		int numSpans = parseInt(tokens[2]);
		final List<String> tokenList = asList(tokens);
		String mainFramePortion = TAB_JOINER.join(tokenList.subList(3, 8));
		String fePortion = TAB_JOINER.join(tokenList.subList(8, tokens.length));
		return new CountFrameAndElements(rank, score, numSpans, mainFramePortion, fePortion);
	}
	
	public static Pair<String,Integer> parseFrameNameAndSentenceNum(String frameElementsLine) {
		final CountFrameAndElements parts = decomposeFELine(frameElementsLine);
		return DataPoint.parseFrameNameAndSentenceNum(parts.frame);
	}

	public ImmutableList<FrameElementAndSpan> processFrameElements(String frameElementsString) {
		// Frame elements
		final ImmutableList.Builder<FrameElementAndSpan> fesAndSpans = ImmutableList.builder();
		final String trimmed = frameElementsString.trim();
		if (!trimmed.equals("")) {
			String[] tokens = trimmed.split("\t");
			for (int i = 0; i < tokens.length; i += 2) {
				final String feName = tokens[i];
				final String feSpan = tokens[i+1];
				fesAndSpans.add(new FrameElementAndSpan(feName, getSpan(feSpan)));
			}
		}
		return fesAndSpans.build();
	}

	public static Range0Based getSpan(String feSpan) {
		int feStart;
		int feEnd;
		if (feSpan.contains(":")) {
			// startIndex:endIndex range
			String[] rangeParts = feSpan.split(":");
			feStart = parseInt(rangeParts[0]);
			feEnd = parseInt(rangeParts[1]);
		} else {
			// single token in the span
			feStart = feEnd = parseInt(feSpan);
		}
		return createSpanRange(feStart, feEnd);
	}

	/**
	 * Produces a frame elements line representation of a specified frame annotation. 
	 * Result does not end in a newline.
	 * 
	 * @param arguments Map from role names to filler argument token ranges
	 * @param frameName
	 * @param lexicalUnit
	 * @param tokenNums Token numbers for the target
	 * @param target The target word(s), separated by spaces
	 * @param sentNum
	 * @return
	 */
	public static String makeFrameElementsLine(Map<String,Range0Based> arguments, String frameName, String lexicalUnit, int[] tokenNums, String target, int sentNum) {
		String s = makeFrameLine(frameName, lexicalUnit, tokenNums, target, sentNum) + "\t";
		int numNonemptySpans = 0;
		for (Map.Entry<String,Range0Based> argument : arguments.entrySet()) {
			Range0Based span = argument.getValue();
			if (CandidateFrameElementFilters.isEmptySpan(span))	// unfilled FE
				continue;
			String rangeS = ""+ span.start;
			if (span.length()>1)
				rangeS += ":" + (span.start +span.length());
			s += argument.getKey() + "\t" + rangeS + "\t";
			numNonemptySpans++;
		}
		return (numNonemptySpans+1) + "\t" + s.trim();
	}
	
	public int getNumSpans() {
		return numSpans;
	}
	
	/**
	 * @return The number of spans in the sentence annotated with frame elements of this frame. 
	 * Does not include null instantiations (INI, DNI, CNI).
	 */
	public int getNumOvertFrameElementFillers() {
		return getNumSpans() - 1;
	}
	
	/**
	 * @return An array listing, in the order they were annotated in the XML file, the frame element names 
	 * (of this frame) corresponding to annotated filler spans in the sentence. The same element name may be 
	 * listed multiple times. Elements filled by null instantiations are not included.
	 */
	public List<String> getOvertFilledFrameElementNames() {
		return Lists.transform(frameElementsAndSpans, new Function<FrameElementAndSpan, String>() {
			@Nullable @Override public String apply(FrameElementAndSpan input) {
				return input.name;
			}});
	}
	
	/**
	 * @return A list of 0-based word token index ranges (startIndex, endIndex) (inclusive) delimiting spans which are 
	 * frame element fillers. This list is parallel to the list of frame element names returned by
	 * {@link #getOvertFilledFrameElementNames()}.
	 */
	public List<Range0Based> getOvertFrameElementFillerSpans() {
		return Lists.transform(frameElementsAndSpans, new Function<FrameElementAndSpan, Range0Based>() {
			@Nullable @Override public Range0Based apply(FrameElementAndSpan input) {
				return input.span;
			}});
	}

	public List<FrameElementAndSpan> getFrameElementsAndSpans() {
		return frameElementsAndSpans;
	}

	public static String getTokens(String sentence, int[] intNums) {
		StringTokenizer st = new StringTokenizer(sentence, " ", true);
		int count = 0;
		String result="";
		Arrays.sort(intNums);
		while(st.hasMoreTokens()) {
			String token = st.nextToken().trim().intern();
			if(token.equals(""))
				continue;
			if(Arrays.binarySearch(intNums, count)>=0)
				result+=token+" ";
			count++;
		}
		return result.trim();
	}
}
