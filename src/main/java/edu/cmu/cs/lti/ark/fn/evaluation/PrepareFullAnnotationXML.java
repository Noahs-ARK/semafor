/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * PrepareFullAnnotationXML.java is part of SEMAFOR 2.0.
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements;
import edu.cmu.cs.lti.ark.util.XmlUtils;
import edu.cmu.cs.lti.ark.util.ds.Range;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;
import gnu.trove.THashSet;
import gnu.trove.TIntObjectHashMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;
import static edu.cmu.cs.lti.ark.util.XmlUtils.addAttribute;
import static org.apache.commons.io.IOUtils.closeQuietly;


public class PrepareFullAnnotationXML {
	private static final Function<DataPointWithFrameElements,String> GET_FRAME =
			new Function<DataPointWithFrameElements, String>() {
				@Override public String apply(DataPointWithFrameElements input) {
					return input.getFrameName();
				}
			};

	/**
	 * Generates the XML representation of a set of predicted semantic parses so evaluation 
	 * can be performed (with SemEval Perl scripts)
	 * 
	 * @param args Options to specify:
	 *   testFEPredictionsFile
	 *   startIndex
	 *   endIndex 
	 *   testParseFile 
	 *   testTokenizedFile 
	 *   outputFile
	 * @see #generateXMLForPrediction
	 */
	public static void main(String[] args) throws Exception {
		// TODO: Change to CommandLineOptions framework (involves changes to shell scripts which use this class)
		ParseOptions options = new ParseOptions(args);
		generateXMLForPrediction(
				options.testFEPredictionsFile,
				new Range0Based(options.startIndex, options.endIndex, false),
				options.testParseFile,
				options.testTokenizedFile,
				options.outputFile);
	}
	
	/**
	 * Generates the XML representation of a set of predicted semantic parses so evaluation 
	 * can be performed (with SemEval Perl scripts)
	 * 
	 * @param testFEPredictionsFile Path to MapReduce output of the parser, formatted as frame elements lines
	 * @param sentenceNums Range of sentences to include (0-based)
	 * @param testParseFile Dependency parses for each sentence in the data
	 * @param testTokenizedFile File Original form of each sentence in the data
	 * @param outputFile Where to store the resulting XML
	 */
	public static void generateXMLForPrediction(String testFEPredictionsFile, Range sentenceNums, 
			String testParseFile, String testTokenizedFile, String outputFile) throws Exception {
		ArrayList<String> parses = new ArrayList<String>();
		List<String> predictedFELines = new ArrayList<String>();
		List<String> orgSentenceLines = new ArrayList<String>();
		// Read in a subset of (predicted) frame element lines
		BufferedReader inFELines = new BufferedReader(new FileReader(testFEPredictionsFile));
		// output of MapReduce--will have an extra number and tab at the beginning of each line
		String pFELine;
		while((pFELine=inFELines.readLine()) != null) {
			int sentNum = DataPointWithFrameElements.parseFrameNameAndSentenceNum(pFELine).second;
			if (sentenceNums.contains(sentNum)) {
				predictedFELines.add(pFELine.trim());
			}
		}
		closeQuietly(inFELines);


		// Read in corresponding sentences and their parses
		BufferedReader inParses = new BufferedReader(new FileReader(testParseFile));
		BufferedReader inOrgSentences = new BufferedReader(new FileReader(testTokenizedFile));
		String parseLine;
		int count = 0;
		while((parseLine=inParses.readLine())!=null) {
			String sentence = inOrgSentences.readLine().trim();
			// skip sentences prior to the specified range
			if(count < sentenceNums.start) {
				count++;
				continue;
			}

			parses.add(parseLine.trim());
			orgSentenceLines.add(sentence);

			// skip sentences after the specified range
			if(count == sentenceNums.start + sentenceNums.length()) {
				break;
			}
			count++;
		}
		closeQuietly(inParses);
		closeQuietly(inOrgSentences);

		Document doc = createXMLDoc(predictedFELines, sentenceNums, parses, orgSentenceLines);
		XmlUtils.writeXML(outputFile, doc);
	}	
	
