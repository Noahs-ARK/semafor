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

import com.google.common.collect.ImmutableList;
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
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import gnu.trove.THashMap;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.transform;
import static edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec.ConllCodec;
import static edu.cmu.cs.lti.ark.util.IntRanges.range;
import static edu.cmu.cs.lti.ark.util.SerializedObjects.readObject;
import static org.apache.commons.io.IOUtils.closeQuietly;

public class Semafor {
	public static final String REQUIRED_DATA_FILENAME = "reqData.jobj";
	private static final String ALPHABET_FILENAME = "parser.conf";
	private static final String FRAME_ELEMENT_MAP_FILENAME = "framenet.frame.element.map";
	private static final String ARG_MODEL_FILENAME = "argmodel.dat";
	/* temp files */
	private static final String EVENTS_FILENAME = "events.bin";
	private static final String SPANS_FILENAME = "spans";

	protected final Set<String> allRelatedWords;
	protected final WordNetRelations wordNetRelations;
	protected final FEDict frameElementsForFrame;

	protected final RoteSegmenter segmenter;

	protected final GraphBasedFrameIdentifier idModel;
	protected final Decoding decoder;
	private final String eventsFilename;
	private final String spansFilename;

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

	public static void runSocketServer(String modelDirectory, File tempDirectory, int port) throws Exception {
		final Semafor server = getSemaforInstance(modelDirectory, tempDirectory);
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

	public static Semafor getSemaforInstance(String modelDirectory, File tempDirectory)
			throws IOException, ClassNotFoundException, URISyntaxException {
		final String requiredDataFilename = new File(modelDirectory, REQUIRED_DATA_FILENAME).getAbsolutePath();
		final String alphabetFilename = new File(modelDirectory, ALPHABET_FILENAME).getAbsolutePath();
		final String frameElementMapFilename = new File(modelDirectory, FRAME_ELEMENT_MAP_FILENAME).getAbsolutePath();
		final String argModelFilename = new File(modelDirectory, ARG_MODEL_FILENAME).getAbsolutePath();
		final File eventsFile = new File(tempDirectory, EVENTS_FILENAME);
		eventsFile.deleteOnExit();
		final String eventsFilename = eventsFile.getAbsolutePath();
		final File spansFile = new File(tempDirectory, SPANS_FILENAME);
		spansFile.deleteOnExit();
		final String spansFilename = spansFile.getAbsolutePath();


		// unpack required data
		final RequiredDataForFrameIdentification r = readObject(requiredDataFilename);
		final Set<String> allRelatedWords = r.getAllRelatedWords();

		/* Initializing WordNet config file */
		final WordNetRelations wordNetRelations = new WordNetRelations();
		final Map<String, Set<String>> relatedWordsForWord = r.getRelatedWordsForWord();
		final Map<String, THashMap<String, Set<String>>> wordNetMap = r.getWordNetMap();
		wordNetRelations.setRelatedWordsForWord(relatedWordsForWord);
		wordNetRelations.setWordNetMap(wordNetMap);

		final GraphBasedFrameIdentifier idModel = GraphBasedFrameIdentifier.getInstance(modelDirectory);

		final RoteSegmenter segmenter = new RoteSegmenter(allRelatedWords);

		System.err.println("Initializing alphabet for argument identification..");
		CreateAlphabet.setDataFileNames(alphabetFilename, frameElementMapFilename, eventsFilename, spansFilename);
		final FEDict frameElementsForFrame = new FEDict(frameElementMapFilename);

		final Decoding decoder = new Decoding();
		decoder.init(argModelFilename, alphabetFilename);

		return new Semafor(allRelatedWords,
				wordNetRelations,
				frameElementsForFrame,
				segmenter,
				idModel,
				decoder,
				eventsFilename,
				spansFilename);
	}

	public Semafor(Set<String> allRelatedWords,
				   WordNetRelations wordNetRelations,
				   FEDict frameElementsForFrame,
				   RoteSegmenter segmenter,
				   GraphBasedFrameIdentifier idModel,
				   Decoding decoder,
				   String eventsFilename,
				   String spansFilename) {
		this.allRelatedWords = allRelatedWords;
		this.wordNetRelations = wordNetRelations;
		this.frameElementsForFrame = frameElementsForFrame;
		this.segmenter = segmenter;
		this.idModel = idModel;
		this.decoder = decoder;
		this.eventsFilename = eventsFilename;
		this.spansFilename = spansFilename;
	}

	public void runParser(InputSupplier<? extends Readable> input, OutputSupplier<? extends Writer> outputSupplier)
			throws Exception {
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

	public SemaforParseResult parseSentence(Sentence unLemmatizedSentence) throws Exception {
		// look up lemmas
		final Sentence sentence = addLemmas(unLemmatizedSentence);
		// find targets
		final List<String> segments = predictTargets(sentence);
		// frame identification
		final List<String> idResult = predictFrames(sentence, segments);
		// argument identification
		return predictArguments(sentence, idResult);
	}

	public List<String> predictTargets(Sentence sentence) throws IOException {
		final List<String> allLemmaTagsSentences =
				ImmutableList.of(AllLemmaTags.makeLine(sentence.toAllLemmaTagsArray()));
		final List<String> tokenNumStrs = transform(range(allLemmaTagsSentences.size()).asList(), toStringFunction());
		return segmenter.getSegmentations(tokenNumStrs, allLemmaTagsSentences);
	}

	public List<String> predictFrames(Sentence sentence, List<String> segments) throws IOException {
		final List<String> allLemmaTagsSentences =
				ImmutableList.of(AllLemmaTags.makeLine(sentence.toAllLemmaTagsArray()));
		return ParserDriver.identifyFrames(idModel, allLemmaTagsSentences, segments);
	}

	public SemaforParseResult predictArguments(Sentence sentence, List<String> idResult) throws Exception {
		final List<String> argResult = predictArgumentLines(sentence, idResult, 1);
		System.out.println("argResult:");
		for (String line : argResult) {
			System.out.println(line);
		}
		return getSemaforParseResult(sentence, argResult);
	}

	public List<String> predictArgumentLines(Sentence sentence, List<String> idResult, int kBest) throws Exception {
		final List<String> allLemmaTagsSentences =
				ImmutableList.of(AllLemmaTags.makeLine(sentence.toAllLemmaTagsArray()));
		return ParserDriver.identifyArguments(wordNetRelations, eventsFilename, spansFilename, decoder, 0, idResult,
				allLemmaTagsSentences, kBest);
	}

	public SemaforParseResult getSemaforParseResult(Sentence sentence, List<String> results) {
		final List<RankedScoredRoleAssignment> roleAssignments =
				copyOf(transform(results, PrepareFullAnnotationJson.processPredictionLine));
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
