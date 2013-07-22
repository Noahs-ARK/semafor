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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.*;
import edu.cmu.cs.lti.ark.fn.parsing.RankedScoredRoleAssignment;
import edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult;
import edu.cmu.cs.lti.ark.util.ds.*;

import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.transform;
import static edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult.Frame;
import static edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult.Frame.NamedSpanSet;
import static edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult.Frame.Span;
import static edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements.FrameElementAndSpan;
import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.readLines;

/**
 * Collates intermediate output files into a final output file
 * Writes one json object (representing a Semafor-parsed sentence) per line
 * @see edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult
 *
 * @author Sam Thomson (sthomson@cs.cmu.edu)
 */
public class PrepareFullAnnotationJson {
	private static final Function<RankedScoredRoleAssignment,Integer> getSentenceIndex =
			new Function<RankedScoredRoleAssignment, Integer>() {
				@Nullable @Override public Integer apply(RankedScoredRoleAssignment input) {
					return input.sentenceIdx;
				}
			};
	public static final Function<String,RankedScoredRoleAssignment> processPredictionLine =
			new Function<String, RankedScoredRoleAssignment>() {
				@Nullable @Override public RankedScoredRoleAssignment apply(@Nullable String input) {
					return RankedScoredRoleAssignment.fromPredictionLine(input);
				}
			};
	private static final Function<RankedScoredRoleAssignment,Range0Based> getTargetSpan =
			new Function<RankedScoredRoleAssignment, Range0Based>() {
				@Override public Range0Based apply(RankedScoredRoleAssignment input) {
					return input.targetSpan;
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
				options.testTokenizedFile,
				options.outputFile);
	}
	
	/**
	 * Generates the JSON representation of a set of predicted semantic parses
	 *
	 * @param testFEPredictionsFile Path to MapReduce output of the parser, formatted as frame elements lines
	 * @param testTokenizedFile File Original form of each sentence in the data
	 * @param outputFile Where to store the resulting json
	 */
	public static void writeJsonForPredictions(String testFEPredictionsFile,
											   String testTokenizedFile,
											   String outputFile) throws Exception {
		final FileReader tokenizedInput = new FileReader(testTokenizedFile);
		try {
			final FileReader feInput = new FileReader(testFEPredictionsFile);
			try {
				final BufferedWriter output = new BufferedWriter(new FileWriter(new File(outputFile)));
				try {
					writeJsonForPredictions(tokenizedInput, feInput, output);
				} finally { closeQuietly(output); }
			} finally { closeQuietly(feInput); }
		} finally { closeQuietly(tokenizedInput); }
	}

	public static void writeJsonForPredictions(Reader tokenizedInput,
											   Reader frameElementsInput,
											   Writer output) throws IOException {
		final List<String> tokenizedLines = readLines(tokenizedInput);
		final Multimap<Integer, RankedScoredRoleAssignment> predictions =
				parseRoleAssignments(readLines(frameElementsInput));
		writeJson(predictions, tokenizedLines, output);
	}

	/**
	 * Reads predicted frame elements from testFEPredictionsFile and groups them by sentence index
	 *
	 * @param lines the predicted frame elements
	 * @return a map from sentence num to a set of predicted frame elements for that sentence
	 * @throws IOException if there is a problem reading from the file
	 */
	public static Multimap<Integer, RankedScoredRoleAssignment> parseRoleAssignments(List<String> lines) {
		final List<RankedScoredRoleAssignment> roleAssignments = copyOf(transform(lines, processPredictionLine));
		// group by sentence index
		return Multimaps.index(roleAssignments, getSentenceIndex);
	}

	private static void writeJson(Multimap<Integer, RankedScoredRoleAssignment> predictions,
								  List<String> tokenizedLines,
								  Writer output) throws IOException {
		for(int i : xrange(tokenizedLines.size())) {
			final Collection<RankedScoredRoleAssignment> predictionsForSentence = predictions.get(i);
			final List<String> tokens = Arrays.asList(tokenizedLines.get(i).split(" "));
			final SemaforParseResult semaforParseResult = getSemaforParse(predictionsForSentence, tokens);
			output.write(semaforParseResult.toJson() + "\n");
		}
	}

	/**
	 * Given predicted frame instances, including their frame elements, create a SemaforParseResult ready to be serialized to
	 * JSON
	 *
	 * @param rankedScoredRoleAssignments Lines encoding predicted frames & FEs in the same format as the .sentences.frame.elements files
	 */
	public static SemaforParseResult getSemaforParse(Collection<RankedScoredRoleAssignment> rankedScoredRoleAssignments,
													 List<String> tokens) {
		final ArrayList<Frame> frames = Lists.newArrayList();
		// group by target span (assumes only one predicted frame per target span)
		final ImmutableListMultimap<Range0Based,RankedScoredRoleAssignment> predictionsByFrame =
				Multimaps.index(rankedScoredRoleAssignments, getTargetSpan);
		for (Range0Based targetSpan : predictionsByFrame.keySet()) {
			final List<RankedScoredRoleAssignment> predictionsForFrame = predictionsByFrame.get(targetSpan);
			final RankedScoredRoleAssignment first = predictionsForFrame.get(0);
			final NamedSpanSet target = makeSpan(first.targetSpan.start, first.targetSpan.end + 1, first.frame, tokens);
			final List<Frame.ScoredRoleAssignment> scoredRoleAssignments = Lists.newArrayList();
			for (RankedScoredRoleAssignment ra : predictionsForFrame) {
				// extract frame elements
				final List<FrameElementAndSpan> frameElementsAndSpans = ra.fesAndSpans;
				final List<NamedSpanSet> frameElements = Lists.newArrayList();
				for(FrameElementAndSpan frameElementAndSpan : frameElementsAndSpans) {
					final Range0Based range = frameElementAndSpan.span;
					frameElements.add(makeSpan(range.start, range.end + 1, frameElementAndSpan.name, tokens));
				}
				scoredRoleAssignments.add(new Frame.ScoredRoleAssignment(ra.rank, ra.score, frameElements));
			}
			frames.add(new Frame(target, scoredRoleAssignments));
		}
		return new SemaforParseResult(frames, tokens);
	}

	private static NamedSpanSet makeSpan(int start, int end, String name, List<String> tokens) {
		final ImmutableList<Span> spans =
				ImmutableList.of(new Span(start, end, Joiner.on(" ").join(tokens.subList(start, end))));
		return new NamedSpanSet(name, spans);
	}
}
