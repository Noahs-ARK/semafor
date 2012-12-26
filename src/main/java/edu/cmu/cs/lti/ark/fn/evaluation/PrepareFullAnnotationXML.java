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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.apache.commons.io.IOUtils.closeQuietly;


public class PrepareFullAnnotationXML {
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
		String feLine;
		while((pFELine=inFELines.readLine())!=null) {
			feLine = pFELine.substring(pFELine.indexOf('\t') + 1);
			int sentNum = DataPointWithFrameElements.parseFrameNameAndSentenceNum(feLine).getSecond();
			if (sentenceNums.contains(sentNum)) {
				predictedFELines.add(feLine.trim());
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
			if(count < sentenceNums.getStart()) {
				count++;
				continue;
			}

			parses.add(parseLine.trim());
			orgSentenceLines.add(sentence);

			// skip sentences after the specified range
			if(count == sentenceNums.getStart() + sentenceNums.length()) {
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
	 * @param orgLines The original sentences, untokenized
	 * @return
	 */
	private static Document createXMLDoc(List<String> predictedFELines, Range sentenceNums,
			List<String> parses, List<String> orgLines)
	{
		
		
		// Map sentence offsets to frame annotation data points within each sentence
		TIntObjectHashMap<Set<String>> predictions = new TIntObjectHashMap<Set<String>>();
		for (String feLine : predictedFELines) {
			int sentNum = DataPointWithFrameElements.parseFrameNameAndSentenceNum(feLine).getSecond();
			
			if (!predictions.containsKey(sentNum))
				predictions.put(sentNum, new THashSet<String>());
			
			predictions.get(sentNum).add(feLine);
		}
		
		
		Document doc = XmlUtils.getNewDocument();
		Node corpus = doc.createElement("corpus");
		XmlUtils.addAttribute(doc,"ID", (Element)corpus,""+100);
		XmlUtils.addAttribute(doc,"name", (Element)corpus,"ONE");
		XmlUtils.addAttribute(doc,"XMLCreated", (Element)corpus,""+new Date());
		Node documents = doc.createElement("documents");
		corpus.appendChild(documents);		
		
		Node document = doc.createElement("document");
		XmlUtils.addAttribute(doc,"ID", (Element)document,""+1);
		XmlUtils.addAttribute(doc,"description", (Element)document,"TWO");
		documents.appendChild(document);
		
		Node paragraphs = doc.createElement("paragraphs");
		document.appendChild(paragraphs);		
		
		Node paragraph = doc.createElement("paragraph");
		XmlUtils.addAttribute(doc,"ID", (Element)paragraph,""+2);
		XmlUtils.addAttribute(doc,"documentOrder", (Element)paragraph,"1");
		paragraphs.appendChild(paragraph);	
				
		Node sentences = doc.createElement("sentences");
		
		for (int sent=sentenceNums.getStart(); sent<sentenceNums.getStart()+sentenceNums.length(); sent++)
		{
			String parseLine = parses.get(sent-sentenceNums.getStart());
			
			Node sentence = doc.createElement("sentence");
			XmlUtils.addAttribute(doc,"ID", (Element)sentence,""+(sent-sentenceNums.getStart()));
			
			Node text = doc.createElement("text");
			Node textNode = doc.createTextNode(orgLines.get(sent-sentenceNums.getStart()));
			text.appendChild(textNode);
			sentence.appendChild(text);
			
			Node annotationSets = doc.createElement("annotationSets");
			
			int frameIndex = 0;	// index of the predicted frame within the sentence
			Set<String> feLines = predictions.get(sent);
			
			if (feLines!=null) {
				for (String feLine : feLines)
				{
						DataPointWithFrameElements dp = new DataPointWithFrameElements(parseLine, feLine);
						String orgLine = orgLines.get(sent-sentenceNums.getStart());
						dp.processOrgLine(orgLine);
						
						// Create the <annotationSet> node for the predicted frame annotation, and add it under the sentence
						Node annotationSetNode = dp.buildAnnotationSetNode(doc, sent-sentenceNums.getStart(), frameIndex, orgLine);
						annotationSets.appendChild(annotationSetNode);
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
}
