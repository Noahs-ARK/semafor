/*******************************************************************************
 * Copyright (c) 2012 Dipanjan Das
 * Language Technologies Institute,
 * Carnegie Mellon University,
 * All Rights Reserved.
 *
 * CommandLineOptions.java is part of SEMAFOR 2.1.
 *
 * SEMAFOR 2.1 is free software: you can redistribute it and/or modify  it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SEMAFOR 2.1 is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with SEMAFOR 2.1.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package edu.cmu.cs.lti.ark.fn.parsing;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.common.io.OutputSupplier;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Token;
import edu.cmu.cs.lti.ark.fn.evaluation.PrepareFullAnnotationJson;
import edu.cmu.cs.lti.ark.fn.identification.GraphBasedFrameIdentifier;
import edu.cmu.cs.lti.ark.fn.identification.RequiredDataForFrameIdentification;
import edu.cmu.cs.lti.ark.fn.segmentation.RoteSegmenter;
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParses;
import gnu.trove.THashMap;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.transform;
import static edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec.ConllCodec;
import static edu.cmu.cs.lti.ark.fn.evaluation.PrepareFullAnnotationJson.processPredictionLine;
import static edu.cmu.cs.lti.ark.fn.identification.FrameIdentificationRelease.getTokenRepresentation;
import static edu.cmu.cs.lti.ark.fn.parsing.DataPrep.SpanAndParseIdx;
import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;
import static edu.cmu.cs.lti.ark.util.SerializedObjects.readObject;
import static org.apache.commons.io.IOUtils.closeQuietly;

public class Semafor {
	public static final String REQUIRED_DATA_FILENAME = "reqData.jobj";
	private static final String ALPHABET_FILENAME = "parser.conf";
	private static final String FRAME_ELEMENT_MAP_FILENAME = "framenet.frame.element.map";
	private static final String ARG_MODEL_FILENAME = "argmodel.dat";

	private static final Joiner TAB = Joiner.on("\t");

	protected final Set<String> allRelatedWords;
	protected final WordNetRelations wordNetRelations;
	protected final FEDict frameElementsForFrame;
	protected final RoteSegmenter segmenter;
	protected final GraphBasedFrameIdentifier idModel;
	protected final Decoding decoder;
	protected final Map<String, Integer> argIdFeatureIndex;

	/**
	 *  required flags:
	 *  model-dir
	 *  port
	 */
	public static void main(String[] args) throws Exception {
		final FNModelOptions options = new FNModelOptions(args);
		final String modelDirectory = options.modelDirectory.get();
		final int port = options.port.get();
		final File tempDirectory = Files.createTempDir();
		tempDirectory.deleteOnExit();
		runSocketServer(modelDirectory, tempDirectory, port);
	}

	public static void runSocketServer(String modelDirectory, File tempDirectory, int port)
			throws URISyntaxException, IOException, ClassNotFoundException {
		final Semafor server = getSemaforInstance(modelDirectory);
		// Set up socket server
		final ServerSocket serverSocket = new ServerSocket(port);
		System.err.println("Listening on port: " + port);
		while (true) {
			try {
				final Socket clientSocket = serverSocket.accept();
				final InputSupplier<InputStreamReader> inputSupplier = new InputSupplier<InputStreamReader>() {
					@Override public InputStreamReader getInput() throws IOException {
						return new InputStreamReader(clientSocket.getInputStream(), UTF_8);
					} };
				final OutputSupplier<OutputStreamWriter> outputSupplier = new OutputSupplier<OutputStreamWriter>() {
					@Override public OutputStreamWriter getOutput() throws IOException {
						return new OutputStreamWriter(clientSocket.getOutputStream(), UTF_8);
					} };
				server.runParser(inputSupplier, outputSupplier);
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}

	public static Semafor getSemaforInstance(String modelDirectory)
			throws IOException, ClassNotFoundException, URISyntaxException {
		final String requiredDataFilename = new File(modelDirectory, REQUIRED_DATA_FILENAME).getAbsolutePath();
		final String alphabetFilename = new File(modelDirectory, ALPHABET_FILENAME).getAbsolutePath();
		final String frameElementMapFilename = new File(modelDirectory, FRAME_ELEMENT_MAP_FILENAME).getAbsolutePath();
		final String argModelFilename = new File(modelDirectory, ARG_MODEL_FILENAME).getAbsolutePath();

		// unpack required data
		final RequiredDataForFrameIdentification r = readObject(requiredDataFilename);
		final Set<String> allRelatedWords = r.getAllRelatedWords();

		/* Initializing WordNet */
		final WordNetRelations wordNetRelations = WordNetRelations.getInstance();
		final Map<String, Set<String>> relatedWordsForWord = r.getRelatedWordsForWord();
		final Map<String, THashMap<String, Set<String>>> wordNetMap = r.getWordNetMap();
		wordNetRelations.setRelatedWordsForWord(relatedWordsForWord);
		wordNetRelations.setWordNetMap(wordNetMap);

		final GraphBasedFrameIdentifier idModel = GraphBasedFrameIdentifier.getInstance(modelDirectory);

		final RoteSegmenter segmenter = new RoteSegmenter(allRelatedWords);

		System.err.println("Initializing alphabet for argument identification..");
		final Map<String, Integer> argIdFeatureIndex = DataPrep.readFeatureIndex(new File(alphabetFilename));
		final FEDict frameElementsForFrame = FEDict.fromFile(frameElementMapFilename);

		final Decoding decoder = Decoding.fromFile(argModelFilename, alphabetFilename);

		return new Semafor(allRelatedWords,
				wordNetRelations,
				frameElementsForFrame,
				segmenter,
				idModel,
				decoder,
				argIdFeatureIndex);
	}

	public Semafor(Set<String> allRelatedWords,
				   WordNetRelations wordNetRelations,
				   FEDict frameElementsForFrame,
				   RoteSegmenter segmenter,
				   GraphBasedFrameIdentifier idModel,
				   Decoding decoder, Map<String, Integer> argIdFeatureIndex) {
		this.allRelatedWords = allRelatedWords;
		this.wordNetRelations = wordNetRelations;
		this.frameElementsForFrame = frameElementsForFrame;
		this.segmenter = segmenter;
		this.idModel = idModel;
		this.decoder = decoder;
		this.argIdFeatureIndex = argIdFeatureIndex;
	}

	public void runParser(InputSupplier<? extends Readable> input, OutputSupplier<? extends Writer> outputSupplier)
			throws IOException {
		final SentenceCodec.SentenceIterator sentences = ConllCodec.readInput(input.getInput());
		try {
			final PrintWriter output = new PrintWriter(outputSupplier.getOutput());
			try {
				while (sentences.hasNext()) {
					final SemaforParseResult result = parseSentence(sentences.next());
					output.println(result.toJson());
					output.flush();
				}
			} finally { closeQuietly(output); }
		} finally { closeQuietly(sentences); }
	}

	public SemaforParseResult parseSentence(Sentence unLemmatizedSentence) throws IOException {
		// look up lemmas
		final Sentence sentence = addLemmas(unLemmatizedSentence);
		// find targets
		final List<List<Integer>> segments = predictTargets(sentence);
		// frame identification
		final List<Pair<List<Integer>, String>> idResult = predictFrames(sentence, segments);
		// argument identification
		return predictArguments(sentence, idResult);
	}

	public List<List<Integer>> predictTargets(Sentence sentence) {
		return segmenter.getSegmentation(sentence);
	}

	private List<Pair<List<Integer>, String>> predictFrames(Sentence sentence, List<List<Integer>> targets) {
		final List<Pair<List<Integer>, String>> idResult = Lists.newArrayList();
		for (List<Integer> targetTokenIdxs : targets) {
			final String frame = idModel.getBestFrame(targetTokenIdxs, sentence);
			idResult.add(Pair.of(targetTokenIdxs, frame));
		}
		return idResult;
	}

	public SemaforParseResult predictArguments(Sentence sentence, List<Pair<List<Integer>, String>> idResults)
			throws IOException {
		final List<String> idResultLines = getArgumentIdInput(sentence, idResults);
		final List<String> argResult = predictArgumentLines(sentence, idResultLines, 1);
		return getSemaforParseResult(sentence, argResult);
	}

	/**
	 * Convert to the weird format that {@link #predictArgumentLines} expects.
	 * @param sentence the input sentence
	 * @param idResults a list of (target, frame) pairs
	 * @return a list of strings in the format that {@link #predictArgumentLines} expects.
	 */
	private List<String> getArgumentIdInput(Sentence sentence, List<Pair<List<Integer>, String>> idResults) {
		final List<String> idResultLines = Lists.newArrayList();
		final String parseLine = AllLemmaTags.makeLine(sentence.toAllLemmaTagsArray());
		for (Pair<List<Integer>, String> targetAndFrame : idResults) {
			final List<Integer> targetTokenIdxs = targetAndFrame.getFirst();
			final String frame = targetAndFrame.getSecond();
			final String tokenIdxsStr = Joiner.on("_").join(targetTokenIdxs);
			final Pair<String, String> tokenRepresentation = getTokenRepresentation(tokenIdxsStr, parseLine);
			final String lexicalUnit = tokenRepresentation.getFirst();
			final String tokenStrs = tokenRepresentation.getSecond();
			idResultLines.add(TAB.join(0, 1.0, 1, frame, lexicalUnit, tokenIdxsStr, tokenStrs, 0));
		}
		return idResultLines;
	}

	public List<String> predictArgumentLines(Sentence sentence, List<String> idResult, int kBest) throws IOException {
		final List<FrameFeatures> frameFeaturesList = Lists.newArrayList();
		for (String feLine : idResult) {
			final DataPointWithFrameElements dataPoint = new DataPointWithFrameElements(sentence, feLine);
			final String frame = dataPoint.getFrameName();
			final DependencyParses parses = dataPoint.getParses();
			final int targetStartTokenIdx = dataPoint.getTargetTokenIdxs()[0];
			final int targetEndTokenIdx = dataPoint.getTargetTokenIdxs()[dataPoint.getTargetTokenIdxs().length - 1];
			final List<SpanAndParseIdx> spans = DataPrep.findSpans(dataPoint, 1);
			final List<String> frameElements = Lists.newArrayList(frameElementsForFrame.lookupFrameElements(frame));
			final List<SpanAndCorrespondingFeatures[]> featuresAndSpanByArgument = Lists.newArrayList();
			for (String frameElement : frameElements) {
				final List<SpanAndCorrespondingFeatures> spansAndFeatures = Lists.newArrayList();
				for (SpanAndParseIdx candidateSpanAndParseIdx : spans) {
					final Range0Based span = candidateSpanAndParseIdx.span;
					final DependencyParse parse = parses.get(candidateSpanAndParseIdx.parseIdx);
					final List<String> featureSet =
							Lists.newArrayList(FeatureExtractor.extractFeatures(dataPoint, frame, frameElement, span, parse).keySet());
					final int[] featArray = convertToIdxs(featureSet);
					spansAndFeatures.add(new SpanAndCorrespondingFeatures(new int[] {span.getStart(), span.getEnd()}, featArray));
				}
				featuresAndSpanByArgument.add(spansAndFeatures.toArray(new SpanAndCorrespondingFeatures[spansAndFeatures.size()]));
			}
			final FrameFeatures frameFeatures =
					new FrameFeatures(frame,
							targetStartTokenIdx,
							targetEndTokenIdx,
							frameElements,
							featuresAndSpanByArgument);
			frameFeaturesList.add(frameFeatures);
		}
		return decoder.decodeAll(frameFeaturesList, idResult, 0, kBest);
	}

	private int[] convertToIdxs(List<String> featureSet) {
		// convert feature names to feature indexes
		final List<Integer> featureList = Lists.newArrayList();
		for (String feature : featureSet) {
			if (argIdFeatureIndex.containsKey(feature)) {
				featureList.add(argIdFeatureIndex.get(feature));
			}
		}
		final int[] featArray = new int[featureList.size()];
		for (int i : xrange(featureList.size())) featArray[i] = featureList.get(i);
		return featArray;
	}

	public SemaforParseResult getSemaforParseResult(Sentence sentence, List<String> results) {
		final List<RankedScoredRoleAssignment> roleAssignments = copyOf(transform(results, processPredictionLine));
		List<String> tokens = Lists.newArrayListWithExpectedSize(sentence.size());
		for(Token token : sentence.getTokens()) {
			tokens.add(token.getForm());
		}
		return PrepareFullAnnotationJson.getSemaforParse(roleAssignments, tokens);
	}

	public Sentence addLemmas(Sentence sentence) {
		return wordNetRelations.addLemmas(sentence);
	}
}
