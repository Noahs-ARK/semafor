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
package edu.cmu.cs.lti.ark.fn;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.common.io.OutputSupplier;
import com.google.common.primitives.Ints;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Token;
import edu.cmu.cs.lti.ark.fn.evaluation.PrepareFullAnnotationJson;
import edu.cmu.cs.lti.ark.fn.identification.GraphBasedFrameIdentifier;
import edu.cmu.cs.lti.ark.fn.identification.RequiredDataForFrameIdentification;
import edu.cmu.cs.lti.ark.fn.parsing.*;
import edu.cmu.cs.lti.ark.fn.segmentation.RoteSegmenter;
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;
import edu.cmu.cs.lti.ark.util.nlp.Lemmatizer;
import edu.cmu.cs.lti.ark.util.nlp.MorphaLemmatizer;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParses;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.transform;
import static edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec.ConllCodec;
import static edu.cmu.cs.lti.ark.fn.evaluation.PrepareFullAnnotationJson.processPredictionLine;
import static edu.cmu.cs.lti.ark.fn.identification.FrameIdentificationRelease.getTokenRepresentation;
import static edu.cmu.cs.lti.ark.fn.parsing.DataPrep.SpanAndParseIdx;
import static edu.cmu.cs.lti.ark.util.SerializedObjects.readObject;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.apache.commons.io.IOUtils.closeQuietly;

public class Semafor {
	public static final String REQUIRED_DATA_FILENAME = "reqData.jobj";
	private static final String ALPHABET_FILENAME = "parser.conf";
	private static final String FRAME_ELEMENT_MAP_FILENAME = "framenet.frame.element.map";
	private static final String ARG_MODEL_FILENAME = "argmodel.dat";

	private static final Joiner TAB = Joiner.on("\t");

	protected final Set<String> allRelatedWords;
	protected final FEDict frameElementsForFrame;
	protected final RoteSegmenter segmenter;
	protected final GraphBasedFrameIdentifier idModel;
	protected final Decoding decoder;
	protected final Map<String, Integer> argIdFeatureIndex;
	protected final Lemmatizer lemmatizer = new MorphaLemmatizer();

	/**
	 * required flags:
	 * model-dir
	 * input-file
	 * output-file
	 */
	public static void main(String[] args) throws Exception {
		final FNModelOptions options = new FNModelOptions(args);
		final File inputFile = new File(options.inputFile.get());
		final File outputFile = new File(options.outputFile.get());
		final String modelDirectory = options.modelDirectory.get();
		final int numThreads = options.numThreads.present() ? options.numThreads.get() : 1;
		final Semafor semafor = getSemaforInstance(modelDirectory);
		semafor.runParser(
				Files.newReaderSupplier(inputFile, Charsets.UTF_8),
				Files.newWriterSupplier(outputFile, Charsets.UTF_8),
				numThreads);
	}

	public Semafor(Set<String> allRelatedWords,
				   FEDict frameElementsForFrame,
				   RoteSegmenter segmenter,
				   GraphBasedFrameIdentifier idModel,
				   Decoding decoder, Map<String, Integer> argIdFeatureIndex) {
		this.allRelatedWords = allRelatedWords;
		this.frameElementsForFrame = frameElementsForFrame;
		this.segmenter = segmenter;
		this.idModel = idModel;
		this.decoder = decoder;
		this.argIdFeatureIndex = argIdFeatureIndex;
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
		final GraphBasedFrameIdentifier idModel = GraphBasedFrameIdentifier.getInstance(modelDirectory);
		final RoteSegmenter segmenter = new RoteSegmenter(allRelatedWords);
		System.err.println("Initializing alphabet for argument identification..");
		final Map<String, Integer> argIdFeatureIndex = DataPrep.readFeatureIndex(new File(alphabetFilename));
		final FEDict frameElementsForFrame = FEDict.fromFile(frameElementMapFilename);
		final Decoding decoder = Decoding.fromFile(argModelFilename, alphabetFilename);
		return new Semafor(allRelatedWords,
				frameElementsForFrame,
				segmenter,
				idModel,
				decoder,
				argIdFeatureIndex);
	}

