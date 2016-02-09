/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * NormalizeLinDekNeighbors.java is part of SEMAFOR 2.0.
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

import java.io.IOException;
import java.util.*;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import gnu.trove.THashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectIntHashMap;

public class NormalizeLinDekNeighbors {
	
	public static void main(String[] args) throws IOException {
		String inFile = "/home/dipanjan/work/fall2010/SSL/FNData/lindekneighbors.dat";
		String outFile = "/home/dipanjan/work/fall2010/SSL/FNData/lindekneighbors.normalized.dat";
		
		List<String> lines = ParsePreparation.readLines(inFile);
		System.out.println("Size of units:" + lines.size());
		int size = lines.size();
		ArrayList<TObjectDoubleHashMap<String>> rnList = 
			new ArrayList<TObjectDoubleHashMap<String>>();
		TObjectIntHashMap<String> unitCount = new TObjectIntHashMap<String>();
		for (int i = 0; i < size; i++) {
			String line = lines.get(i).trim();
			String[] toks = line.split("\t");
			String unit = toks[0];
			TObjectIntHashMap<String> map = new TObjectIntHashMap<String>();
			for (int j = 1; j < toks.length; j = j + 2) {
				String nbr = toks[j];
				int count = map.get(nbr);
				map.put(nbr, count + 1);
			}
			TObjectDoubleHashMap<String> valMap = new TObjectDoubleHashMap<String>();
			for (int j = 1; j < toks.length; j = j + 2) {
				String nbr = toks[j];
				int count = map.get(nbr);
				double valInMap = valMap.get(nbr);
				double value = new Double(toks[j+1]);
				valMap.put(nbr, valInMap + value/(double)count);
			}
			int uc = unitCount.get(unit);
			unitCount.put(unit, uc+1);
			rnList.add(valMap);
		}
		THashMap<String, TObjectDoubleHashMap<String>> finalMap = 
			new THashMap<String, TObjectDoubleHashMap<String>>();
		for (int i = 0; i < size; i++) {
			String line = lines.get(i).trim();
			String[] toks = line.split("\t");
			String unit = toks[0];
			TObjectDoubleHashMap<String> vector = rnList.get(i);
			int uc = unitCount.get(unit);
			if (!finalMap.contains(unit)) {
				finalMap.put(unit, add(vector, null, uc));
			} else {
				finalMap.put(unit, add(vector, finalMap.get(unit), uc));
			}
		}
		printFinalMap(finalMap, outFile);		
	}
	
	public static void printFinalMap(THashMap<String, TObjectDoubleHashMap<String>> finalMap, String outFile) {
		Set<String> keySet = finalMap.keySet();
		String[] arr = new String[keySet.size()];
		keySet.toArray(arr);
		Arrays.sort(arr);
		ArrayList<String> finalLines = new ArrayList<String>();
		Comparator<Pair<String, Double>> c = new Comparator<Pair<String, Double>> () {
			public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
				if (o1.second > o2.second)
					return -1;
				else {
					if (o1.second == o2.second) {
						return 0;
					} else
						return 1;
				}
			}			
		};
		for (String unit: arr) {
			String line = unit + "\t";
			TObjectDoubleHashMap<String> map = finalMap.get(unit);
			String[] keys = new String[map.size()];
			map.keys(keys);
			Pair<String, Double>[] pArray = new Pair[map.size()];
			for (int i = 0; i < keys.length; i++) {
				pArray[i]  = new Pair<String, Double>(keys[i], map.get(keys[i]));
			}
			Arrays.sort(pArray, c);
			for (int i = 0; i < keys.length; i++) {
				line += pArray[i].first + "\t" + pArray[i].second + "\t";
			}
			line = line.trim();
			finalLines.add(line);
		}
		ParsePreparation.writeSentencesToFile(outFile, finalLines);
	}
	
	public static TObjectDoubleHashMap<String> add(TObjectDoubleHashMap<String> vector, 
			TObjectDoubleHashMap<String> oldVector, int count) {
		TObjectDoubleHashMap<String> res = new TObjectDoubleHashMap<String>();
		if (oldVector == null) {
			oldVector = new TObjectDoubleHashMap<String>();
		}
		String[] vKeys = new String[vector.size()];
		vector.keys(vKeys);
		for (int i = 0; i < vKeys.length; i++) {
			double value = vector.get(vKeys[i]) / (double)count;
			if (oldVector.contains(vKeys[i])) {
				res.put(vKeys[i], value + oldVector.get(vKeys[i]));
				oldVector.remove(vKeys[i]);
			} else {
				res.put(vKeys[i], value);
			}
		}			
		String[] oldKeys = new String[oldVector.size()];
		oldVector.keys(oldKeys);
		for (int i = 0; i < oldKeys.length; i++) {
			res.put(oldKeys[i], oldVector.get(oldKeys[i]));
		}
		return res;
	}
}
