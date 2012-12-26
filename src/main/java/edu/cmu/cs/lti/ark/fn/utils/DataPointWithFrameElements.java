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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import edu.cmu.cs.lti.ark.fn.parsing.CandidateFrameElementFilters;
import edu.cmu.cs.lti.ark.util.Interner;
import edu.cmu.cs.lti.ark.util.XmlUtils;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.Range;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParses;

import javax.annotation.Nullable;

import static edu.cmu.cs.lti.ark.fn.parsing.CandidateFrameElementFilters.createSpanRange;
import static java.lang.Integer.parseInt;

public class DataPointWithFrameElements extends DataPoint
{
	private final int numSpans;	// includes the target span and any frame elements

	private List<FrameElementAndSpan> frameElementsAndSpans;
	
	private String target;
	
	private static final String RE_FE = "(\t([^\\t]+)\t(\\d+([:]\\d+)?))";	// \t frame_name \t token_range

	public static class FrameElementAndSpan {
		public final String name;
		public final Range0Based span;

		public FrameElementAndSpan(String name, Range0Based span) {
			this.name = name;
			this.span = span;
		}
	}

	protected static class CountFrameAndElements {
		public final int count;
		public final String frame;
		public final String elements;

		public CountFrameAndElements(int count, String frame, String elements) {
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
		processFrameLine(parts.frame);
		frameElementsAndSpans = processFrameElements(parts.elements);
	}

	/**
	 *
	 * @param frameElementsLine a string encoded in the frame.elements format
	 * @return @code{(numSpans, (mainFramePortion, fePortion))}
	 */
	protected static CountFrameAndElements decomposeFELine(String frameElementsLine) {
		Pattern r = Pattern.compile("^(\\d+)\t([^\\t]*\t[^\\t]*\t[^\\t]*\t[^\\t]*\t\\d+)(" + RE_FE + "*)\\s*$");
		Matcher m = r.matcher(frameElementsLine);
		if (!m.find()) {
			throw new RuntimeException("Error processing frame elements line:\n" + frameElementsLine);
		}
		int numSpans = parseInt(m.group(1));
		String mainFramePortion = m.group(2);
		String fePortion = m.group(3);
		return new CountFrameAndElements(numSpans, mainFramePortion, fePortion);
	}
	
	public static Pair<String,Integer> parseFrameNameAndSentenceNum(String frameElementsLine) {
		final CountFrameAndElements parts = decomposeFELine(frameElementsLine);
		return DataPoint.parseFrameNameAndSentenceNum(parts.frame);
	}

	public ImmutableList<FrameElementAndSpan> processFrameElements(String frameElementsString) {
		// Frame elements
		ImmutableList.Builder<FrameElementAndSpan> fesAndSpans = ImmutableList.builder();
		
		int i = 0;
		if (!frameElementsString.trim().equals("")) {
			Matcher feM = Pattern.compile(RE_FE).matcher(frameElementsString);
			while (feM.find()) {
				final String feName = (String)Interner.globalIntern(feM.group(2));
				final String feSpan = feM.group(3);
				final Range0Based span = getSpan(feSpan);
				fesAndSpans.add(new FrameElementAndSpan(feName, span));
				i++;
			}
		}
		if (i != numSpans-1) {
			// sanity check
			System.err.println("Unable to read correct number of frame elements from string (found " + Integer.toString(i) + ", should be " + Integer.toString(numSpans-1) + "):\n" + frameElementsString);
			System.exit(1);
		}
		return fesAndSpans.build();
	}

	private Range0Based getSpan(String feSpan) {
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
			String rangeS = ""+span.getStart();
			if (span.length()>1)
				rangeS += ":" + (span.getStart()+span.length());
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
	public String[] getOvertFilledFrameElementNames() {
		List<String> names = Lists.transform(frameElementsAndSpans, new Function<FrameElementAndSpan, String>() {
			@Nullable @Override public String apply(FrameElementAndSpan input) {
				return input.name;
			}});
		return names.toArray(new String[names.size()]);
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

	public Node buildAnnotationSetNode(Document doc, int parentId, int num, String orgLine) {
		Node node = super.buildAnnotationSetNode(doc, parentId, num, orgLine);
		
		Node layers = XmlUtils.applyXPath(node, "layers")[0];
		Node feLayer = doc.createElement("layer");
		int setId = parentId*100+num;
		int layerId = setId*100+2;
		XmlUtils.addAttribute(doc,"ID", (Element)feLayer,""+layerId);
		XmlUtils.addAttribute(doc,"name", (Element)feLayer,"FE");
		layers.appendChild(feLayer);
		Node labels = doc.createElement("labels");
		feLayer.appendChild(labels);
		
		List<Range0Based> fillerSpans = getOvertFrameElementFillerSpans();
		String[] feNames = getOvertFilledFrameElementNames();
		for (int i=0; i<feNames.length; i++) {
			String feName = feNames[i];
			Range fillerSpan = fillerSpans.get(i);
			
			int labelId = layerId*100+i+1;
			Node label = doc.createElement("label");
			XmlUtils.addAttribute(doc,"ID", (Element)label,""+labelId);
			XmlUtils.addAttribute(doc,"name", (Element)label,feName);
			
			int startCharIndex = getCharacterIndicesForToken(fillerSpan.getStart()).getStart();
			int endCharIndex = getCharacterIndicesForToken(fillerSpan.getEnd()).getEnd();
			XmlUtils.addAttribute(doc,"start", (Element)label,""+startCharIndex);
			XmlUtils.addAttribute(doc,"end", (Element)label,""+endCharIndex);
			labels.appendChild(label);
		}
		
		return node;
	}

	public static String getTokens(String sentence, int[] intNums) {
		StringTokenizer st = new StringTokenizer(sentence, " ", true);
		int count = 0;
		String result="";
		Arrays.sort(intNums);
		while(st.hasMoreTokens()) {
			String token = (String)Interner.globalIntern(st.nextToken().trim());
			if(token.equals(""))
				continue;
			if(Arrays.binarySearch(intNums, count)>=0)
				result+=token+" ";
			count++;
		}
		return result.trim();
	}
}
