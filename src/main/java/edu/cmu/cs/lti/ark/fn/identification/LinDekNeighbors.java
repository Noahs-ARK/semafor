/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * LinDekNeighbors.java is part of SEMAFOR 2.0.
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
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import gnu.trove.THashSet;
import gnu.trove.TObjectIntHashMap;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class LinDekNeighbors {

	public static final int MAX_NEIGHBORS = 20;

	public static void main(String[] args) throws IOException {
		Pair<TObjectIntHashMap<String>, TObjectIntHashMap<String>> p = 
			readAdjectivesAndAdverbs();
		TObjectIntHashMap<String> adjectives = p.first;
		TObjectIntHashMap<String> adverbs = p.second;
		System.out.println("Number of adjectives:" + adjectives.size());
		System.out.println("Number of adverbs:" + adverbs.size());
		try {
			//createNeighborsForAdverbsAndAdjectives(p);
			//createNeighborsForNouns();
			createNeighborsForVerbs();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}		
	
	public static void createNeighborsForVerbs() 
	throws IOException {
		String stopfile = "lrdata/stopwords.txt";
		String wnConfigFile = "file_properties.xml";
		WordNetRelations wnr = new WordNetRelations(stopfile, wnConfigFile);
		if (true)
			System.exit(-1);
		String lindekdirectory = "/home/dipanjan/work/fall2010/SSL/FNData";
		String outFile = "/home/dipanjan/work/fall2010/SSL/FNData/lindekneighbors.dat";
		BufferedWriter bWriter = new BufferedWriter(new FileWriter(outFile, true));
		String line;
		
		BufferedReader bReader = new BufferedReader(new FileReader(lindekdirectory + "/simV.lsp"));
		line = bReader.readLine();
		ArrayList<String> lines = new ArrayList<String>();
		while (line != null) {
			line = line.trim();
			lines.clear();
			while (!line.equals("))")) {
				lines.add(line);
				line = bReader.readLine();
			}
			String firstLine = lines.get(0);
			firstLine = firstLine.substring(1);
			int ind = firstLine.indexOf("(desc");
			firstLine = firstLine.substring(0, ind).trim();
			String pos = null;
			// multiword case
			boolean isAdjective = false;
			boolean isAdverb = false;
			int knn = 0;
			String outline = "";
			for (int i = 1; i < lines.size() ; i ++) {
				StringTokenizer st = new StringTokenizer(lines.get(i).trim(), " \t", true);
				ArrayList<String> toks = new ArrayList<String>();
				while (st.hasMoreTokens()) {
					toks.add(st.nextToken());
				}
				double value = new Double(toks.get(toks.size()-1));
				String unit = "";
				for (int j = 0; j < toks.size()-2; j++) {
					unit += toks.get(j);
				}
				if (unit.startsWith("\"")) {
					if (!unit.endsWith("\"")) {
						System.out.println("Problem with unit:" + unit);
						System.exit(-1);
					}				
					unit = unit.substring(1, unit.length()-1).toLowerCase();
				} else {
					String lc = unit.toLowerCase();
					lc = wnr.getLemma(lc, "V");
					unit = lc;
				}
				outline += unit + ".v\t" + value +"\t";
				knn++;
				if (knn > MAX_NEIGHBORS) {
					break;
				}
			}
			outline = outline.trim();
			if (firstLine.startsWith("\"")) {
				if (!firstLine.endsWith("\"")) {
					System.out.println("Problem with unit:" + firstLine);
					System.exit(-1);
				}				
				firstLine = firstLine.substring(1, firstLine.length()-1).toLowerCase();
			} else {
				String lc = firstLine.toLowerCase();
				firstLine = wnr.getLemma(lc, "V");
			}
			bWriter.write(firstLine + ".v\t" + outline + "\n");
			line = bReader.readLine();
		}
		bReader.close();
		bWriter.close();
	}
	
	
	public static void createNeighborsForNouns() 
	throws IOException {
		String stopfile = "lrdata/stopwords.txt";
		String wnConfigFile = "file_properties.xml";
		WordNetRelations wnr = new WordNetRelations(stopfile, wnConfigFile);

		String lindekdirectory = "/home/dipanjan/work/fall2010/SSL/FNData";
		String outFile = "/home/dipanjan/work/fall2010/SSL/FNData/lindekneighbors.dat";
		BufferedWriter bWriter = new BufferedWriter(new FileWriter(outFile, true));
		String line;
		
		BufferedReader bReader = new BufferedReader(new FileReader(lindekdirectory + "/simN.lsp"));
		line = bReader.readLine();
		ArrayList<String> lines = new ArrayList<String>();
		while (line != null) {
			line = line.trim();
			lines.clear();
			while (!line.equals("))")) {
				lines.add(line);
				line = bReader.readLine();
			}
			String firstLine = lines.get(0);
			firstLine = firstLine.substring(1);
			int ind = firstLine.indexOf("(desc");
			firstLine = firstLine.substring(0, ind).trim();
			String pos = null;
			// multiword case
			boolean isAdjective = false;
			boolean isAdverb = false;
			int knn = 0;
			String outline = "";
			for (int i = 1; i < lines.size() ; i ++) {
				StringTokenizer st = new StringTokenizer(lines.get(i).trim(), " \t", true);
				ArrayList<String> toks = new ArrayList<String>();
				while (st.hasMoreTokens()) {
					toks.add(st.nextToken());
				}
				double value = new Double(toks.get(toks.size()-1));
				String unit = "";
				for (int j = 0; j < toks.size()-2; j++) {
					unit += toks.get(j);
				}
				if (unit.startsWith("\"")) {
					if (!unit.endsWith("\"")) {
						System.out.println("Problem with unit:" + unit);
						System.exit(-1);
					}				
					unit = unit.substring(1, unit.length()-1).toLowerCase();
				} else {
					String lc = unit.toLowerCase();
					lc = wnr.getLemma(lc, "N");
					unit = lc;
				}
				outline += unit + ".n\t" + value +"\t";
				knn++;
				if (knn > MAX_NEIGHBORS) {
					break;
				}
			}
			outline = outline.trim();
			if (firstLine.startsWith("\"")) {
				if (!firstLine.endsWith("\"")) {
					System.out.println("Problem with unit:" + firstLine);
					System.exit(-1);
				}				
				firstLine = firstLine.substring(1, firstLine.length()-1).toLowerCase();
			} else {
				String lc = firstLine.toLowerCase();
				firstLine = wnr.getLemma(lc, "N");
			}
			bWriter.write(firstLine + ".n\t" + outline + "\n");
			line = bReader.readLine();
		}
		bReader.close();
		bWriter.close();
	}

	public static void createNeighborsForAdverbsAndAdjectives(Pair<TObjectIntHashMap<String>, 
			TObjectIntHashMap<String>> p) 
	throws IOException {
		String lindekdirectory = "/home/dipanjan/work/fall2010/SSL/FNData";
		String outFile = "/home/dipanjan/work/fall2010/SSL/FNData/lindekneighbors.dat";
		BufferedWriter bWriter = new BufferedWriter(new FileWriter(outFile));
		String line;
		List<String> mAdjectives = ParsePreparation.readLines(lindekdirectory + "/mult.word.j");
		THashSet<String> adjSet = new THashSet<String>();
		for (int i = 0; i < mAdjectives.size(); i++) {
			String w = mAdjectives.get(i).toLowerCase();
			adjSet.add(w);
		}
		List<String> mAdverbs = ParsePreparation.readLines(lindekdirectory + "/mult.word.r");
		THashSet<String> advSet = new THashSet<String>();
		for (int i = 0; i < mAdverbs.size(); i++) {
			String w = mAdverbs.get(i).toLowerCase();
			advSet.add(w);
		}

		TObjectIntHashMap<String> dAdjectives = p.first;
		TObjectIntHashMap<String> dAdverbs = p.second;
		// adverbs and adjectives;
		BufferedReader bReader = new BufferedReader(new FileReader(lindekdirectory + "/simA.lsp"));
		line = bReader.readLine();
		ArrayList<String> lines = new ArrayList<String>();
		while (line != null) {
			line = line.trim();
			lines.clear();
			while (!line.equals("))")) {
				lines.add(line);
				line = bReader.readLine();
			}
			String firstLine = lines.get(0);
			firstLine = firstLine.substring(1);
			int ind = firstLine.indexOf("(desc");
			firstLine = firstLine.substring(0, ind).trim();
			// multiword case
			boolean isAdjective = false;
			boolean isAdverb = false;
			int knn = 0;
			String outline = "";
			for (int i = 1; i < lines.size() ; i ++) {
				StringTokenizer st = new StringTokenizer(lines.get(i).trim(), " \t", true);
				ArrayList<String> toks = new ArrayList<String>();
				while (st.hasMoreTokens()) {
					toks.add(st.nextToken());
				}
				double value = new Double(toks.get(toks.size()-1));
				String unit = "";
				for (int j = 0; j < toks.size()-2; j++) {
					unit += toks.get(j);
				}
				if (unit.startsWith("\"")) {
					if (!unit.endsWith("\"")) {
						System.out.println("Problem with unit:" + unit);
						System.exit(-1);
					}				
					unit = unit.substring(1, unit.length()-1).toLowerCase();
					isAdjective = isMultiWordAdjective(adjSet, unit);
					isAdverb = isMultiWordAdverb(advSet, unit);
				} else {
					String lc = unit.toLowerCase();
					int wpos = findWordPOS(dAdjectives, 
							dAdverbs,
							lc); 
					if (wpos == 1) {
						isAdjective = true; 
					} else if (wpos == 2) {
						isAdverb = true;
					} else {
						isAdjective = true; 
						isAdverb = true;
					}
					unit = lc;
				}
				if (isAdjective)
					outline += unit + ".a\t" + value +"\t";
				if (isAdverb)
					outline += unit + ".adv\t" + value +"\t";
				knn++;
				if (knn > MAX_NEIGHBORS) {
					break;
				}
			}
			outline = outline.trim();
			if (firstLine.startsWith("\"")) {
				if (!firstLine.endsWith("\"")) {
					System.out.println("Problem with unit:" + firstLine);
					System.exit(-1);
				}				
				firstLine = firstLine.substring(1, firstLine.length()-1).toLowerCase();
				isAdjective = isMultiWordAdjective(adjSet, firstLine);
				isAdverb = isMultiWordAdverb(advSet, firstLine);
			} else {
				String lc = firstLine.toLowerCase();
				int wpos = findWordPOS(dAdjectives, 
						dAdverbs,
						lc); 
				if (wpos == 1) {
					isAdjective = true; 
				} else if (wpos == 2) {
					isAdverb = true;
				} else {
					isAdjective = true; 
					isAdverb = true;
				}
				firstLine = lc;
			}
			if (isAdjective) {
				bWriter.write(firstLine + ".a\t" + outline + "\n");
			}
			if (isAdverb) {
				bWriter.write(firstLine + ".adv\t" + outline + "\n");
			}
			line = bReader.readLine();
		}
		bReader.close();
		bWriter.close();
	}

	public static boolean isMultiWordAdjective(THashSet<String> mAdjectives, 
			String word) {
		return mAdjectives.contains(word);
	}

	public static boolean isMultiWordAdverb(THashSet<String> mAdverbs, 
			String word) {
		return mAdverbs.contains(word);	
	}

	// 1 : adjective
	// 2: adverb
	// 3: both
	public static int findWordPOS(TObjectIntHashMap<String> dAdjectives, 
			TObjectIntHashMap<String> dAdverbs,
			String word) {
		int adjCount = dAdjectives.get(word);
		int advCount = dAdverbs.get(word);

		if (adjCount == 0 && advCount == 0) {
			if (word.endsWith("ly")) 
				return 2;
			else {
				return 3;
			}				
		}
		int total = adjCount + advCount;
		double adjProb = (double)adjCount / (double) total;
		double advProb = (double)advCount / (double) total;
		if (Math.abs(adjProb - advProb) < 0.2) {
			return 3;
		} else {
			if (adjProb > advProb) {
				return 1;
			} else {
				return 2;
			}
		}
	}


	public static Pair<TObjectIntHashMap<String>, TObjectIntHashMap<String>> 
	readAdjectivesAndAdverbs() throws IOException {
		String adjFile = "/home/dipanjan/work/fall2010/SSL/FNData/gw.a";
		String advFile = "/home/dipanjan/work/fall2010/SSL/FNData/gw.adv";
		List<String> adjectives =
			ParsePreparation.readLines(adjFile);
		List<String> adverbs =
			ParsePreparation.readLines(advFile);
		TObjectIntHashMap<String> adjMap = 
			new TObjectIntHashMap<String>();
		TObjectIntHashMap<String> advMap = 
			new TObjectIntHashMap<String>();
		for (String string: adjectives) {
			String[] toks = string.trim().split("\t");
			adjMap.put(toks[0], Integer.parseInt(toks[1]));
		}
		for (String string: adverbs) {
			String[] toks = string.trim().split("\t");
			try {
				advMap.put(toks[0], Integer.parseInt(toks[1]));
			} catch (Exception e) {
				System.out.println(string + "\n\n");
				e.printStackTrace();
				System.exit(-1);
			}
		}
		return new Pair<TObjectIntHashMap<String>, TObjectIntHashMap<String>>(adjMap, advMap);
	}


	public static Pair<TObjectIntHashMap<String>, TObjectIntHashMap<String>> 
	scanAdjectivesAndAdverbs() {
		String largeFile = 
			"/home/dipanjan/work/fall2010/SSL/FNData/AP_1m.all.lemma.tags";
		TObjectIntHashMap<String> adjectives = new TObjectIntHashMap<String>();
		TObjectIntHashMap<String> adverbs = new TObjectIntHashMap<String>();
		try {
			BufferedReader bReader = new BufferedReader(new FileReader(largeFile));
			String line = null;
			int count = 0;
			while ((line = bReader.readLine()) != null) {
				line = line.trim();
				String[] toks = line.split("\t");
				int numTokens = Integer.parseInt(toks[0]);
				for (int i = 0; i < numTokens; i++) {
					String pos = toks[1 + numTokens + i];
					if (pos.startsWith("J")) {
						int c = adjectives.get(toks[1 + i].toLowerCase());
						adjectives.put(toks[1 + i].toLowerCase(), c+1);
					} else if (pos.startsWith("RB")) {
						int c = adverbs.get(toks[1 + i].toLowerCase());
						adverbs.put(toks[1 + i].toLowerCase(), c+1);
					}
				}
				count++;
				if (count % 1000 == 0) {
					System.out.print(". ");
				}
				if (count % 10000 == 0) {
					System.out.println(count);
				}
				//				if (count > 1000) 
				//					break;
			}
			bReader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}		
		return new Pair<TObjectIntHashMap<String>, TObjectIntHashMap<String>>(adjectives, adverbs);
	}
}
