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

import com.google.common.collect.Lists;
import edu.cmu.cs.lti.ark.fn.data.prep.AllAnnotationsMergingWithoutNE;
import edu.cmu.cs.lti.ark.fn.data.prep.OneLineDataCreation;
import edu.cmu.cs.lti.ark.fn.evaluation.ParseUtils;
import edu.cmu.cs.lti.ark.fn.identification.FastFrameIdentifier;
import edu.cmu.cs.lti.ark.fn.identification.FrameIdentificationRelease;
import edu.cmu.cs.lti.ark.fn.identification.RequiredDataForFrameIdentification;
import edu.cmu.cs.lti.ark.fn.identification.SmoothedGraph;
import edu.cmu.cs.lti.ark.fn.segmentation.MoreRelaxedSegmenter;
import edu.cmu.cs.lti.ark.fn.segmentation.RoteSegmenter;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectDoubleHashMap;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static edu.cmu.cs.lti.ark.fn.evaluation.ParseUtils.getRightInputForFrameIdentification;
import static edu.cmu.cs.lti.ark.util.SerializedObjects.readSerializedObject;
import static org.apache.commons.io.IOUtils.closeQuietly;

public class ParserDriver {

	public static final String SERVER_FLAG = "server";
	public static final int BATCH_SIZE = 50;
	/*
	 *  required flags:
	 *  mstmode
	 * 	mstserver
	 * 	mstport
	 *  posfile
	 *  test-parsefile
	 *  stopwords-file
	 *  wordnet-configfile
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

		/* Initializing connection to the MST server, if it exists */
		String mstServer = null;
		int mstPort = -1;
		if (mstServerMode.equals(SERVER_FLAG)) {
			mstServer = options.mstServerName.get();
			mstPort = options.mstServerPort.get();
		}
		/* Initializing WordNet config file */
		final String stopWordsFile = options.stopWordsFile.get();
		final String wnConfigFile = options.wnConfigFile.get();
		final WordNetRelations wnr = new WordNetRelations(stopWordsFile, wnConfigFile);
		/* Opening POS tagged file */
		final String posFile = options.posTaggedFile.get();
		final String tokenizedFile = options.testTokenizedFile.get();

