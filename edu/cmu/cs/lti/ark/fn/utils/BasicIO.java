/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * BasicIO.java is part of SEMAFOR 2.0.
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.aliasi.sentences.IndoEuropeanSentenceModel;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;



public class BasicIO
{
	static final TokenizerFactory TOKENIZER_FACTORY = new IndoEuropeanTokenizerFactory();
    static final SentenceModel SENTENCE_MODEL  = new IndoEuropeanSentenceModel();
	
    public static void main(String[] args) {
    	WordNetRelations wnr = new WordNetRelations("lrdata/stopwords.txt", "file_properties.xml");
    	String haveLemma = wnr.getLemmaForWord("be", "VB");
    	System.out.println(haveLemma);
    }
    
	public static String replaceSentenceWithSRLWords(String sentence)
	{
		sentence = sentence.replace("_-LRB-", "_(");
		sentence = sentence.replace("_-RRB-", "_)");
		sentence = sentence.replace("-LRB-_", "(_");
		sentence = sentence.replace("-RRB-_", ")_");
		sentence = sentence.replace("-LSB-_", "[_");
		sentence = sentence.replace("-RSB-_", "]_");
		sentence = sentence.replace("-LCB-_", "{_");
		sentence = sentence.replace("-RCB-_", "}_");
		return sentence;
	}
	
	
	static String replaceSentenceWithPTBWords(String sentence)
	{
		sentence = sentence.replace("-LRB-_", "(_");
		sentence = sentence.replace("-RRB-_", ")_");
		sentence = sentence.replace("-LSB-_", "[_");
		sentence = sentence.replace("-RSB-_", "]_");
		sentence = sentence.replace("-LCB-_", "{_");
		sentence = sentence.replace("-RCB-_", "}_");
		return sentence;
	}
	
	
	public static void printCoNLLTypeInput(String posFile, String conllInputFile)
	{
		ArrayList<String> posSentences = readSentencesFromFile(posFile);
		try
		{
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(conllInputFile));
			for(String posSentence:posSentences)
			{
				posSentence=replaceSentenceWithPTBWords(posSentence);
				ArrayList<String> words = new ArrayList<String>();
				ArrayList<String> pos = new ArrayList<String>();
				ArrayList<String> parents = new ArrayList<String>();
				ArrayList<String> labels = new ArrayList<String>();
				StringTokenizer st = new StringTokenizer(posSentence.trim());
				while(st.hasMoreTokens())
				{
					String token = st.nextToken();
					int lastIndex = token.lastIndexOf('_');
					String word = token.substring(0,lastIndex);
					String POS = token.substring(lastIndex+1);
					words.add(word);
					pos.add(POS);
					parents.add("0");
					labels.add("SUB");
				}
				writeStuff(bWriter,words,pos,parents,labels);
				System.out.println("Processed sentence:"+posSentence);
			}
			bWriter.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void writeStuff(BufferedWriter bWriter, ArrayList<String> words, ArrayList<String> pos, ArrayList<String> parent, ArrayList<String> label)
	{
		int size = words.size();
		for(int i = 0; i < size; i ++)
		{
			String line = "";
			line+=(i+1)+"\t";
			line+=words.get(i).toLowerCase()+"\t";
			line+=words.get(i).toLowerCase()+"\t";
			line+=pos.get(i)+"\t";
			line+=pos.get(i)+"\t";
			line+="_\t";
			line+=parent.get(i)+"\t";
			line+=label.get(i);
			try {
				bWriter.write(line+"\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			bWriter.write("\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
	
	public static void posTagSentences(String tokenizedFile, String posTaggedFile, String projectRoot)
	{
		runExternalCommand("scripts/runPosTagger.sh "+projectRoot+" "+tokenizedFile+" "+posTaggedFile);
	}
	
	public static void tokenizeSentences(ArrayList<String> sentences,String file)
	{	
		String tempFile="temp.txt";
		System.out.println(sentences.size());
		writeSentencesToTempFile(tempFile, sentences);
		runExternalCommand("./scripts/tokenizer.sed temp.txt", "tokenized.txt");
		ArrayList<String> tokenizedSentences=readSentencesFromFile("tokenized.txt");
		if(sentences.size()!=tokenizedSentences.size())
		{
			System.err.println("Problem in tokenized file. Exiting.");
			System.exit(0);
		}
		writeSentencesToTempFile(file,tokenizedSentences);
	}
	
	
	public static void tokenizeSentencesOPENNLP(ArrayList<String> sentences)
	{
		ArrayList<String> tokenizedSentences = new ArrayList<String>();
		int size = sentences.size();
		try {
			opennlp.tools.lang.english.Tokenizer tokenizer = new opennlp.tools.lang.english.Tokenizer("data/EnglishTok.bin.gz");
			for(int i = 0; i < size; i ++)
			{
				String[] tokens = tokenizer.tokenize(sentences.get(i).trim());
				String tokenized = "";
				for(int j = 0; j < tokens.length; j ++)
				{
					tokenized+=tokens[j]+" ";
				}
				tokenized=tokenized.trim();
				tokenizedSentences.add(tokenized);
				System.out.println(tokenized);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writeSentencesToTempFile("data/allSentencesTokenizedOPENNLP.txt",tokenizedSentences);
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
	

	public static void runExternalCommand(String command)
	{
		String s = null;
		try {
			Process p = Runtime.getRuntime().exec(command);
			PrintStream errStream=System.err;
			System.setErr(System.out);
			//BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			// read the output from the command
//			System.out.println("Here is the standard output of the command:");
//			while ((s = stdInput.readLine()) != null) {
//				System.out.println(s.trim());
//			}
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
	
	
	public static ArrayList<String> readSentencesFromFile(String file)
	{
		ArrayList<String> result = new ArrayList<String>();
		try {
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
	

	public static ArrayList<String> splitSentences(String paragraph)
	{
		ArrayList<String> tokenList = new ArrayList<String>();
		ArrayList<String> whiteList = new ArrayList<String>();
		paragraph=paragraph.trim();
		paragraph=paragraph.replace("\r\n", " ");
		paragraph=paragraph.replace("\n", " ");
		paragraph=paragraph.replace("\r", " ");
		
		
		Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(paragraph.toCharArray(),0, paragraph.length());
		tokenizer.tokenize(tokenList, whiteList);
		String[] tokens = new String[tokenList.size()];
		String[] whites = new String[whiteList.size()];
		tokenList.toArray(tokens);
		whiteList.toArray(whites);
		int[] sentenceBoundaries = SENTENCE_MODEL.boundaryIndices(tokens,whites);
		
		ArrayList<String> sentences = new ArrayList<String>();
		
		if (sentenceBoundaries.length < 1) {
		    System.out.println("No sentence boundaries found.");
		    sentences.add(paragraph);
		}
		int sentStartTok = 0;
		int sentEndTok = 0;
		for (int i = 0; i < sentenceBoundaries.length; ++i)
		{
		    sentEndTok = sentenceBoundaries[i];
		    String sentence="";
		    for (int j=sentStartTok; j<=sentEndTok; j++)
		    {
		    	sentence+=tokens[j]+whites[j+1];
		    }
		    sentences.add(sentence.trim());
		    sentStartTok = sentEndTok+1;
		}
		return sentences;
	}	
	
}
