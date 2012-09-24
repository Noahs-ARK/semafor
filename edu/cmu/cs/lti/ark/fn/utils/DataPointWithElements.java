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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.fn.parsing.CandidateFrameElementFilters;
import edu.cmu.cs.lti.ark.util.Interner;
import edu.cmu.cs.lti.ark.util.XmlUtils;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.Range;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParses;
import gnu.trove.THashMap;

public class DataPointWithElements extends DataPoint
{
	private final int numSpans;	// includes the target span and any frame elements
	private String[] frameElementNames;
	private ArrayList<Range0Based> frameElementTokenRanges;
	
	private String target;
	
	private static final String RE_FE = "(\t([^\\t]+)\t(\\d+([:]\\d+)?))";	// \t frame_name \t token_range
	
	public DataPointWithElements(String parseLine, String frameElementsLine, int sentNum)
	{
		this(buildParsesForLineWithKBestCache(parseLine,sentNum), frameElementsLine, null);
	}
	
	public DataPointWithElements(String parseLine, String frameElementsLine)
	{
		this(new DependencyParses(buildParsesForLine(parseLine)), frameElementsLine, null);
	}
	
	public DataPointWithElements(DependencyParses parses, String frameElementsLine, String dataSet) {
		super(parses, 0, dataSet);
		
		Pair<Integer,Pair<String,String>> parts = decomposeFELine(frameElementsLine);
		
		numSpans = parts.getFirst();
		
		String mainFramePortion = parts.getSecond().getFirst();
		this.processFrameLine(mainFramePortion);
				
		String fePortion = parts.getSecond().getSecond();
		processFrameElements(fePortion);
	}
	