	/**
	 * Reads conll sentences, parses them, and writes the json-serialized results.
	 *
	 * @param inputSupplier where to read conll sentences from
	 * @param outputSupplier where to write the results to
	 * @param numThreads the number of threads to use
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void runParser(final InputSupplier<? extends Readable> inputSupplier,
						  final OutputSupplier<? extends Writer> outputSupplier,
						  final int numThreads)
			throws IOException, InterruptedException {
		// use the producer-worker-consumer pattern to parse all sentences in multiple threads, while keeping
		// output in order.
		final BlockingQueue<Future<Optional<SemaforParseResult>>> results =
				Queues.newLinkedBlockingDeque(5 * numThreads);
		final ExecutorService workerThreadPool = newFixedThreadPool(numThreads);
		// try to shutdown gracefully. don't worry too much if it doesn't work
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override public void run() {
				try {
					workerThreadPool.shutdown();
					workerThreadPool.awaitTermination(5, TimeUnit.SECONDS);
				} catch (InterruptedException ignored) { }
			} }));

		final PrintWriter output = new PrintWriter(outputSupplier.getOutput());
		try {
			// Start thread to fetch computed results and write to file
			final Thread consumer = new Thread(new Runnable() {
				@Override public void run() {
					while (!Thread.currentThread().isInterrupted()) {
						try {
							final Optional<SemaforParseResult> oResult = results.take().get();
							if (!oResult.isPresent()) break; // got poison pill. we're done
							output.println(oResult.get().toJson());
							output.flush();
						} catch (Exception e) {
							e.printStackTrace();
							throw new RuntimeException(e);
						}
					}
				} });
			consumer.start();
			// in main thread, put placeholders on results queue (so results stay in order), then
			// tell a worker thread to fill up the placeholder
			final SentenceCodec.SentenceIterator sentences = ConllCodec.readInput(inputSupplier.getInput());
			try {
				int i = 0;
				while (sentences.hasNext()) {
					final Sentence sentence = sentences.next();
					final int sentenceId = i;
					results.put(workerThreadPool.submit(new Callable<Optional<SemaforParseResult>>() {
						@Override public Optional<SemaforParseResult> call() throws Exception {
							final long start = System.currentTimeMillis();
							try {
								final SemaforParseResult result = parseSentence(sentence);
								final long end = System.currentTimeMillis();
								System.err.printf("parsed sentence %d in %d millis.%n", sentenceId, end - start);
								return Optional.of(result);
							} catch (Exception e) {
								e.printStackTrace();
								throw e;
							}
						} }));
					i++;
				}
				// put a poison pill on the queue to signal that we're done
				results.put(workerThreadPool.submit(new Callable<Optional<SemaforParseResult>>() {
					@Override public Optional<SemaforParseResult> call() throws Exception {
						return Optional.absent();
					} }));
				workerThreadPool.shutdown();
			} finally { closeQuietly(sentences); }
			// wait for consumer to finish
			consumer.join();
		} finally { closeQuietly(output); }
		System.err.println("Done.");
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

	public List<Pair<List<Integer>, String>> predictFrames(Sentence sentence, List<List<Integer>> targets) {
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
	public List<String> getArgumentIdInput(Sentence sentence, List<Pair<List<Integer>, String>> idResults) {
		final List<String> idResultLines = Lists.newArrayList();
		final String parseLine = AllLemmaTags.makeLine(sentence.toAllLemmaTagsArray());
		for (Pair<List<Integer>, String> targetAndFrame : idResults) {
			final List<Integer> targetTokenIdxs = targetAndFrame.first;
			final String frame = targetAndFrame.second;
			final String tokenIdxsStr = Joiner.on("_").join(targetTokenIdxs);
			final Pair<String, String> tokenRepresentation = getTokenRepresentation(tokenIdxsStr, parseLine);
			final String lexicalUnit = tokenRepresentation.first;
			final String tokenStrs = tokenRepresentation.second;
			idResultLines.add(TAB.join(0, 1.0, 1, frame, lexicalUnit, tokenIdxsStr, tokenStrs, 0));
		}
		return idResultLines;
	}

	public List<String> predictArgumentLines(Sentence sentence, List<String> idResult, int kBest) throws IOException {
		final List<FrameFeatures> frameFeaturesList = Lists.newArrayList();
		final FeatureExtractor featureExtractor = new FeatureExtractor();
		for (String feLine : idResult) {
			final DataPointWithFrameElements dataPoint = new DataPointWithFrameElements(sentence, feLine);
			final String frame = dataPoint.getFrameName();
			final DependencyParses parses = dataPoint.getParses();
			final int targetStartTokenIdx = dataPoint.getTargetTokenIdxs()[0];
			final int targetEndTokenIdx = dataPoint.getTargetTokenIdxs()[dataPoint.getTargetTokenIdxs().length-1];
			final List<SpanAndParseIdx> spans = DataPrep.findSpans(dataPoint, 1);
			final List<String> frameElements = Lists.newArrayList(frameElementsForFrame.lookupFrameElements(frame));
			final List<SpanAndCorrespondingFeatures[]> featuresAndSpanByArgument = Lists.newArrayList();
			for (String frameElement : frameElements) {
				final List<SpanAndCorrespondingFeatures> spansAndFeatures = Lists.newArrayList();
				for (SpanAndParseIdx candidateSpanAndParseIdx : spans) {
					final Range0Based span = candidateSpanAndParseIdx.span;
					final DependencyParse parse = parses.get(candidateSpanAndParseIdx.parseIdx);
					final Set<String> featureSet =
							featureExtractor.extractFeatures(dataPoint, frame, frameElement, span, parse).elementSet();
					final int[] featArray = convertToIdxs(featureSet);
					spansAndFeatures.add(new SpanAndCorrespondingFeatures(new int[] {span.start, span.end}, featArray));
				}
				featuresAndSpanByArgument.add(spansAndFeatures.toArray(new SpanAndCorrespondingFeatures[spansAndFeatures.size()]));
			}
			frameFeaturesList.add(new FrameFeatures(frame,
					targetStartTokenIdx,
					targetEndTokenIdx,
					frameElements,
					featuresAndSpanByArgument));
		}
		return decoder.decodeAll(frameFeaturesList, idResult, 0, kBest);
	}

	private int[] convertToIdxs(Iterable<String> featureSet) {
		// convert feature names to feature indexes
		final List<Integer> featureList = Lists.newArrayList();
		for (String feature : featureSet) {
			final Integer idx = argIdFeatureIndex.get(feature);
			if (idx != null) {
				featureList.add(idx);
			}
		}
		return Ints.toArray(featureList);
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
		return lemmatizer.addLemmas(sentence);
	}
}
