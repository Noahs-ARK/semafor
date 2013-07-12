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
import edu.cmu.cs.lti.ark.fn.data.prep.AllAnnotationsMergingWithoutNE;
import edu.cmu.cs.lti.ark.fn.data.prep.OneLineDataCreation;
import edu.cmu.cs.lti.ark.fn.identification.*;
import edu.cmu.cs.lti.ark.fn.segmentation.MoreRelaxedSegmenter;
import edu.cmu.cs.lti.ark.fn.segmentation.RoteSegmenter;
import edu.cmu.cs.lti.ark.fn.segmentation.SegmentationMode;
import edu.cmu.cs.lti.ark.fn.segmentation.Segmenter;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectDoubleHashMap;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.cmu.cs.lti.ark.fn.evaluation.ParseUtils.GOLD_TARGET_SUFFIX;
import static edu.cmu.cs.lti.ark.fn.evaluation.ParseUtils.getRightInputForFrameIdentification;
import static edu.cmu.cs.lti.ark.fn.identification.FrameIdentificationRelease.getTokenRepresentation;
import static edu.cmu.cs.lti.ark.fn.segmentation.SegmentationMode.*;
import static edu.cmu.cs.lti.ark.util.IntRanges.range;
import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;
import static edu.cmu.cs.lti.ark.util.SerializedObjects.readObject;
import static org.apache.commons.io.IOUtils.closeQuietly;

public class ParserDriver {

	public static final String SERVER_FLAG = "server";
	public static final int BATCH_SIZE = 50;
	private static final Joiner TAB = Joiner.on("\t");
	/*
	 *  required flags:
	 *  mstmode
	 * 	mstserver
	 * 	mstport
	 *  posfile
	 *  test-parsefile
	 *  fnidreqdatafile
	 *  goldsegfile
	 *  userelaxed
	 *  testtokenizedfile
	 *  idmodelfile
	 *  alphabetfile
	 *  framenet-femapfile
	 *  eventsfile
	 *  spansfile
	 *  model
	 *  useGraph
	 *  frameelementsoutputfile
	 *  alllemmatagsfile
	 *  decodingtype
	 */
	public static void main(String[] args) throws Exception {
		// parse options
		final FNModelOptions options = new FNModelOptions(args);

		final String mstServerMode = options.mstServerMode.get();
		// Initializing connection to the MST server, if it exists
		String mstServer = null;
		int mstPort = -1;
		if (mstServerMode.equals(SERVER_FLAG)) {
			mstServer = options.mstServerName.get();
			mstPort = options.mstServerPort.get();
		}
		/* Initializing WordNet */
		final WordNetRelations wnr = new WordNetRelations();
		/* Opening POS tagged file */
		final String posFile = options.posTaggedFile.get();
		final String tokenizedFile = options.testTokenizedFile.get();

		BufferedReader posReader = null;
		BufferedReader tokenizedReader = null;
		try {
			posReader = new BufferedReader(new FileReader(posFile));
			tokenizedReader = new BufferedReader(new FileReader(tokenizedFile));
			runParser(posReader, tokenizedReader, wnr, options, mstServer, mstPort);
		} finally {
			closeQuietly(posReader);
			closeQuietly(tokenizedReader);
		}
	}