	protected static Pair<Integer,Pair<String,String>> decomposeFELine(String frameElementsLine) {
		Pattern r = Pattern.compile("^(\\d+)\t([^\\t]*\t[^\\t]*\t[^\\t]*\t[^\\t]*\t\\d+)(" + RE_FE + "*)\\s*$");
		Matcher m = r.matcher(frameElementsLine);
		try {
			if (!m.find()) {
				throw new Exception("Error processing frame elements line:\n" + frameElementsLine);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
		
		int numSpans = Integer.parseInt(m.group(1));
		
		String mainFramePortion = m.group(2);
		String fePortion = m.group(3);
		
		Pair<String,String> portions = new Pair<String,String>(mainFramePortion, fePortion);
		return new Pair<Integer,Pair<String,String>>(numSpans, portions);
	}
	
	public static Pair<String,Integer> parseFrameNameAndSentenceNum(String frameElementsLine) {
		Pair<Integer,Pair<String,String>> parts = decomposeFELine(frameElementsLine);
		String mainFramePortion = parts.getSecond().getFirst();
		return DataPoint.parseFrameNameAndSentenceNum(mainFramePortion);
	}
	
	public String getTarget() {
		if (target==null) {
			target = getTokens(this.getParses().getSentence(), this.getTokenNums());
		}
		return target;
	}
	
	public void processFrameElements(String frameElementsString)
	{
		// Frame elements
		frameElementNames = new String[numSpans-1];
		frameElementTokenRanges = new ArrayList<Range0Based>(numSpans-1);
		
		int nFE = 0;
		if (!frameElementsString.trim().equals("")) {
			Matcher feM = Pattern.compile(RE_FE).matcher(frameElementsString);
			while (feM.find()) {
				String feName = (String)Interner.globalIntern(feM.group(2));
				String feSpan = feM.group(3);
				int feStart = -1;
				int feEnd = -1;
				if (feSpan.contains(":")) {	// startIndex:endIndex range
					String[] rangeParts = feSpan.split(":");
					feStart = Integer.parseInt(rangeParts[0]);
					feEnd = Integer.parseInt(rangeParts[1]);
				}
				else {	// single token in the span
					feStart = feEnd = Integer.parseInt(feSpan);
				}
				
				frameElementNames[nFE] = feName;
				frameElementTokenRanges.add(CandidateFrameElementFilters.createSpanRange(feStart, feEnd));
				nFE++;
			}
		}
		if (nFE!=numSpans-1) {	// sanity check
			System.err.println("Unable to read correct number of frame elements from string (found " + Integer.toString(nFE) + ", should be " + Integer.toString(numSpans-1) + "):\n" + frameElementsString);
			System.exit(1);
		}
		
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
		return getNumSpans()-1;
	}
	
	/**
	 * @return An array listing, in the order they were annotated in the XML file, the frame element names 
	 * (of this frame) corresponding to annotated filler spans in the sentence. The same element name may be 
	 * listed multiple times. Elements filled by null instantiations are not included.
	 */
	public String[] getOvertFilledFrameElementNames() {
		return frameElementNames;
	}
	
	/**
	 * @return A list of 0-based word token index ranges (startIndex, endIndex) (inclusive) delimiting spans which are 
	 * frame element fillers. This list is parallel to the list of frame element names returned by {@link #getOvertFilledFrameElementNames()}.
	 */
	public ArrayList<Range0Based> getOvertFrameElementFillerSpans() {
		return frameElementTokenRanges;
	}
	
	/**
	 * @param feName Frame role name
	 * @return The first filler of this role in the sentence
	 */
	public Range0Based getFillerSpan(String feName) {
		int i=0;
		for (String fe : getOvertFilledFrameElementNames()) {
			if (fe.equals(feName)) {
				return getOvertFrameElementFillerSpans().get(i);
			}
			i++;
		}
		return null;
	}
	
	public Node buildAnnotationSetNode(Document doc, int parentId, int num, String orgLine)
	{
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
	
	/**
	 * @param feFile Path to file with .frame.elements extension
	 * @param parseFile Path to file with .lemma.tags extension
	 * @return The data points listed in the specified files, i.e. the target word/phrase, its frame type and frame element fillers, 
	 * and the sentence parse
	 */
	public static List<DataPointWithElements> loadAll(String feFile, String parseFile) {
		return loadAll(feFile, parseFile, null);
	}
	
	/**
	 * @param feFile Path to file with .frame.elements extension
	 * @param parseFile Path to file with .lemma.tags extension
	 * @param dataSet One of {@link #SEMEVAL07_TRAIN_SET}, {@link #SEMEVAL07_DEV_SET}, or {@link #SEMEVAL07_TEST_SET}
	 * @return The data points listed in the specified files, i.e. the target word/phrase, its frame type and frame element fillers, 
	 * and the sentence parse
	 */
	public static List<DataPointWithElements> loadAll(String feFile, String parseFile, String dataSet) {
		return loadAll(feFile, loadAllParses(parseFile), dataSet, false);
	}
	
	/**
	 * @param feFile Path to file with .frame.elements extension
	 * @param parseFile Path to file with .lemma.tags extension
	 * @param dataSet One of {@link #SEMEVAL07_TRAIN_SET}, {@link #SEMEVAL07_DEV_SET}, or {@link #SEMEVAL07_TEST_SET}
	 * @param stripPrefix If {@literal true}, the each entry will start with "0" followed by a tab; this will be ignored.
	 * @return The data points listed in the specified files, i.e. the target word/phrase, its frame type and frame element fillers, 
	 * and the sentence parse
	 */
	public static List<DataPointWithElements> loadAll(String feFile, String parseFile, String dataSet, boolean stripPrefix) {
		return loadAll(feFile, loadAllParses(parseFile), dataSet, stripPrefix);
	}
	
	/**
	 * @param feFile Path to file with .sentences.frame.elements extension
	 * @param sentenceParses List of parses, with indices corresponding to sentence numbers in the frame elements file
	 * @see loadAllParses(String)
	 * @return
	 */
	public static List<DataPointWithElements> loadAll(String feFile, List<DependencyParses> sentenceParses) {
		return loadAll(feFile, sentenceParses, null, false);
	}
	
	/**
	 * @param feFile Path to file with .sentences.frame.elements extension
	 * @param sentenceParses List of parses, with indices corresponding to sentence numbers in the frame elements file
	 * @param dataSet One of {@link #SEMEVAL07_TRAIN_SET}, {@link #SEMEVAL07_DEV_SET}, or {@link #SEMEVAL07_TEST_SET}
	 * @param stripPrefix If {@literal true}, the each entry will start with "0" followed by a tab; this will be ignored.
	 * @see loadAllParses(String)
	 * @return
	 */
	public static List<DataPointWithElements> loadAll(String feFile, List<DependencyParses> sentenceParses, String dataSet, boolean stripPrefix) {
		List<DataPointWithElements> data = new ArrayList<DataPointWithElements>();
		List<String> frameElementLines = ParsePreparation.readSentencesFromFile(feFile);
		for (int l=0; l<frameElementLines.size(); l++) {
			String frameElementsLine = frameElementLines.get(l);
			if (stripPrefix) {
				if (!frameElementsLine.substring(0,2).equals("0\t"))
					System.err.println("Unexpected prefix in frame elements file: should be 0\\t, found " + frameElementsLine.substring(0,2));
				frameElementsLine = frameElementsLine.substring(2);
			}
			int sentenceNum = DataPointWithElements.parseFrameNameAndSentenceNum(frameElementsLine).getSecond();
			DependencyParses sentenceParseList = sentenceParses.get(sentenceNum);
			DataPointWithElements dp = new DataPointWithElements(sentenceParseList, frameElementsLine, dataSet);
			data.add(dp);
		}
		return data;
	}
	
	/**
	 * Loads a batch of frame parses from the specified files. {@link BufferedReader} instances will be 
	 * created automatically if arguments are {@code null}, and closed automatically once 
	 * the end of the files is reached. The two files must have the same number of lines.
	 * @param feFile Path to file with .frame.elements extension
	 * @param parseFile Path to file with .lemma.tags extension
	 * @param n Number of data points to load
	 * @param feReader Reader for the frame elements file stream; if {@code null}, 
	 * will be assigned a new file stream for {@code feFile}
	 * @param parseReader Reader for the preprocessed parse/tags file stream; if {@code null}, 
	 * will be assigned a new file stream for {@code parseFile}
	 * @return The next 'n' data points listed in the specified files (until the end of the file is reached)
	 * @throws IOException 
	 */
	public static Pair<List<DataPointWithElements>,BufferedReader[]> loadNextN(String feFile, String parseFile, int n, BufferedReader feReader, BufferedReader parseReader) throws IOException {
		List<DataPointWithElements> data = new ArrayList<DataPointWithElements>();
		if (feReader==null)
			feReader = new BufferedReader(new FileReader(feFile));
		if (parseReader==null)
			parseReader = new BufferedReader(new FileReader(parseFile));
		
		ArrayList<String> frameElementLines = ParsePreparation.readLinesFromFile(feReader, n, true);
		Map<Integer,String> parseLines = new THashMap<Integer,String>();
		
		if (frameElementLines.size()<n) {
			feReader = null;
		}
			
		
		for (int l=0; l<frameElementLines.size(); l++) {
			String frameElementsLine = frameElementLines.get(l);
			int sentenceNum = DataPointWithElements.parseFrameNameAndSentenceNum(frameElementsLine).getSecond();
			String parseLine = parseLines.get(sentenceNum);
			if (parseLine==null) {
				parseLine = parseReader.readLine();
				parseLines.put(sentenceNum, parseLine);
			}
	try {
			DataPointWithElements dp = new DataPointWithElements(parseLine, frameElementsLine);
			data.add(dp);
	} catch (Exception ex) { System.out.println(parseLine); System.out.println(frameElementsLine); ex.printStackTrace(); System.exit(1); }
			
		}
		
		
		return new Pair<List<DataPointWithElements>,BufferedReader[]>(data, new BufferedReader[]{feReader,parseReader});
	}
	
	public static String getTokens(String sentence, int[] intNums)
	{
		StringTokenizer st = new StringTokenizer(sentence, " ", true);
		int count = 0;
		String result="";
		Arrays.sort(intNums);
		while(st.hasMoreTokens())
		{
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
