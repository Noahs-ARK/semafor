/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * ProduceLargerFrameDistribution.java is part of SEMAFOR 2.0.
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import gnu.trove.THashMap;
import gnu.trove.THashSet;


public class ProduceLargerFrameDistribution {
	public static void main(String[] args) throws IOException {
		String frameMapFile = args[0];
		String trainDistFile = args[1];
		String largeDist = args[2];
		String allFramesFile = args[3];
		
		THashMap<String, THashMap<String, Double>> trainDist = 
			readTrainDistFile(trainDistFile);
		THashMap<String, THashMap<String, Double>> lexDist = 
			new THashMap<String, THashMap<String, Double>>();
		
		THashMap<String,THashSet<String>> frameMap = 
			(THashMap<String,THashSet<String>>)SerializedObjects.readSerializedObject(frameMapFile);
		Set<String> frames = frameMap.keySet();
		for (String f: frames) {
			THashSet<String> hus = frameMap.get(f);
			for (String hu: hus) {
				if (hu.contains(" "))
					continue;
				int li = hu.lastIndexOf("_");
				String word = hu.substring(0, li).toLowerCase();
				String pos = hu.substring(li+1).substring(0,1).toLowerCase();
				String pred = word + "." + pos;
				if (!trainDist.contains(pred)) {
					if (lexDist.contains(pred)) {
						lexDist.get(pred).put(f, 1.0);
					} else {
						THashMap<String, Double> map = new THashMap<String, Double>();
						map.put(f, 1.0);
						lexDist.put(pred, map);
					}
				} else {
					THashMap<String, Double> map = trainDist.get(pred);
					if (!map.contains(f))
						map.put(f, 0.1);
				}
			}
		}
		normalizeMap(lexDist);
		normalizeMap(trainDist);
		Set<String> keys = lexDist.keySet();
		for (String key: keys) {
			trainDist.put(key, lexDist.get(key));
		}
		writeDist(trainDist, largeDist);
	}
	
	public static void writeDist(THashMap<String, THashMap<String, Double>> map, String file) {
		try {
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(file));
			Set<String> keys = map.keySet();
			for (String key: keys) {
				bWriter.write(key + "\t");
				THashMap<String, Double> map1 = map.get(key);
				Set<String> keys1 = map1.keySet();
				int count = 0;
				for (String key1: keys1) {
					if (count != 0) {
						bWriter.write(" ");
					}
					bWriter.write(key1 + " " + map1.get(key1));
					count++;
				}
				bWriter.write("\n");
			}
			bWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public static void normalizeMap(THashMap<String, THashMap<String, Double>> map) {
		Set<String> keys = map.keySet();
		for (String key: keys) {
			THashMap<String, Double> val = map.get(key);
			double total = 0.0;
			Set<String> keys1 = val.keySet();
			for (String key1: keys1) {
				total += val.get(key1);
			}
			for (String key1: keys1) {
				double v = val.get(key1) / total;
				val.put(key1, v);
			}
			map.put(key, val);
		}
	}
	
	public static THashMap<String, THashMap<String, Double>> 
		readTrainDistFile(String trainDistFile) throws IOException {
		THashMap<String, THashMap<String, Double>> result = 
			new THashMap<String, THashMap<String, Double>>();
		List<String> sents = ParsePreparation.readLines(trainDistFile);
		for (String sent: sents) {
			sent = sent.trim();
			String[] toks = sent.split("\t");
			String pred = toks[0];
			String[] toks1 = toks[1].trim().split(" ");
			THashMap<String, Double> map = new THashMap<String, Double>();
 			for (int i = 0; i < toks1.length; i = i + 2) {
				String frame = toks1[i];
				double prob = new Double(toks1[i+1]);
				map.put(frame, prob);
			}
 			result.put(pred, map);
		}
		return result;
	} 
}
