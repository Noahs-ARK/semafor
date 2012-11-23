/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * IdentificationErrors.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.identification;

import java.util.ArrayList;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import gnu.trove.THashMap;
import gnu.trove.THashSet;


public class IdentificationErrors {
	public static void main(String[] args) {
		String goldParseFile = 
			"/mal2/dipanjan/experiments/FramenetParsing/fndata-1.5/CVSplits/0/" +
			"cv.test.sentences.all.lemma.tags";
		String goldFEFile = 
			"/mal2/dipanjan/experiments/FramenetParsing/fndata-1.5/CVSplits/0/" +
			"cv.test.sentences.frame.elements";
		String predictFEFile = 
			"/mal2/dipanjan/experiments/FramenetParsing/SSFrameStructureExtraction/temp_1288980401_0_0.0/" + 
			"file.predict.frame.elements";
		String seenPredsFile = "/mal2/dipanjan/experiments/SSL/GraphBasedSSL/data/" +
			"similar.lemmatized.predicates";
		
		
		ArrayList<String> parses = ParsePreparation.readSentencesFromFile(goldParseFile);
		ArrayList<String> goldFEs = ParsePreparation.readSentencesFromFile(goldFEFile);
		ArrayList<String> predictedFEs = ParsePreparation.readSentencesFromFile(predictFEFile);
		ArrayList<String> seenPreds = ParsePreparation.readSentencesFromFile(seenPredsFile);
		THashSet<String> predSet = new THashSet<String>();
		for (String seenPred: seenPreds) {
			predSet.add(seenPred.split("\t")[0]);
		}
		
		System.out.println("Total number of gold lexical units:" + goldFEs.size());
		int countMU = 0;
		THashMap<String, String> multMap = new THashMap<String, String>();
		for (int i = 0; i < goldFEs.size(); i ++) {
			String[] toks = goldFEs.get(i).trim().split("\t");
			String sentNum = toks[5];
			if (toks[3].contains("_")) {
				countMU++;
			}
			multMap.put(toks[3] + "\t" + sentNum, toks[1]);
		}
		System.out.println("Total number of gold LUs with multiple words:" + countMU);
		int correctMUs = 0;
		int totalCorrect = 0;
		int totalOneWordLUsNotSeen = 0;
		int totalOneWordLUsNotSeenCorrect = 0;
		for (int i = 0; i < predictedFEs.size(); i ++) {
			String[] toks = predictedFEs.get(i).trim().split("\t");
			String sentNum = toks[6];
			String pair = toks[4] + "\t" + sentNum;
			if (toks[4].contains("_")) {
				if (!multMap.contains(pair)) {
					System.out.println("Problem with pair:" + pair);
					System.exit(-1);
				}
				if (multMap.get(pair).equals(toks[2])) {
					correctMUs++;
				}
			} else {
				String parse = parses.get(new Integer(sentNum));
				String[] pToks = parse.split("\t");
				int numTokens = new Integer(pToks[0]);
				int tokNum = new Integer(toks[4]);
				String lemma = pToks[1 + 5*numTokens + tokNum];
				String pos = pToks[1 + numTokens + tokNum].toLowerCase().substring(0, 1);
				String join = lemma + "." + pos;
				if (!predSet.contains(join)) {
					totalOneWordLUsNotSeen++;
					if (multMap.get(pair).equals(toks[2]))
						totalOneWordLUsNotSeenCorrect++;
				}
			}
			if (multMap.get(pair).equals(toks[2])) {
				totalCorrect++;
			}	
		}
		System.out.println("Number of multiple word LUs correct:" + correctMUs);
		System.out.println("Total correct LUs:" + totalCorrect);
		System.out.println("Total one word LUs:" + (goldFEs.size() - countMU));
		System.out.println("Total one word LUs not seen before:" + totalOneWordLUsNotSeen);
		System.out.println("Total one word LUs not seen before that are correct:" + totalOneWordLUsNotSeenCorrect);
	}	
}