		BufferedReader posReader = null;
		BufferedReader tokenizedReader = null;
		try {
			posReader = new BufferedReader(new FileReader(posFile));
			tokenizedReader = new BufferedReader(new FileReader(tokenizedFile));
			runParser(posReader,
					tokenizedReader,
					wnr,
					options,
					mstServer,
					mstPort
			);
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

		// unpack required data
		final RequiredDataForFrameIdentification r =
			(RequiredDataForFrameIdentification) readSerializedObject(requiredDataFilename);
		final THashSet<String> allRelatedWords = r.getAllRelatedWords();
		final Map<String, Set<String>> relatedWordsForWord = r.getRelatedWordsForWord();
		final Map<String, THashMap<String, Set<String>>> wordNetMap = r.getWordNetMap();
		final THashMap<String,THashSet<String>> frameMap = r.getFrameMap();
		final THashMap<String,THashSet<String>> cMap = r.getcMap();
		final Map<String, Map<String, Set<String>>> revisedRelationsMap =
			r.getRevisedRelMap();
		wnr.setRelatedWordsForWord(relatedWordsForWord);
		wnr.setWordNetMap(wordNetMap);
		final Map<String, String> hvLemmas = r.getHvLemmaCache();
		final TObjectDoubleHashMap<String> paramList =
			FrameIdentificationRelease.parseParamFile(idParamsFile);

		System.out.println("Initializing frame identification model...");
		final FastFrameIdentifier idModel = new FastFrameIdentifier(
				paramList, 
				"reg", 
				0.0, 
				frameMap, 
				null, 
				cMap,
				relatedWordsForWord,
				revisedRelationsMap,
				hvLemmas);
		SmoothedGraph sg = null;
		if (useGraph) {
			sg = (SmoothedGraph) readSerializedObject(graphFilename);
			System.out.println("Read graph successfully from: " + graphFilename);
		}
		// initializing argument identification
		// reading requires and excludes map
		System.out.println("Initializing alphabet for argument identification..");
		CreateAlphabet.setDataFileNames(alphabetFilename,
				fedictFilename,
				eventsFilename,
				spansFilename);
		Decoding decoding = getDecoding(decodingType, modelFilename, alphabetFilename, requiresMapFile, excludesMapFile);

		BufferedReader goldSegReader = null;
		// 0 == gold, 1 == strict, 2 == relaxed
		int segmentationMode;
		if (goldSegFile == null || goldSegFile.equals("null") || goldSegFile.equals("")) {
			if (useRelaxedSegmentation.equals("yes")) {
				segmentationMode = 2;
				System.err.println("Using relaxed auto target identification.");
			} else {
				segmentationMode = 1;
				System.err.println("Using strict auto target identification.");
			}
		} else {
			segmentationMode = 0;
			System.err.println("Using gold targets from: " + goldSegFile);
			goldSegReader = new BufferedReader(new FileReader(goldSegFile));
		}

		final BufferedWriter outputWriter = new BufferedWriter(new FileWriter(outputFile));
		final BufferedWriter lemmaTagsWriter = new BufferedWriter(new FileWriter(allLemmaTagsOutputFile));

		int count = 0;
		ArrayList<ArrayList<String>> parseSets = Lists.newArrayList();

		BufferedReader parseReader = null;
		if (serverName == null) {
			parseReader = new BufferedReader(new FileReader(options.testParseFile.get()));
		}
		String posLine = null;
		do {
			int index;
			final ArrayList<String> posLines = Lists.newArrayList();
			final ArrayList<String> tokenizedLines = Lists.newArrayList();
			final ArrayList<String> segLines = Lists.newArrayList();
			final ArrayList<String> tokenNums = Lists.newArrayList();
			ArrayList<String> segs = Lists.newArrayList();
			final ArrayList<String> idResult = Lists.newArrayList();
			int size = parseSets.size();
			for (int i = 0; i < size; i++) {
				final ArrayList<String> set = parseSets.get(0);
				set.clear();
				parseSets.remove(0);
			}
			parseSets.clear();
			System.out.println("Processing batch of size:" + BATCH_SIZE + " starting from: " + count);
			for (index = 0; index < BATCH_SIZE; index++) {
				posLine = posReader.readLine();
				if (posLine == null) {
					break;
				}
				posLines.add(posLine);
				tokenizedLines.add(tokenizedReader.readLine());
				if (goldSegReader != null) {
					segLines.add(goldSegReader.readLine().trim());
				}
				if (serverName == null) {
					parseSets.add(readCoNLLParse(parseReader));
				}
				tokenNums.add("" + index);
			}
			if (posLines.size() == 0) break;
			if (serverName != null) {
				parseSets = getParsesFromServer(serverName,
												serverPort,
												posLines);
			}
			final ArrayList<String> allLemmaTagsSentences =
				getAllLemmaTagsSentences(tokenizedLines, parseSets, wnr);
			for (String outSent: allLemmaTagsSentences) {
				lemmaTagsWriter.write(outSent + "\n");
			}
			/* actual parsing */
			// 1. getting segments
			if (segmentationMode == 0) {
				int j = 0;
				for (String seg: segLines) {
					final String[] toks = seg.trim().split("\\s");
					String outSeg = "";
					for (String tok: toks) {
						outSeg += tok+"#true\t";
					}
					outSeg += tokenNums.get(j);
					segs.add(outSeg.trim());
					j++;
				}
			} else if (segmentationMode == 1) {
				RoteSegmenter seg = new RoteSegmenter();
				segs = seg.findSegmentationForTest(tokenNums, allLemmaTagsSentences, allRelatedWords);
			} else if (segmentationMode == 2) {
				MoreRelaxedSegmenter seg = new MoreRelaxedSegmenter();
				segs = seg.findSegmentationForTest(tokenNums, allLemmaTagsSentences, allRelatedWords);
			}
			final ArrayList<String> inputForFrameId = getRightInputForFrameIdentification(segs);

			// 2. frame identification
			for(String input: inputForFrameId)
			{
				final String[] toks = input.split("\t");
				int sentNum = new Integer(toks[2]);	// offset of the sentence within the loaded data (relative to options.startIndex)
				String bestFrame;
				if (sg == null) {
					bestFrame = idModel.getBestFrame(input,allLemmaTagsSentences.get(sentNum));
				} else {
					bestFrame = idModel.getBestFrame(input,allLemmaTagsSentences.get(sentNum),sg);
				}
				final String tokenRepresentation = FrameIdentificationRelease.getTokenRepresentation(toks[1],allLemmaTagsSentences.get(sentNum));
				final String[] split = tokenRepresentation.trim().split("\t");
				idResult.add(1+"\t"+bestFrame+"\t"+split[0]+"\t"+toks[1]+"\t"+split[1]+"\t"+sentNum);	// BestFrame\tTargetTokenNum(s)\tSentenceOffset
			}
			// 3. argument identification
			CreateAlphabet.run(false, allLemmaTagsSentences, idResult, wnr);
			final LocalFeatureReading lfr =
					new LocalFeatureReading(eventsFilename,
							spansFilename,
											idResult);
			lfr.readLocalFeatures();
			decoding.setData(null, lfr.getMFrameFeaturesList(), idResult);
			final ArrayList<String> argResult = decoding.decodeAll("overlapcheck", count);
			for (String result: argResult) {
				outputWriter.write(result + "\n");
			}
			count += index;
		} while (posLine != null);
		//close streams
		if (parseReader != null) {
			parseReader.close();
		}
		if (outputWriter != null) {
			outputWriter.close();
		}
		if (lemmaTagsWriter != null) {
			lemmaTagsWriter.close();
		}
		// wrapping up joint decoding
		if (!decodingType.equals("beam")) {
			((JointDecoding)decoding).wrapUp();
		}
	}