	private static void runParser(BufferedReader posReader,
								  BufferedReader tokenizedReader,
								  WordNetRelations wnr,
								  FNModelOptions options,
								  String serverName,
								  int serverPort) throws Exception {
		// extract options
		final String graphFilename = options.useGraph.get();
		final boolean useGraph = !graphFilename.equals("null");
		final String requiredDataFilename = options.fnIdReqDataFile.get();
		final String idParamsFile = options.idParamFile.get();
		final String requiresMapFile = options.requiresMapFile.get();
		final String excludesMapFile = options.excludesMapFile.get();
		final String alphabetFilename = options.alphabetFile.get();
		final String fedictFilename = options.frameNetElementsMapFile.get();
		final String eventsFilename = options.eventsFile.get();
		final String spansFilename = options.spansFile.get();
		final String decodingType = options.decodingType.get();
		final String modelFilename = options.modelFile.get();
		final String goldSegFile = options.goldSegFile.get();
		final String useRelaxedSegmentation = options.useRelaxedSegmentation.get();
		final String outputFile = options.frameElementsOutputFile.get();
		final String allLemmaTagsOutputFile = options.outAllLemmaTagsFile.get();
		if(options.kBestOutput.absent())
			options.kBestOutput.set("1");
		final int kBestOutput = options.kBestOutput.get();

		// unpack required data
		final RequiredDataForFrameIdentification r = readObject(requiredDataFilename);
		final Set<String> allRelatedWords = r.getAllRelatedWords();
		final Map<String, Set<String>> relatedWordsForWord = r.getRelatedWordsForWord();
		final Map<String, THashMap<String, Set<String>>> wordNetMap = r.getWordNetMap();
		final THashMap<String,THashSet<String>> frameMap = r.getFrameMap();
		final THashMap<String,THashSet<String>> cMap = r.getcMap();
		wnr.setRelatedWordsForWord(relatedWordsForWord);
		wnr.setWordNetMap(wordNetMap);
		final Pair<IdFeatureExtractor,TObjectDoubleHashMap<String>> extractorAndParams =
				FrameIdentificationRelease.parseParamFile(idParamsFile);
		final IdFeatureExtractor featureExtractor = extractorAndParams.getFirst();
		final TObjectDoubleHashMap<String> paramList =
				extractorAndParams.getSecond();

		System.err.println("Initializing frame identification model...");
		FastFrameIdentifier idModel;
		if (useGraph) {
			SmoothedGraph graph = readObject(graphFilename);
			System.err.println("Read graph successfully from: " + graphFilename);
			idModel = new GraphBasedFrameIdentifier(
					featureExtractor,
					frameMap.keySet(),
					cMap,
					paramList,
					graph);
		} else {
			idModel = new FastFrameIdentifier(
					featureExtractor,
					paramList,
					frameMap.keySet(),
					cMap
			);
		}
		// initializing argument identification
		// reading requires and excludes map
		System.err.println("Initializing alphabet for argument identification..");
		CreateAlphabet.setDataFileNames(alphabetFilename, fedictFilename, eventsFilename, spansFilename);
		final Decoding decoding =
				getDecoding(decodingType, modelFilename, alphabetFilename, requiresMapFile, excludesMapFile);

		final SegmentationMode segmentationMode = getSegmentationMode(goldSegFile, useRelaxedSegmentation);
		BufferedReader goldSegReader = null;
		if(segmentationMode.equals(GOLD)) goldSegReader = new BufferedReader(new FileReader(goldSegFile));

		final BufferedWriter outputWriter = new BufferedWriter(new FileWriter(outputFile));
		final BufferedWriter lemmaTagsWriter = new BufferedWriter(new FileWriter(allLemmaTagsOutputFile));

		int count = 0;

		BufferedReader parseReader = null;
		if (serverName == null) {
			parseReader = new BufferedReader(new FileReader(options.testParseFile.get()));
		}
		String posLine = null;
		do {
			final ArrayList<String> posLines = Lists.newArrayList();
			final ArrayList<String> tokenizedLines = Lists.newArrayList();
			List<ArrayList<String>> parseSets = Lists.newArrayList();
			System.err.println("Processing batch of size:" + BATCH_SIZE + " starting from: " + count);
			long time = System.currentTimeMillis();
			for (int ignored : xrange(BATCH_SIZE)) {
				posLine = posReader.readLine();
				if (posLine == null) break;
				posLines.add(posLine);
				tokenizedLines.add(tokenizedReader.readLine());
			}
			if (posLines.size() == 0) break;
			if (serverName == null) {
				for (int ignored : xrange(BATCH_SIZE)) {
					final ArrayList<String> parse = readCoNLLParse(parseReader);
					if (parse.isEmpty()) break;
					parseSets.add(parse);
				}
			} else {
				parseSets = getParsesFromServer(serverName, serverPort, posLines);
			}
			final ArrayList<String> allLemmaTagsSentences =
				getAllLemmaTagsSentenceBatch(tokenizedLines, parseSets, wnr);
			for (String outSent: allLemmaTagsSentences) {
				lemmaTagsWriter.write(outSent + "\n");
			}
			/* actual parsing */
			// get segments
			final List<String> segments = getSegments(allRelatedWords, segmentationMode, goldSegReader, allLemmaTagsSentences);

			// frame identification
			final List<String> idResult = identifyFrames(idModel, allLemmaTagsSentences, segments);

			// argument identification
			final List<String> argResult =
					identifyArguments(wnr, eventsFilename, spansFilename, decoding, count, idResult,
							allLemmaTagsSentences, kBestOutput);
			for (String result: argResult) {
				outputWriter.write(result + "\n");
			}
			System.err.println("Processed " + posLines.size() + " sentences in " + (System.currentTimeMillis()-time) + " millis.");
			count += BATCH_SIZE;
		} while (posLine != null);

		//close streams
		closeQuietly(parseReader);
		closeQuietly(outputWriter);
		closeQuietly(lemmaTagsWriter);
		closeQuietly(goldSegReader);
		decoding.wrapUp();
	}

