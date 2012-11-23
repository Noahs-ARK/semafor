/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * ScanAdverbsAndAdjectives.java is part of SEMAFOR 2.0.
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

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import gnu.trove.TObjectIntHashMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

public class ScanAdverbsAndAdjectives {
	public static void main(String[] args) throws Exception{
		String directory = "/usr2/dipanjan/experiments/FramenetParsing/DistributionalSimilarityFeatures/data/pos";
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith("_pos.txt");
			}
		};
		TObjectIntHashMap<String> adjs = new TObjectIntHashMap<String>();
		TObjectIntHashMap<String> advs = new TObjectIntHashMap<String>();
		File fdir = new File(directory);
		String[] files = fdir.list(filter);
		for (int i = 0; i < files.length; i++) {
			ArrayList<String> sents = 
				ParsePreparation.readSentencesFromFile(directory + "/" + files[i]);
			for (String sent: sents) {
				sent = sent.trim();
				String[] tokens = getTokens(sent);
				for (String tok: tokens) {
					int li = tok.lastIndexOf("_");
					String word = tok.substring(0, li);
					String POS = tok.substring(li + 1);
					if (POS.startsWith("J")) {
						word = getCanonicalForm(word);
						int c = adjs.get(word);
						adjs.put(word, c+1);
					} else if (POS.startsWith("RB")) {
						word = getCanonicalForm(word);
						int c = advs.get(word);
						advs.put(word, c+1);
					} else {
						continue;
					}
				}
			}
			System.out.println("Done with:" + files[i]);
		}
		String outDir = "/usr2/dipanjan/experiments/SSL/lindek";
		String adjFile = outDir + "/gw.a";
		String[] keys = new String[adjs.size()];
		adjs.keys(keys);
		Arrays.sort(keys);
		BufferedWriter bWriter = new BufferedWriter (new FileWriter(adjFile));
		for (String key: keys) {
			bWriter.write(key + "\t" + adjs.get(key) + "\n");
		}
		bWriter.close();
		String advFile = outDir + "/gw.adv";
		keys = new String[advs.size()];
		advs.keys(keys);
		Arrays.sort(keys);
		bWriter = new BufferedWriter (new FileWriter(advFile));
		for (String key: keys) {
			bWriter.write(key + "\t" + advs.get(key) + "\n");
		}	
		bWriter.close();
	}	
	
	public static String getCanonicalForm(String word) {
		int len = word.length();
		String ans = "";
		for (int i = 0; i < len; i ++) {
			char c = word.charAt(i);
			if (Character.isDigit(c)) {
				ans += "@";
			} else {
				ans += c;
			}
		}
		return ans.toLowerCase();
	}	
	
	public static String[] getTokens(String sentence) {
		sentence = sentence.trim();
		StringTokenizer st = new StringTokenizer(sentence, " \t", true);
		ArrayList<String> toksList = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			String tok = st.nextToken().trim();
			if (tok.equals("")) {
				continue;
			}
			toksList.add(tok);
		}
		String[] arr = new String[toksList.size()];
		toksList.toArray(arr);
		return arr;
	}
}
