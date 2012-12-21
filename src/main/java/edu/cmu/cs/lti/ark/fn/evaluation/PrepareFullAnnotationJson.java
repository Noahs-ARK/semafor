/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute,
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * PrepareFullAnnotationJson.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import edu.cmu.cs.lti.ark.fn.parsing.SemaforParse;
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithElements;
import edu.cmu.cs.lti.ark.util.ds.Range;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;

import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.transform;
import static edu.cmu.cs.lti.ark.fn.parsing.SemaforParse.Frame;
import static edu.cmu.cs.lti.ark.fn.parsing.SemaforParse.Frame.Span;
import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.readLines;

/**
 * Collates intermediate output files into a final output file
 * Writes one json object (representing a Semafor-parsed sentence) per line
 * @see SemaforParse
 *
 * @author Sam Thomson (sthomson@cs.cmu.edu)
 */
public class PrepareFullAnnotationJson {
	private static final ObjectMapper jsonMapper = new ObjectMapper();
	// gets the sentence index given a frame.elements line
	private static final Function<String,Integer> getSentenceIndex = new Function<String, Integer>() {
		@Nullable @Override public Integer apply(@Nullable String input) {
			return DataPointWithElements.parseFrameNameAndSentenceNum(input).getSecond();
		}
	};

	/**
	 * Generates the json representation of a set of predicted semantic parses
	 * 
	 * @param args Options to specify:
	 *   testFEPredictionsFile
	 *   testParseFile 
	 *   testTokenizedFile 
	 *   outputFile
	 * @see #writeJsonForPredictions
	 */
	public static void main(String[] args) throws Exception {
		ParseOptions options = new ParseOptions(args);
		writeJsonForPredictions(
				options.testFEPredictionsFile,
				options.testParseFile,
				options.testTokenizedFile,
				options.outputFile);
	}
	
	/**
	 * Generates the JSON representation of a set of predicted semantic parses
	 *
	 * @param testFEPredictionsFile Path to MapReduce output of the parser, formatted as frame elements lines
	 * @param testParseFile Dependency parses for each sentence in the data
	 * @param testTokenizedFile File Original form of each sentence in the data
	 * @param outputFile Where to store the resulting json
	 */
	public static void writeJsonForPredictions(String testFEPredictionsFile,
											   String testParseFile,
											   String testTokenizedFile,
											   String outputFile) throws Exception {
		final FileReader tokenizedInput = new FileReader(testTokenizedFile);
		try {
			final FileReader parseInput = new FileReader(testParseFile);
			try {
				final FileReader feInput = new FileReader(testFEPredictionsFile);
				try {
					final BufferedWriter output = new BufferedWriter(new FileWriter(new File(outputFile)));
					try {
						writeJsonForPredictions(tokenizedInput, feInput, parseInput, output);
					} finally { closeQuietly(output); }
				} finally { closeQuietly(feInput); }
			} finally { closeQuietly(parseInput); }
		} finally { closeQuietly(tokenizedInput); }
	}

	public static void writeJsonForPredictions(Reader tokenizedInput,
											   Reader frameElementsInput,
											   Reader parseInput,
											   Writer output) throws IOException {
		final List<String> tokenizedLines = readLines(tokenizedInput);
		final Multimap<Integer, String> predictions = readFrameElementLines(frameElementsInput);
		final List<String> parses = readLines(parseInput);
		writeJson(predictions, parses, tokenizedLines, output);
	}

	/**
	 * Reads predicted frame elements from testFEPredictionsFile and groups them by sentence index
	 *
	 * @param feInput the input stream from which to read predicted frame elements
	 * @return a map from sentence num to a set of predicted frame elements for that sentence
	 * @throws IOException if there is a problem reading from the file
	 */
	private static Multimap<Integer, String> readFrameElementLines(Reader feInput) throws IOException {
		List<String> lines = readLines(feInput);
		// output of MapReduce--will have an extra number and tab at the beginning of each line
		// throw away the first field
		lines = transform(lines, new Function<String, String>() {
			@Override public String apply(String input) {
				return input.substring(input.indexOf('\t') + 1).trim();
			}
		});
		// group by sentence index
		return Multimaps.index(lines, getSentenceIndex);
	}

	private static void writeJson(Multimap<Integer, String> predictions,
								  List<String> parses,
								  List<String> tokenizedLines,
								  Writer output) throws IOException {
		for(int i : xrange(parses.size())) {
			final String parseLine = parses.get(i);
			final Collection<String> feLines = predictions.get(i);
			final String tokenizedLine = tokenizedLines.get(i);
			final SemaforParse semaforParse = getSemaforParse(parseLine, feLines, tokenizedLine);
			output.write(jsonMapper.writeValueAsString(semaforParse) + "\n");
		}
	}

	/**
	 * Given predicted frame instances, including their frame elements, create a SemaforParse ready to be serialized to
	 * JSON
	 *
	 * @param parseLine Line encoding the dependency parse of the sentence
	 * @param feLines Lines encoding predicted frames & FEs in the same format as the .sentences.frame.elements files
	 * @param tokenizedLine The original sentence, space-separated
	 */
	private static SemaforParse getSemaforParse(String parseLine, Iterable<String> feLines, String tokenizedLine) {
		final List<String> tokens = Arrays.asList(tokenizedLine.split(" "));
		final ArrayList<Frame> frames = Lists.newArrayList();
		for (String feLine : feLines) {
			final DataPointWithElements dp = new DataPointWithElements(parseLine, feLine);
			dp.processOrgLine(tokenizedLine);
			// extract target
			final int[] indices = dp.getTokenNums();
			final Span target = makeSpan(indices[0], indices[indices.length - 1] + 1, dp.getFrameName(), tokens);
			// extract frame elements
			final List<Range0Based> ranges = dp.getOvertFrameElementFillerSpans();
			final String[] feNames = dp.getOvertFilledFrameElementNames();
			final ArrayList<Span> frameElements = Lists.newArrayList();
			for (int i : xrange(feNames.length)) {
				final Range range = ranges.get(i);
				frameElements.add(makeSpan(range.getStart(), range.getEnd() + 1, feNames[i], tokens));
			}
			frames.add(new Frame(target, frameElements));
		}
		return new SemaforParse(frames, tokens);
	}

	private static Span makeSpan(int start, int end, String name, List<String> tokens) {
		return new Span(start, end, name, Joiner.on(" ").join(tokens.subList(start, end)));
	}
}