	/**
	 * Given several parallel lists of predicted frame instances, including their frame elements, create an XML file 
	 * for the full-text annotation predicted by the model.
	 * @param predictedFELines Lines encoding predicted frames & FEs in the same format as the .sentences.frame.elements files
	 * @param sentenceNums Global sentence number for the first sentence being predicted, so as to map FE lines to items in parses/orgLines
	 * @param parses Lines encoding the parse for each sentence
	 * @param origLines The original sentences, untokenized
	 * @return
	 */
	public static Document createXMLDoc(List<String> predictedFELines,
										Range sentenceNums,
										List<String> parses,
										List<String> origLines) {
		final Document doc = XmlUtils.getNewDocument();
		final Element corpus = doc.createElement("corpus");
		addAttribute(doc, "ID", corpus, "100");
		addAttribute(doc, "name", corpus, "ONE");
		addAttribute(doc, "XMLCreated", corpus, new Date().toString());
		final Element documents = doc.createElement("documents");
		corpus.appendChild(documents);

		final Element document = doc.createElement("document");
		addAttribute(doc, "ID", document, "1");
		addAttribute(doc, "description", document, "TWO");
		documents.appendChild(document);

		final Element paragraphs = doc.createElement("paragraphs");
		document.appendChild(paragraphs);

		final Element paragraph = doc.createElement("paragraph");
		addAttribute(doc, "ID", paragraph, "2");
		addAttribute(doc, "documentOrder", paragraph, "1");
		paragraphs.appendChild(paragraph);

		final Element sentences = doc.createElement("sentences");

		// Map sentence offsets to frame annotation data points within each sentence
		final TIntObjectHashMap<Set<String>> predictions = new TIntObjectHashMap<Set<String>>();
		for (String feLine : predictedFELines) {
			final int sentNum = DataPointWithFrameElements.parseFrameNameAndSentenceNum(feLine).second;

			if (!predictions.containsKey(sentNum))
				predictions.put(sentNum, new THashSet<String>());

			predictions.get(sentNum).add(feLine);
		}

		for (int sent = sentenceNums.start; sent < sentenceNums.start + sentenceNums.length(); sent++) {
			final String parseLine = parses.get(sent- sentenceNums.start);

			final Element sentence = doc.createElement("sentence");
			addAttribute(doc, "ID", sentence, "" + (sent - sentenceNums.start));

			final Element text = doc.createElement("text");
			final String origLine = origLines.get(sent - sentenceNums.start);
			final Node textNode = doc.createTextNode(origLine);
			text.appendChild(textNode);
			sentence.appendChild(text);
			final Element tokensElt = doc.createElement("tokens");
			final String[] tokens = origLine.trim().split(" ");
			for (int i : xrange(tokens.length)) {
				final String token = tokens[i];
				final Element tokenElt = doc.createElement("token");
				addAttribute(doc, "index", tokenElt, "" + i);
				final Node tokenNode = doc.createTextNode(token);
				tokenElt.appendChild(tokenNode);
				tokensElt.appendChild(tokenElt);
			}
			sentence.appendChild(tokensElt);

			final Element annotationSets = doc.createElement("annotationSets");
			
			int frameIndex = 0;	// index of the predicted frame within the sentence
			final Set<String> feLines = predictions.get(sent);

			if (feLines != null) {
				final List<DataPointWithFrameElements> dataPoints = Lists.newArrayList();
				for (String feLine : feLines) {
					final DataPointWithFrameElements dp = new DataPointWithFrameElements(parseLine, feLine);
					dp.processOrgLine(origLine);
					dataPoints.add(dp);
				}
				final Set<String> seenTargetSpans = Sets.newHashSet();
				for (DataPointWithFrameElements dp : dataPoints) {
					final String targetIdxsString = Arrays.toString(dp.getTargetTokenIdxs());
					if (seenTargetSpans.contains(targetIdxsString)) {
						System.err.println("Duplicate target tokens: " + targetIdxsString + ". Skipping. Sentence id: " + sent);
						continue; // same target tokens should never show up twice in the same sentence
					} else {
						seenTargetSpans.add(targetIdxsString);
					}
					assert dp.rank == 1; // this doesn't work with k-best lists anymore
					final String frame = dp.getFrameName();
					// Create the <annotationSet> Element for the predicted frame annotation, and add it under the sentence
					Element annotationSet = buildAnnotationSet(frame, ImmutableList.of(dp), doc, sent - sentenceNums.start, frameIndex);
					annotationSets.appendChild(annotationSet);
					frameIndex++;
				}
			}
			sentence.appendChild(annotationSets);
			sentences.appendChild(sentence);
		}			
		paragraph.appendChild(sentences);
		doc.appendChild(corpus);
		return doc;
	}

