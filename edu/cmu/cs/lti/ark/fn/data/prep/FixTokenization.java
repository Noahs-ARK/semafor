/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * FixTokenization.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.data.prep;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import gnu.trove.THashMap;


public class FixTokenization
{
	
	public static void main(String[] args)
	{
		String orgFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.sentences";
		String fixedFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.sentences.fixed";
		String convFile = "/usr0/dipanjan/software/varcon/abbc.tab";
//		THashMap<String,String> convMap = readConversions(convFile);
//		fixTokenization(orgFile,fixedFile,convMap);
		String tokenizedFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.sentences.tokenized";
//		ArrayList<String> sentences = readSentencesFromFile(fixedFile);
//		tokenizeSentences(sentences,tokenizedFile);
		sanityCheckTokenization(orgFile, tokenizedFile);
	}
	
	public static void sanityCheckTokenization(String orgFile, String finalFile)
	{
		ArrayList<String> orgSentences = FixTokenization.readSentencesFromFile(orgFile);
		ArrayList<String> tokenizedSentences = FixTokenization.readSentencesFromFile(finalFile);
		
		int size = orgSentences.size();
		for(int i = 0; i < size; i ++)
		{
			StringTokenizer st1 = new StringTokenizer(orgSentences.get(i));
			StringTokenizer st2 = new StringTokenizer(tokenizedSentences.get(i));
			if(st1.countTokens()!=st2.countTokens())
			{
				System.out.println(orgSentences.get(i));
				System.out.println(tokenizedSentences.get(i));
			}
			//System.out.println("Sentence:"+i);
		}
	}
	
	public static void tokenizeSentences(ArrayList<String> sentences,String file)
	{	
		int size = sentences.size();
		ArrayList<String> revisedSentences = new ArrayList<String>();
		for(int i = 0; i < size; i ++)
		{
			String sentence = sentences.get(i).trim();
			sentence = replaceSentenceWithPTBWords(sentence);
			revisedSentences.add(sentence);
		}
		FixTokenization.writeSentencesToTempFile(file, revisedSentences);
	}
	
	public static String replaceSentenceWithPTBWords(String sentence)
	{
		sentence = sentence.replace("(","-LRB-");
		sentence = sentence.replace(")","-RRB-");
		sentence = sentence.replace("[","-LSB-");
		sentence = sentence.replace("]","-RSB-");
		sentence = sentence.replace("{","-LCB-");
		sentence = sentence.replace("}","-RCB-");
		return sentence;
	}
	
