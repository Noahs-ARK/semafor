/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * OneLineDataCreation.java is part of SEMAFOR 2.0.
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

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.FileReader;


public class OneLineDataCreation
{
	public static final String POSTAG_SUFFIX = ".pos.tagged";
	public static final String NETAG_SUFFIX = ".ne.tagged";
	public static final String TOKENIZED_SUFFIX = ".tokenized";
	public static final String CONLL_PARSED_SUFFIX = ".conll.parsed";
	public static final String ONELINE_SUFFIX = ".all.tags";
	public static final String DIR_ROOT = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData";
		
	public static void main(String[] args)
	{
		String[] prefixes = {"framenet.original.sentences","semeval.fulltrain.sentences","semeval.fulldev.sentences","semeval.fulltest.sentences"};
		int size = prefixes.length;
		for(int i = 0; i < size; i ++)
		{
			transFormIntoPerLineParse(prefixes[i]);
		}		
	}
	
	public static void transFormIntoPerLineParse(String prefix)
	{
		prefix = DIR_ROOT+"/"+prefix;
		ArrayList<ArrayList<String>> parses = readCoNLLParses(prefix+CONLL_PARSED_SUFFIX);
		ArrayList<String> tokenizedSentences = ParsePreparation.readSentencesFromFile(prefix+TOKENIZED_SUFFIX);
		ArrayList<String> neTaggedSentences = ParsePreparation.readSentencesFromFile(prefix+NETAG_SUFFIX);
		ArrayList<String> perSentenceParses=getPerSentenceParses(parses,tokenizedSentences,neTaggedSentences);
		ParsePreparation.writeSentencesToTempFile(prefix+ONELINE_SUFFIX, perSentenceParses);
	}
	
	public static ArrayList<String> getPerSentenceParses(ArrayList<ArrayList<String>> parses, ArrayList<String> tokenizedSentences, ArrayList<String> neTaggedSentences)
	{
		ArrayList<String> result = new ArrayList<String>();
		ArrayList<String> gatheredSents=new ArrayList<String>();
		ArrayList<String> gatheredParses=new ArrayList<String>();
		ArrayList<String> gatheredNESents=new ArrayList<String>();
		int size = parses.size();
		for(int i = 0; i < size; i ++)
		{	
			gatheredSents=new ArrayList<String>();
			gatheredParses=new ArrayList<String>();
			gatheredNESents=new ArrayList<String>();
			gatheredSents.add(tokenizedSentences.get(i));
			gatheredParses.addAll(parses.get(i));
			gatheredNESents.add(neTaggedSentences.get(i));
			String oneLineParse=processGatheredSentences(gatheredSents,gatheredParses,gatheredNESents);
			result.add(oneLineParse);
			// System.out.println("Processed "+i);
		}	
		
		return result;
	}
	
	
	public static String processGatheredSentences(ArrayList<String> gatheredSentences, ArrayList<String> gatheredParses, ArrayList<String> gatheredNESents)
	{
		String result="";
		int totalNumOfSentences=gatheredSentences.size();
		int totalTokens=0;
		int[] tokenNums=new int[totalNumOfSentences];
		String neLine="";
		for(int i = 0; i < totalNumOfSentences; i ++)
		{
			String tokenizedSentence=gatheredSentences.get(i);
			StringTokenizer st = new StringTokenizer(tokenizedSentence);
			totalTokens+=st.countTokens();
			tokenNums[i]=st.countTokens();
			while(st.hasMoreTokens())
			{
				result+=st.nextToken()+"\t";
			}
			st = new StringTokenizer(gatheredNESents.get(i));
			while(st.hasMoreTokens())
			{
				String token = st.nextToken();
				int lastInd=token.lastIndexOf("_");
				String NE=token.substring(lastInd+1);
				neLine+=NE+"\t";
			}
		}	
		result=totalTokens+"\t"+result;
		if(totalTokens!=gatheredParses.size())
		{
			System.out.println("Some problem: total number of tokens in gathered sentences not equal to gathered parses.");
			System.exit(0);
		}	
		int count = 0;
		String posTagSequence="";
		String labelTagSequence="";
		String parentTagSequence="";
		int offset=0;
		for(int i = 0; i < totalNumOfSentences; i ++)
		{
			if(i>0)
				offset+=tokenNums[i-1];
			for(int j = 0; j < tokenNums[i]; j ++)
			{
				String parseLine=gatheredParses.get(count).trim();
				StringTokenizer st = new StringTokenizer(parseLine,"\t");
				int countTokens=st.countTokens();
				if(countTokens!=10)
				{
					System.out.println("Parse line:"+parseLine+" does not have 10 tokens. Exiting.");
					System.exit(0);
				}
				st.nextToken();
				st.nextToken();
				st.nextToken();
				posTagSequence += st.nextToken().trim()+"\t";
				st.nextToken();
				st.nextToken();				
				int parent = new Integer(st.nextToken().trim());
				if(parent!=0)
				{
					parent+=offset;
				}
				parentTagSequence+=parent+"\t";
				labelTagSequence+=st.nextToken()+"\t";
				count++;                               
			}			
		}	
		result+=posTagSequence+labelTagSequence+parentTagSequence+neLine;
		result=result.trim();
		StringTokenizer st = new StringTokenizer(result,"\t");
		int tokensInFirstSent = new Integer(st.nextToken());
		String[] first = new String[5];
		for(int i = 0; i < 5; i ++)
		{
			first[i]="";
			for(int j = 0; j < tokensInFirstSent; j ++)
			{
				first[i]+=st.nextToken().trim()+"\t";
			}
			first[i]=first[i].trim();
		}
		return result.trim();
	}	
	

	
	
	public static ArrayList<ArrayList<String>> readCoNLLParses(String conllParseFile)
	{
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		try
		{
			BufferedReader bReader = new BufferedReader(new FileReader(conllParseFile));
			String line=null;
			ArrayList<String> thisParse = new ArrayList<String>();
			while((line=bReader.readLine())!=null)
			{
				line=line.trim();
				if(line.equals(""))
				{
					result.add(thisParse);
					thisParse = new ArrayList<String>();
				}
				else
				{
					thisParse.add(line);
				}
			}
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}
	
}


