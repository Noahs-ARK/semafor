/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * ConvertGraphToSerObj.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.identification.training;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.fn.identification.SmoothedGraph;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import gnu.trove.TObjectIntHashMap;

public class ConvertGraphToSerObj {
	public static void main(String[] args) {
		variousMs(args);
	}

	public static void variousMs(String[] args) {
		String dir = "/mal2/dipanjan/experiments/FramenetParsing/fndata-1.5/ACLSplits";
		int i = Integer.parseInt(args[0]);
		String graphdir = dir + "/" + i;
		String graphPrefix = "smoothed.graph.a.0.0.k.10.mu.0.1.nu.0.000001";
		for (int m = 1; m < 11; m++) {
			String filepath = graphdir + "/" + graphPrefix;
			SmoothedGraph sg = new SmoothedGraph(filepath, m);
			SerializedObjects.writeSerializedObject(sg, filepath + ".t." + m + ".jobj.gz");
			System.out.println("Done with:" + m);
		}
	}
	
	public static void allGraphs(String[] args) {
		String dir = "/usr2/dipanjan/experiments/FramenetParsing/fndata-1.5/ACLSplits";
		int i = Integer.parseInt(args[0]);
		String graphdir = dir + "/" + i;
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("smoothed.graph") && (!name.endsWith(".jobj"));
			}				
		};
		File f = new File(graphdir);
		String[] files = f.list(filter);
		for (int j = 0; j < files.length; j++) {
			String filepath = graphdir + "/" + files[j];
//			ArrayList<String> sents = ParsePreparation.readSentencesFromFile(filepath);
//			int size = sents.size();
//			String[] predicates = new String[size];
//			int[][] frames = new int[size][];
//			TObjectIntHashMap<String> f2Idx = new TObjectIntHashMap<String>();
//			ArrayList<String> idx2F = new ArrayList<String>();
//			for (int k = 0; k < size; k++) {
//				String sent = sents.get(k);
//				String[] toks = sent.trim().split("\t");
//				predicates[k] = toks[0];
//				String[] fs = toks[1].split(" ");
//				frames[k] = new int[fs.length/2];
//				for (int l = 0; l < fs.length; l = l + 2) {
//					int ind = indexString(fs[l], f2Idx, idx2F);
//					frames[k][l/2] = ind;
//				}
//			}
//			Graph g = new Graph();
//			g.predicates = predicates;
//			g.frames = frames;
//			g.f2Idx = f2Idx;
//			g.idx2F = idx2F;
			int t = Integer.parseInt(args[1]);
			SmoothedGraph sg = new SmoothedGraph(filepath, t);
			SerializedObjects.writeSerializedObject(sg, filepath + ".t." + t + ".jobj.gz");
			System.out.println("Done with:" + j + " " + files[j]);
		}
	}
	
	public static int indexString(String string, TObjectIntHashMap<String> map, ArrayList<String> list) {
		if (map.contains(string)) {
			return map.get(string);
		} else {
			int size = map.size();
			map.put(string, size);
			list.add(string);
			return size;
		}
	}
}
