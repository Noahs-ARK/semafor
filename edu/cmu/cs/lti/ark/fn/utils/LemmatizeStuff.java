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
import java.io.*;
import java.util.*;

import edu.cmu.cs.lti.ark.fn.parsing.CustomOptions;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
public class LemmatizeStuff {
	public static String infilename="semeval.fulldev.sentences.all.tags";
	public static String outfilename="semeval.fulldev.sentences.lemma.tags";
	public static WordNetRelations wnr;
	public static String stopWordsFile="lrdata/stopwords.txt";
	public static String wnConfigFile="file_properties.xml";
	public static final String STOP_WORDS="stopWords";
	public static final String WN_XML="wnXML";
	public static final String IN_FILE="in";
	public static final String OUT_FILE="out";
	public static void main(String[] args) {
		CustomOptions co=new CustomOptions(args);
		if(co.isPresent(STOP_WORDS)){
			stopWordsFile=co.get(STOP_WORDS);
		}
		if(co.isPresent(WN_XML)){
			wnConfigFile=co.get(WN_XML);
		}
		if(co.isPresent(IN_FILE)){
			infilename=co.get(IN_FILE);
		}
		if(co.isPresent(OUT_FILE)){
			outfilename=co.get(OUT_FILE);
		}
		wnr = new WordNetRelations(stopWordsFile, wnConfigFile);
		run();
	}
	
	public static void lemmatize(String stopFile, String wnFile, String infile, String outfile)
	{
		stopWordsFile=stopFile;
		wnConfigFile=wnFile;
		infilename=infile;
		outfilename=outfile;
		wnr = new WordNetRelations(stopWordsFile, wnConfigFile);
		run();
	}	
	
	public static void run(){
		Scanner sc=null;
		PrintStream ps=null;
		try{
			sc=new Scanner (new FileInputStream(infilename));
			ps=new PrintStream(new FileOutputStream (outfilename));
		}catch (IOException ioe){System.out.println(ioe.getMessage());}
		while(sc.hasNextLine()){
			String line=sc.nextLine();
			ps.print(line+"\t");
			String[] toks=line.trim().split("\\s");
			int sentLen=Integer.parseInt(toks[0]);
			for(int i=0;i<sentLen;i++){
				String lemma=wnr.getLemmaForWord(toks[i+1].toLowerCase(), toks[i+1+sentLen]);
				ps.print(lemma+"\t");
			}
			ps.println();
		}
	}
}