	public static List<String> identifyFrames(FastFrameIdentifier idModel,
											   List<String> allLemmaTagsSentences,
											   List<String> segments) throws IOException {
		final List<String> inputForFrameId = getRightInputForFrameIdentification(segments);

		final List<String> idResult = Lists.newArrayList();
		for(String input: inputForFrameId) {
			final String[] tokens = input.split("\t");
			// offset of the sentence within the loaded data (relative to options.startIndex)
			final String tokenIdxsStr = tokens[1];
			final int sentNum = Integer.parseInt(tokens[2]);
			final String parseLine = allLemmaTagsSentences.get(sentNum);
			final String frame = idModel.getBestFrame(input, parseLine);
			final Pair<String, String> tokenRepresentation = getTokenRepresentation(tokenIdxsStr, parseLine);
			final String lexicalUnit = tokenRepresentation.getFirst();
			final String tokenStrs = tokenRepresentation.getSecond();
			// rank(=0) \t score(=1.0) \t numTargets+numFes(=1) \t bestFrame \t targetTokenNum(s) \t sentenceOffset
			idResult.add(TAB.join(0, 1.0, 1, frame, lexicalUnit, tokenIdxsStr, tokenStrs, sentNum));
		}
		return idResult;
	}

	public static List<String> getSegments(Set<String> allRelatedWords,
											SegmentationMode segmentationMode,
											BufferedReader goldSegReader,
											List<String> allLemmaTagsSentences) throws IOException {
		final int size = allLemmaTagsSentences.size();
		final List<Integer> sentenceIdxs = range(size).asList();
		List<String> segments;
		if (segmentationMode.equals(GOLD)) {
			final ArrayList<String> segLines = Lists.newArrayList();
			for (int ignored : xrange(size)) {
				final String segLine = goldSegReader.readLine().trim();
				if (segLine == null) break;
				segLines.add(segLine);
			}
			segments = getGoldSegmentationBatch(segLines, sentenceIdxs);
		} else {
			final Segmenter segmenter =
					segmentationMode.equals(STRICT) ? new RoteSegmenter(allRelatedWords) : new MoreRelaxedSegmenter(allRelatedWords);
			segments = segmenter.getSegmentations(allLemmaTagsSentences);
		}
		return segments;
	}

	public static List<String> identifyArguments(WordNetRelations wnr,
												  String eventsFilename,
												  String spansFilename,
												  Decoding decoding,
												  int count,
												  List<String> idResult,
												  List<String> allLemmaTagsSentences,
												  int kBestOutput) throws IOException {
		CreateAlphabet.run(false, allLemmaTagsSentences, idResult, wnr);
		final LocalFeatureReading lfr = new LocalFeatureReading(eventsFilename, spansFilename, idResult);
		final ArrayList<FrameFeatures> frameFeaturesList = lfr.readLocalFeatures();
		decoding.setData(frameFeaturesList, idResult);
		return decoding.decodeAll(count, kBestOutput);
	}

	private static ArrayList<String> getGoldSegmentationBatch(List<String> segLines, List<Integer> sentenceIdxs) {
		final ArrayList<String> segs = Lists.newArrayList();
		for(int j : xrange(segLines.size())) {
			final String segLine = segLines.get(j);
			final Integer sentenceIdx = sentenceIdxs.get(j);
			segs.add(getGoldSegmentation(segLine, sentenceIdx));
		}
		return segs;
	}

