/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * ParsePreparation.java is part of SEMAFOR 2.0.
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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.aliasi.sentences.IndoEuropeanSentenceModel;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

import edu.cmu.cs.lti.ark.util.XmlUtils;
import edu.cmu.cs.lti.ark.fn.wordnet.WordnetInteraction;




public class ParsePreparation
{
	public static final String TRAIN_SET="data/rte_train.xml";
	public static final String DEV_SET="data/rte_dev.xml";
	public static final String TEST_SET="data/rte_test.xml";
	
	static final TokenizerFactory TOKENIZER_FACTORY = new IndoEuropeanTokenizerFactory();
    static final SentenceModel SENTENCE_MODEL  = new IndoEuropeanSentenceModel();
	    
    
	public static void main(String[] args)
	{
		framenetStuff();
	}	

	
	public static void framenetStuff()
	{
		/*
		 * already tokenized
		 */
		//ArrayList<String> allSentences = ParsePreparation.readSentencesFromFile("/usr2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.sentences.tokenized");
		//posTagSentences("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.sentences.tokenized","/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.sentences.pos.tagged");
		//printCoNLLTypeInput("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.sentences.pos.tagged","/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.sentences.conll.input");

		String prefix = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData";
		String input[] = {"semeval.fulltrain.sentences.tokenized", "semeval.fulldev.sentences.tokenized", "semeval.fulltest.sentences.tokenized"};
		String posOutput[] = {"semeval.fulltrain.sentences.pos.tagged", "semeval.fulldev.sentences.pos.tagged", "semeval.fulltest.sentences.pos.tagged"};
		String conllInput[] = {"semeval.fulltrain.sentences.conll.input", "semeval.fulldev.sentences.conll.input", "semeval.fulltest.sentences.conll.input"};
		
		int length = input.length;
		for(int i = 0; i < length; i ++)
		{
			String inputFile = prefix+"/"+input[i];
			String posOutputFile = prefix+"/"+posOutput[i];
			posTagSentences(inputFile, posOutputFile);
			String conllInputFile = prefix+"/"+conllInput[i];
			printCoNLLTypeInput(posOutputFile, conllInputFile);
		}
	}
	