	public static Element buildAnnotationSet(String frame,
											 List<DataPointWithFrameElements> dataPointList,
											 Document doc,
											 int parentId,
											 int num) {
		final Element ret = doc.createElement("annotationSet");
		final int setId = parentId * 100 + num;
		addAttribute(doc, "ID", ret, "" + setId);
		addAttribute(doc, "frameName", ret, frame);

		final Element layers = doc.createElement("layers");

		// Target Layer
		final Element targetLayer = doc.createElement("layer");
		final int layerId1 = setId * 100 + 1;
		addAttribute(doc, "ID", targetLayer, "" + layerId1);
		addAttribute(doc, "name", targetLayer, "Target");
		final Element targetLabels = doc.createElement("labels");
		// Target info is inlined in every data point; might as well look at the first one
		final DataPointWithFrameElements firstDataPoint = dataPointList.get(0);
		final List<Range0Based> targetTokenRanges = firstDataPoint.getTokenStartEnds(true);
		final List<Range0Based> targetCharRanges = firstDataPoint.getCharStartEnds(targetTokenRanges);
		int count = 0;
		for(int i : xrange(targetCharRanges.size())) {
			final Range0Based charRange = targetCharRanges.get(i);
			final Range0Based tokenRange = targetTokenRanges.get(i);
			final int labelId1 = layerId1 * 100 + count + 1;
			final Element label1 = doc.createElement("label");
			addAttribute(doc, "ID", label1, "" + labelId1);
			addAttribute(doc, "name", label1, "Target");
			addAttribute(doc, "start", label1, "" + charRange.start);
			addAttribute(doc, "end", label1, "" + charRange.end);
			addAttribute(doc, "tokenStart", label1, "" + tokenRange.start);
			addAttribute(doc, "tokenEnd", label1, "" + tokenRange.end);
			targetLabels.appendChild(label1);
			count = count + 3;
		}
		targetLayer.appendChild(targetLabels);
		layers.appendChild(targetLayer);

		// Frame Element Layers
		for (DataPointWithFrameElements dataPointWithFrameElements : dataPointList) {
			final int rank = dataPointWithFrameElements.rank;
			final double score = dataPointWithFrameElements.score;
			final Element feLayer = doc.createElement("layer");
			final int layerId = setId * 100 + rank + 1;
			addAttribute(doc, "ID", feLayer, "" + layerId);
			addAttribute(doc, "name", feLayer, "FE");
			addAttribute(doc, "kbestRank", feLayer, "" + rank);
			addAttribute(doc, "score", feLayer, "" + score);
			layers.appendChild(feLayer);

			final Element labels = doc.createElement("labels");
			feLayer.appendChild(labels);

			final List<Range0Based> fillerSpans = dataPointWithFrameElements.getOvertFrameElementFillerSpans();
			final List<String> feNames = dataPointWithFrameElements.getOvertFilledFrameElementNames();
			for (int i = 0; i < feNames.size(); i++) {
				final String feName = feNames.get(i);
				final Range fillerSpan = fillerSpans.get(i);

				final int labelId = layerId * 100 + i + 1;
				final Element label = doc.createElement("label");
				addAttribute(doc, "ID", label, "" + labelId);
				addAttribute(doc, "name", label, feName);

				final int tokenStart = fillerSpan.start;
				final int tokenEnd = fillerSpan.end;
				final int startCharIndex = dataPointWithFrameElements.getCharacterIndicesForToken(tokenStart).start;
				final int endCharIndex = dataPointWithFrameElements.getCharacterIndicesForToken(tokenEnd).end;
				addAttribute(doc, "start", label, "" + startCharIndex);
				addAttribute(doc, "end", label, "" + endCharIndex);
				addAttribute(doc, "tokenStart", label, "" + tokenStart);
				addAttribute(doc, "tokenEnd", label, "" + tokenEnd);
				labels.appendChild(label);
			}
		}
		ret.appendChild(layers);
		return ret;
	}
}