	private static Decoding getDecoding(String decodingType,
										String modelFilename,
										String alphabetFilename,
										String requiresMapFile,
										String excludesMapFile) {
		Decoding decoding;
		if (decodingType.equals("beam")) {
			decoding = new Decoding();
			decoding.init(modelFilename,
					alphabetFilename);
		} else {
			decoding = new JointDecoding(true); // exact decoding
			decoding.init(modelFilename,
					alphabetFilename);
			((JointDecoding)decoding).setMaps(requiresMapFile, excludesMapFile);
		}
		return decoding;
	}

	private static ArrayList<String> getAllLemmaTagsSentences(
			ArrayList<String> tokenizedLines, 
			ArrayList<ArrayList<String>> parses,
			WordNetRelations wnr) {
		ArrayList<String> neSentences = 
			AllAnnotationsMergingWithoutNE.findDummyNESentences(tokenizedLines);
		ArrayList<String> perSentenceParses = 
			OneLineDataCreation.getPerSentenceParses(parses, tokenizedLines, neSentences);
		ArrayList<String> res = new ArrayList<String>();
		for (String line: perSentenceParses){
			String outLine = line+"\t";
			String[] toks=line.trim().split("\\s");
			int sentLen=Integer.parseInt(toks[0]);
			for(int i=0;i<sentLen;i++){
				String lemma=wnr.getLemmaForWord(toks[i+1].toLowerCase(), toks[i+1+sentLen]);
				outLine += lemma+"\t";
			}
			outLine = outLine.trim();
			res.add(outLine);
		}
		return res;
	}

	public static ArrayList<ArrayList<String>> getParsesFromServer(String server,
			int port,
			ArrayList<String> posLines) throws IOException {
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