	public static void runExternalCommand(String command, String printFile)
	{
		String s = null;
		try {
			Process p = Runtime.getRuntime().exec(command);
			PrintStream errStream=System.err;
			System.setErr(System.out);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(printFile));
			// read the output from the command
			System.out.println("Here is the standard output of the command:");
			while ((s = stdInput.readLine()) != null) {
				bWriter.write(s.trim()+"\n");
			}
			bWriter.close();
			// read any errors from the attempted command
			System.out.println("Here is the standard error of the command (if any):");
			while ((s = stdError.readLine()) != null) {
				System.out.println(s);
			}
			p.destroy();
			System.setErr(errStream);
		}
		catch (IOException e) {
			System.out.println("exception happened - here's what I know: ");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	
	
	public static String replaceBritishWords(String line, THashMap<String,String> conv)
	{
		String revisedLine = "";
		StringTokenizer st = new StringTokenizer(line);
		while(st.hasMoreTokens())
		{
			String word = st.nextToken();
			if(conv.get(word)!=null)
			{
				word = conv.get(word);
			}
			else
			{
				if(conv.get(word.toLowerCase())!=null)
				{
					String lookup = conv.get(word.toLowerCase());
					if(word.charAt(0)>='A'&&word.charAt(0)<='Z')
					{
						char ch = lookup.toLowerCase().charAt(0);
						char ch1 = (""+ch).toUpperCase().charAt(0);
						word=ch1+word.substring(1);
					}
				}
			}
			revisedLine+=word+" ";
		}
		return revisedLine.trim();
	}
	
	public static String fixDoubleQuotes(String line, THashMap<String,String> conv)
	{
		String revisedLine = null;
		String pattern = "`(.+?)\"";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(line.trim());
		ArrayList<MatchIndexPair> matches = new ArrayList<MatchIndexPair>();
		boolean flag = false;
		while(m.find())
		{
			MatchIndexPair mip = new MatchIndexPair(m.group(),m.start());
			matches.add(mip);
			flag = true;
		}
		if(!flag)
		{
			line = replaceBritishWords(line,conv);
			return line;
		}
		int size = matches.size();
		MatchIndexPair[] mArray = new MatchIndexPair[size];
		matches.toArray(mArray);
		Arrays.sort(mArray, new Comparator<MatchIndexPair>(){

			public int compare(MatchIndexPair arg0, MatchIndexPair arg1) {
				if(arg0.index<arg1.index)
					return -1;
				else if(arg0.index==arg1.index)
					return 0;
				else
					return 1;
			}
		});
		revisedLine="";
		int prevInd = 0;
		for(int k = 0; k < size; k ++)
		{
			int ind = mArray[k].index;
			String match = mArray[k].match;
			match = match.substring(1,match.length()-1);
			match = "``"+match+"''";
			revisedLine+=line.substring(prevInd,ind);
			revisedLine+=match+" ";
			prevInd = ind+match.length()-1;
		}
		revisedLine+=line.substring(prevInd-1);
		revisedLine.trim();
		revisedLine=replaceBritishWords(revisedLine,conv);
		return revisedLine;
	}
	
	
	public static String fixOpeningQuote(String line, THashMap<String,String> conv)
	{
		line =  line.replaceAll(" ` ", " `` ");
		if(line.startsWith("` "))
		{
			line = "`` "+line.substring(2);
		}
		if(line.endsWith(" `"))
		{
			line = line.substring(0,line.length()-2)+" ``";
		}
		return line;
			
	}
	
	public static String fixClosingQuote(String line, THashMap<String,String> conv)
	{
		line = line.replaceAll(" \" ", " '' ");
		if(line.startsWith("\" "))
		{
			line = "'' "+line.substring(2);
		}
		if(line.endsWith(" \""))
		{
			line = line.substring(0,line.length()-2)+" ''";
		}
		line = line.replaceAll(" ' ", " '' ");
		if(line.startsWith("' "))
		{
			line = "'' "+line.substring(2);
		}
		if(line.endsWith(" '"))
		{
			line = line.substring(0,line.length()-2)+" ''";
		}
		return line;
	}
	
	
	
	
	public static void fixTokenization(String inputFile, String outputFile, THashMap<String, String> conv)
	{
		try
		{
			BufferedReader bReader = new BufferedReader(new FileReader(inputFile));
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(outputFile));
			String line = null;
			while((line=bReader.readLine())!=null)
			{	
				line = line.trim();
				System.out.println(line);
				line = fixDoubleQuotes(line,conv);
				line = fixOpeningQuote(line,conv);
				line = fixClosingQuote(line,conv);
				bWriter.write(line+"\n");
			}			
			bReader.close();
			bWriter.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}		
	}
	
	public static THashMap<String,String> readConversions(String file)
	{
		ArrayList<String> list = ParsePreparation.readSentencesFromFile(file);
		int size = list.size();
		THashMap<String,String> map = new THashMap<String,String>();
		for(int i = 0; i < size; i ++)
		{
			StringTokenizer st = new StringTokenizer(list.get(i),"\t");
			String american = st.nextToken();
			String british = st.nextToken();
			map.put(british, american);
		}
		return map;
	}

	public static ArrayList<String> readSentencesFromFile(String file)
	{
		ArrayList<String> result = new ArrayList<String>();
		try
		{
			BufferedReader bReader = new BufferedReader(new FileReader(file));
			String line = null;
			while((line=bReader.readLine())!=null)
			{
				result.add(line.trim());
			}
			bReader.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}	
	
	public static void writeDepSentencesToTempFile(String file, ArrayList<String> sentences)
	{
		try
		{
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(file));
			int size = sentences.size();
			System.out.println("Size of sentences:"+size);
			for(int i = 0; i < size; i ++)
			{
				bWriter.write(sentences.get(i).trim()+"\n\n");
			}
			bWriter.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}	
	
	public static void writeSentencesToTempFile(String file, ArrayList<String> sentences)
	{
		try
		{
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(file));
			int size = sentences.size();
			System.out.println("Size of sentences:"+size);
			for(int i = 0; i < size; i ++)
			{
				bWriter.write(sentences.get(i).trim()+"\n");
			}
			bWriter.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}	
	
}

class MatchIndexPair
{
	String match;
	int index;
	public MatchIndexPair(String m, int i)
	{
		match = m;
		index = i;
	}
	
}