	public static void entailment()
	{
		//ArrayList<String> keyWords = new ArrayList<String>();
		//ArrayList<String> sentences = new ArrayList<String>();
		//getSentences(keyWords, sentences);
		//tokenizeSentences(sentences);
		//tokenizeSentencesOPENNLP(sentences);
		//System.out.println("Total number of keywords:"+keyWords.size());
		//writeSentencesToTempFile("data/keywords.txt",keyWords);
		//posTagSentences();
		//printCoNLLTypeInput();
		//ArrayList<String> labels=getLabels("data/rte_train.xml");
		//labels.addAll(getLabels("data/rte_dev.xml"));
		//labels.addAll(getLabels("data/rte_test.xml"));
		//ParsePreparation.writeSentencesToTempFile("data/allLabels.txt",labels);
		prepareForSRLSystem();
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
	
	
	public static void prepareForSRLSystem()
	{	
		String posFile = "data/allSentencesPOSTagged.txt";
		String srlInputFile="data/allSentencesSRLInput.txt";
		List<String> posSentences = readSentencesFromFile(posFile);
		WordnetInteraction wi = new WordnetInteraction("/mnt/win1/Program Files/WordNet/2.1");
		try
		{
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(srlInputFile));
			for(String posSentence:posSentences)
			{
				posSentence=replaceSentenceWithSRLWords(posSentence);
				ArrayList<String> words = new ArrayList<String>();
				ArrayList<String> lemmas = new ArrayList<String>();
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
					String lemma = wi.returnRoot(word, POS);
					words.add(word);
					lemmas.add(lemma);
					pos.add(POS);
					parents.add("0");
					labels.add("_");
				}
				writeStuffSRL(bWriter,words,pos,parents,labels,lemmas);
				System.out.println("Processed sentence:"+posSentence);
			}
			bWriter.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void writeStuffSRL(BufferedWriter bWriter, ArrayList<String> words, ArrayList<String> pos, ArrayList<String> parent, ArrayList<String> label, ArrayList<String> lemmas)
	{
		int size = words.size();
		for(int i = 0; i < size; i ++)
		{
			String line = "";
			line+=(i+1)+"\t";
			line+=words.get(i)+"\t";
			line+=lemmas.get(i)+"\t";
			line+="_\t";
			line+=pos.get(i)+"\t";

			line+=words.get(i)+"\t";
			line+=lemmas.get(i)+"\t";
			line+=pos.get(i)+"\t";

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
	
	
	
	public static ArrayList<String> getLabels(String xmlFile)
	{
		Document firstDoc = XmlUtils.parseXmlFile(xmlFile, false);
		Element[] eList = XmlUtils.applyXPath(firstDoc, "/entailment-corpus/pair");
		int len = eList.length;
		ArrayList<String> labels = new ArrayList<String>();
		for(int i = 0; i < len; i ++)
		{
			String em = eList[i].getAttribute("entailment");
			if(em.equalsIgnoreCase("yes"))
				labels.add("1");
			else
				labels.add("0");
		}
		return labels;
	}
	
	
	public static String replaceSentenceWithPTBWords(String sentence)
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
		List<String> posSentences = readSentencesFromFile(posFile);
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
	
	
	public static void posTagSentences(String tokenizedFile, String posTaggedFile)
	{
		runExternalCommand("scripts/runPosTagger.sh /usr0/dipanjan/work/spring2009/FramenetParsing/FrameStructureExtraction "+tokenizedFile+" "+posTaggedFile);
	}
	
	public static void tokenizeSentences(List<String> sentences,String file)
	{	
		String tempFile="temp.txt";
		System.out.println(sentences.size());
		writeSentencesToTempFile(tempFile, sentences);
		runExternalCommand("./scripts/tokenizer.sed temp.txt", "tokenized.txt");
		List<String> tokenizedSentences=readSentencesFromFile("tokenized.txt");
		if(sentences.size()!=tokenizedSentences.size())
		{
			System.err.println("Problem in tokenized file. Exiting.");
			System.exit(0);
		}
		writeSentencesToTempFile(file,tokenizedSentences);
	}
	
	
	public static void tokenizeSentencesOPENNLP(List<String> sentences)
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
	
	/**
	 * @param file Path to the file
	 * @return List of all lines from the given file
	 */
	public static ArrayList<String> readSentencesFromFile(String file) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(file));
			return readLinesFromFile(reader, 0, true);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Reads up to a certain number of lines of a file, starting at the current position of the {@link BufferedReader}
	 * @param reader
	 * @param n Nonnegative number of lines to read ({@code 0} to read all lines).
	 * @param closeAtEOF If {@code true} and either {@code n==0} or fewer than {@code n} line could be fetched 
	 * due to the end of the file, the stream will be closed. Otherwise, closing the stream is the caller's responsibility.
	 * @return List of the lines in the range
	 */
	public static ArrayList<String> readLinesFromFile(BufferedReader reader, int n, boolean closeAtEOF) {
		int j = 0;
		ArrayList<String> result = new ArrayList<String>();
		try
		{
			String line = null;
			while((n==0 || j<n) && (line=reader.readLine())!=null)
			{
				result.add(line.trim());
				j++;
			}
			if (closeAtEOF && (n==0 || j<n))
				reader.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * @param file Path to the file
	 * @param n Maximum number of lines to read; if 0, all lines will be read. Must be nonnegative.
	 * @param start Index of first line to include (first line is 0)
	 * @return List of lines of the specified file in the specified range--i.e. lines {@code start} through {@code (start+n-1)}
	 */
	public static ArrayList<String> readSentencesFromFile(String file, int n, int start)	// TODO
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
	
	public static String readLineFromFile(BufferedReader reader, int lineOffset) {
		try {
			int l=0;
			while(l<lineOffset && reader.readLine()!=null)
			{
				l++;
			}
			return reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}	
	
	
	public static void writeSentencesToTempFile(String file, List<String> sentences)
	{
		try
		{
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(file));
			int size = sentences.size();
			//System.out.println("Size of sentences:"+size);
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
	
	public static void getSentences(List<String> keyWords, List<String> sentences)
	{
		getSentencesForSpecificFile(keyWords, sentences, TRAIN_SET);
		getSentencesForSpecificFile(keyWords, sentences, DEV_SET);
		getSentencesForSpecificFile(keyWords, sentences, TEST_SET);
	}
	
	public static void getSentencesForSpecificFile(List<String> keyWords, List<String> sentences, String file)
	{
		Document firstDoc = XmlUtils.parseXmlFile(file, false);
		Element[] eList = XmlUtils.applyXPath(firstDoc, "/entailment-corpus/pair");
		int length = eList.length;
		for(int i = 0; i < length; i ++)
		{
			String id = eList[i].getAttribute("id");
			String sampleLength = eList[i].getAttribute("length");
			String dataset = eList[i].getAttribute("dataset");
			Node t=null;
			Node h=null;
			for (Node j = eList[i].getFirstChild(); j != null; j = j.getNextSibling())
			{
				if(j.getNodeName().equals("t"))
					t=j;
				if(j.getNodeName().equals("h"))
					h=j;
			}
			String tText = XmlUtils.getTextNode((Element)t);
			String hText = XmlUtils.getTextNode((Element)h);
			ArrayList<String> sentenceList = splitSentences(tText.trim());
			int len = sentenceList.size();
			for(int k = 0; k < len; k ++)
			{
				keyWords.add(dataset+"_"+id+"_t");
				sentences.add(sentenceList.get(k).trim());
			}					
			keyWords.add(dataset+"_"+id+"_h");
			sentences.add(hText.trim());
			System.out.println("Processed element:"+i);
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
