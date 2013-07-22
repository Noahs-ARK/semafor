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
import edu.cmu.cs.lti.ark.util.nlp.Lemmatizer;
import edu.cmu.cs.lti.ark.util.nlp.MorphaLemmatizer;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Scanner;

import static org.apache.commons.io.IOUtils.closeQuietly;

public class LemmatizeStuff {
	public static String infilename = "semeval.fulldev.sentences.all.tags";
	public static String outfilename = "semeval.fulldev.sentences.lemma.tags";
	private static Lemmatizer lemmatizer = new MorphaLemmatizer();
	public static final String IN_FILE = "in";
	public static final String OUT_FILE = "out";

	public static void main(String[] args) throws URISyntaxException, FileNotFoundException {
		CustomOptions options = new CustomOptions(args);
		if(options.isPresent(IN_FILE)) {
			infilename = options.get(IN_FILE);
		}
		if(options.isPresent(OUT_FILE)) {
			outfilename = options.get(OUT_FILE);
		}
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
	public static void lemmatize(String infile, String outfile) throws FileNotFoundException {
		infilename = infile;
		outfilename = outfile;
		run();
	}

	private static void run() throws FileNotFoundException {
		Scanner sc = new Scanner(new FileInputStream(infilename));
		PrintStream ps = new PrintStream(new FileOutputStream (outfilename));
		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			ps.print(line + "\t");
			String[] toks = line.trim().split("\\s");
			int sentLen = Integer.parseInt(toks[0]);
			for(int i = 0; i < sentLen; i++) {
				String lemma = lemmatizer.getLemma(toks[i + 1].toLowerCase(), toks[i + 1 + sentLen]);
				ps.print(lemma + "\t");
			}
			ps.println();
		}
		sc.close();
		closeQuietly(ps);
	}
}
