/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * GenCandidateFromConll.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.parsing;

import java.io.*;
import java.util.*;

import edu.cmu.cs.lti.ark.util.FileUtil;

public class GenCandidateFromConll {
	public static String conllFilename="lrdata/conll.parsed.dat";
	public static String tagFilename="lrdata/lexicon.lemma.tags";
/*
	public static String conllFilename="lrdata/kbest.train";
	public static String tagFilename="lrdata/semeval.fulltrain.sentences.lemma.tags";
*/	
	public static String outTagFilename="lrdata/kbestlexicon.lemma.tags";
	public static final int MAX_K=10;
	public static String CONLL ="conll";
	public static String TAG ="tag";
	public static String OUT ="out";
	public static PrintStream tagps;
	public static Scanner tagsc;
	/**
	 * bug !!!
	 * doesn't work if there are consecutive duplicated short sentences
	 * @param args
	 */
	public static void main(String[] args) {
		CustomOptions co=new CustomOptions(args);
		if(co.isPresent(CONLL)){
			conllFilename=co.get(CONLL);
		}
		if(co.isPresent(TAG)){
			tagFilename=co.get(TAG);
		}
		if(co.isPresent(OUT)){
			outTagFilename=co.get(OUT);
		}
		run();
	}
	public static void run(){
		tagps=FileUtil.openOutFile(outTagFilename);
		tagsc=FileUtil.openInFile(tagFilename);
		Scanner sc = FileUtil.openInFile(conllFilename);
		ArrayList<String[]> lines = readASentence(sc);
		ArrayList<String[]> prev = lines;
		while (sc.hasNextLine()) {
			boolean sameSentence = true;
			ArrayList<int[]>parses=new ArrayList<int[]>();
			ArrayList<String[] >labels=new ArrayList<String[]>();
			int K=0;
			while (sameSentence && sc.hasNextLine() && K < MAX_K ) {
				int parse[]=new int[lines.size()];
				for(int k=0;k<lines.size();k++){
					parse[k]=Integer.parseInt(lines.get(k)[6]);
				}
				parses.add(parse);
				String label[]=new String[lines.size()];
				for(int k=0;k<lines.size();k++){
					label[k]=lines.get(k)[7];
				}
				labels.add(label);
				
				prev = lines;
				lines = readASentence(sc);
				
				// check if it is a new sentence
				sameSentence=isSameSentence(lines,prev);
				K++;
			}
			printTagFile(parses,labels);
		}
	}
	public static ArrayList<String[]> readASentence(Scanner sc){
		ArrayList<String[] >lines=new ArrayList<String[]>();
		String line[]=sc.nextLine().trim().split("\t");
		while(line.length>2){
			lines.add(line);
			line=sc.nextLine().trim().split("\t");
		}
		return lines;
	}
	public static boolean isSameSentence(ArrayList<String []>sa, ArrayList<String[]>sb){
		if (sa == null || sb==null ||  sa.size() != sb.size()){
			return false;
		}
			for (int i = 0; i < sa.size(); i++) {
				if (!sa.get(i)[1].equals(sb.get(i)[1])) {
					return  false;
				}
			}
		return true;
	}
	public static void printTagFile(ArrayList<int[]>parses,ArrayList<String[]>labels){
		String [] toks=tagsc.nextLine().trim().split("\t");
		int sentLen=Integer.parseInt(toks[0]);
		tagps.print(sentLen);
		for(int j=0;j<6;j++){
			if(j==2){
				for(int k=0;k<sentLen;k++){
					tagps.print("\t"+labels.get(0)[k]);
				}
				for(int i=1;i<labels.size();i++){
					tagps.print("\t|");
					for(int k=0;k<sentLen;k++){
						tagps.print("\t"+labels.get(i)[k]);
					}
				}
				continue;
			}
			if(j==3){
				for(int k=0;k<sentLen;k++){
					tagps.print("\t"+parses.get(0)[k]);
				}
				for(int i=1;i<parses.size();i++){
					tagps.print("\t|");
					for(int k=0;k<sentLen;k++){
						tagps.print("\t"+parses.get(i)[k]);
					}
				}
				continue;
			}
			for(int i=0;i<sentLen;i++){				
				tagps.print("\t"+toks[1+j*sentLen+i]);
			}
		}
		tagps.print("\n");
	}
}