	private static String getGoldSegmentation(String segLine, Integer sentenceIdx) {
		List<String> result = Lists.newArrayList();
		for (String tok: segLine.trim().split("\\s")) {
			if (tok.length() > 0) {
				result.add(tok + GOLD_TARGET_SUFFIX);
			}
		}
		return TAB.join(TAB.join(result), sentenceIdx);
	}

	private static SegmentationMode getSegmentationMode(String goldSegFile, String useRelaxedSegmentation) {
		SegmentationMode segmentationMode;
		if (goldSegFile == null || goldSegFile.equals("null") || goldSegFile.equals("")) {
			if (useRelaxedSegmentation.equals("yes")) {
				segmentationMode = RELAXED;
				System.err.println("Using relaxed auto target identification.");
			} else {
				segmentationMode = STRICT;
				System.err.println("Using strict auto target identification.");
			}
		} else {
			segmentationMode = GOLD;
			System.err.println("Using gold targets from: " + goldSegFile);
		}
		return segmentationMode;
	}

	private static Decoding getDecoding(String decodingType,
										String modelFilename,
										String alphabetFilename,
										String requiresMapFile,
										String excludesMapFile) {
		// beam search vs. exact decoding
		final boolean isBeam = decodingType.equals("beam");
		final Decoding decoding = isBeam ? new Decoding() : new JointDecoding(true);
		decoding.init(modelFilename, alphabetFilename);
		if (!isBeam) ((JointDecoding)decoding).setMaps(requiresMapFile, excludesMapFile);
		return decoding;
	}

	private static ArrayList<String> getAllLemmaTagsSentenceBatch(List<String> tokenizedLines,
																  List<ArrayList<String>> parses,
																  WordNetRelations wnr) {
		final ArrayList<String> neSentences =
			AllAnnotationsMergingWithoutNE.findDummyNESentences(tokenizedLines);
		final ArrayList<String> perSentenceParses =
			OneLineDataCreation.getPerSentenceParses(parses, tokenizedLines, neSentences);
		final ArrayList<String> res = new ArrayList<String>();
		for (String line: perSentenceParses){
			res.add(getAllLemmaTagsSentence(line, wnr));
		}
		return res;
	}

	private static String getAllLemmaTagsSentence(String line, WordNetRelations wnr) {
		final String[] toks = line.trim().split("\\s");
		final int sentLen = Integer.parseInt(toks[0]);
		final List<String> lemmas = Lists.newArrayList();
		for(int i = 0; i < sentLen; i++){
			final String word = toks[i + 1].toLowerCase();
			final String pos = toks[sentLen + i + 1];
			lemmas.add(wnr.getLemma(word, pos));
		}
		return TAB.join(line, TAB.join(lemmas));
	}

	public static List<ArrayList<String>> getParsesFromServer(String server,
			int port,
			List<String> posLines) throws IOException {
		Socket kkSocket = null;
		PrintWriter out = null;
		BufferedReader in = null;
		try {
			kkSocket = new Socket(server, port);
			out = new PrintWriter(kkSocket.getOutputStream(),true);
			in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host: " + server);
			System.exit(-1);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to: " + server);
			System.exit(-1);
		}
		for (String posLine: posLines) {
			out.println(posLine);
		}		
		out.println("*");
		ArrayList<ArrayList<String>> ret = Lists.newArrayList();
        String fromServer;
		while ((fromServer = in.readLine()) != null) {
			fromServer = fromServer.trim();
			final String[] toks = fromServer.split("\t");
			ArrayList<String> list = Lists.newArrayList();
			for (int t = 0; t < toks.length; t+=10) {
				String outLine = "";
				for (int s = 0; s < 10; s++) {
					outLine += toks[t+s] + "\t";
				}
				outLine = outLine.trim();
				list.add(outLine);
			}
			ret.add(list);
		}
        out.close();
		in.close();
		kkSocket.close();
        return ret;
	}

	/**
	 * Reads lines until a blank line
	 */
	private static ArrayList<String> readCoNLLParse(BufferedReader bReader) throws IOException {
		ArrayList<String> thisParse = new ArrayList<String>();
		String line;
		while((line = bReader.readLine()) != null) {
			line = line.trim();
			if(line.equals("")) break;
			thisParse.add(line);
		}
		return thisParse;
	}
}
