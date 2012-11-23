/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * SegmentationScore.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.segmentation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import gnu.trove.TIntObjectHashMap;

public class SegmentationScore {
	public static void main(String[] args) {
		String goldFile = args[0];
		String autoFile = args[1];
		
		ArrayList<String> goldLines = 
			ParsePreparation.readSentencesFromFile(goldFile);
		ArrayList<String> autoLines = 
			ParsePreparation.readSentencesFromFile(autoFile);
		
		TIntObjectHashMap<Set<String>> goldMap = 
			new TIntObjectHashMap<Set<String>>();
		TIntObjectHashMap<Set<String>> autoMap = 
			new TIntObjectHashMap<Set<String>>();
		readMap(goldLines, goldMap);
		readMap(autoLines, autoMap);
		getScore(goldMap, autoMap);	
	}
	
	public static void getScore(TIntObjectHashMap<Set<String>> goldMap, 
			TIntObjectHashMap<Set<String>> autoMap) {
		int[] keys = goldMap.keys();
		double totalGold = 0.0;
		double totalAuto = 0.0;
		double correct = 0.0;
		for (int i = 0; i < keys.length; i ++) {
			Set<String> gIdxs = goldMap.get(keys[i]);
			if (!autoMap.contains(keys[i])) {
				totalGold += gIdxs.size();
			} else {
				Set<String> aIdxs = autoMap.get(keys[i]);
				totalGold += gIdxs.size();
				totalAuto += aIdxs.size();
				for (String a: aIdxs) {
					if (gIdxs.contains(a)) {
						correct++;
					}
				}
			}
		}
		double precision = (correct/totalAuto);
		double recall = (correct/totalGold);
		double f = 2 * (precision * recall) / (precision + recall);
		System.out.println("P="+precision+" R="+recall+" F="+f);
	}
	
	public static void readMap(ArrayList<String> lines,
			TIntObjectHashMap<Set<String>> map) {
		for (String line: lines) {
			line = line.trim();
			String[] toks = line.split("\t");
			int sentNum = new Integer(toks[5]);
			String tokIdx = toks[3];
			if (map.contains(sentNum)) {
				Set<String> set = map.get(sentNum);
				set.add(tokIdx);
			} else {
				Set<String> set = new HashSet<String>();
				set.add(tokIdx);
				map.put(sentNum, set);
			}
		}
	}
	
}
