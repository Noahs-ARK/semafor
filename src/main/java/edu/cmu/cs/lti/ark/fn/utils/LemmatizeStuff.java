/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * LemmatizeStuff.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.utils;
import edu.cmu.cs.lti.ark.fn.parsing.CustomOptions;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.Scanner;

public class LemmatizeStuff {
	public static String infilename = "semeval.fulldev.sentences.all.tags";
	public static String outfilename = "semeval.fulldev.sentences.lemma.tags";
	public static WordNetRelations wnr;
	public static String stopWordsFile = "stopwords.txt";
	public static String wnConfigFile = "file_properties.xml";
	public static final String STOP_WORDS = "stopWords";
	public static final String WN_XML = "wnXML";
	public static final String IN_FILE = "in";
	public static final String OUT_FILE = "out";

	public static void main(String[] args) throws URISyntaxException {
		CustomOptions options = new CustomOptions(args);
		if(options.isPresent(STOP_WORDS)) {
			stopWordsFile = options.get(STOP_WORDS);
		}
		if(options.isPresent(WN_XML)) {
			wnConfigFile = options.get(WN_XML);
		}
		if(options.isPresent(IN_FILE)) {
			infilename = options.get(IN_FILE);
		}
		if(options.isPresent(OUT_FILE)) {
			outfilename = options.get(OUT_FILE);
		}
		wnr = WordNetRelations.getInstance();
		run();
	}

	/**
	 * Reads sentences from infile, in the format
	 * n   word_1    ...   word_n    ...{other_stuff}...
	 * and writes them with their lemmatized versions appended to outfile in the format
	 * n   word_1    ...   word_n    ...{other_stuff}...   lemma_1   ...   lemma_n
	 *
	 * @param stopFile path to a file containing a list of stopwords, one per line
	 * @param wnFile path to the file containing the wordnet map, as created by
	 * 				 {@link edu.cmu.cs.lti.ark.fn.identification.training.RequiredDataCreation}
	 * @param infile path to a file containing the input sentences
	 * @param outfile path to file to which to write
	 */
	public static void lemmatize(String stopFile, String wnFile, String infile, String outfile) {
		stopWordsFile = stopFile;
		wnConfigFile = wnFile;
		infilename = infile;
		outfilename = outfile;
		wnr = new WordNetRelations(stopWordsFile, wnConfigFile);
		run();
	}

	/**
	 * Reads sentences from infile, in the format
	 * n   word_1    ...   word_n    ...{other_stuff}...
	 * and writes them with their lemmatized versions appended to outfile in the format
	 * n   word_1    ...   word_n    ...{other_stuff}...   lemma_1   ...   lemma_n
	 *
	 * @param infile path to a file containing the input sentences
	 * @param outfile path to file to which to write
	 */
	public static void lemmatize(String infile, String outfile) throws URISyntaxException {
		infilename = infile;
		outfilename = outfile;
		wnr = WordNetRelations.getInstance();
		run();
	}

	private static void run() {
		Scanner sc = null;
		PrintStream ps = null;
		try {
			sc = new Scanner(new FileInputStream(infilename));
			ps = new PrintStream(new FileOutputStream (outfilename));
		} catch (IOException ioe){
			System.out.println(ioe.getMessage());
		}
		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			ps.print(line + "\t");
			String[] toks = line.trim().split("\\s");
			int sentLen = Integer.parseInt(toks[0]);
			for(int i = 0; i < sentLen; i++) {
				String lemma = wnr.getLemma(toks[i + 1].toLowerCase(), toks[i + 1 + sentLen]);
				ps.print(lemma + "\t");
			}
			ps.println();
		}
	}
}
