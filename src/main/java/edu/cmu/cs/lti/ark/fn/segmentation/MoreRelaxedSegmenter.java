/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * MoreRelaxedSegmenter.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.segmentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;

import gnu.trove.*;

public class MoreRelaxedSegmenter
{
	public static final int MAX_LEN = 4;
	
	private DependencyParse[] mNodeList = null;
	private DependencyParse mParse = null;
		
	public static void main(String[] args)
	{
		//createM45Data();
		RoteSegmenter seg = new RoteSegmenter();
		seg.roteSegmentation();
	}
	
	public MoreRelaxedSegmenter() {
		mNodeList = null;
		mParse =  null;
	}	
	
	public ArrayList<String[][]> readNewParses(String file)
	{
		ArrayList<String> lines = ParsePreparation.readSentencesFromFile(file);
		int size = lines.size();
		ArrayList<String> collection = new ArrayList<String>();
		ArrayList<String[][]> result = new ArrayList<String[][]>();
		for(int i = 0; i < size; i ++)
		{
			String line = lines.get(i).trim();
			if(line.equals(""))
			{
				String[][] parse = getParse(collection);
				result.add(parse);
				collection = new ArrayList<String>();
			}		
			else
			{
				collection.add(line);
			}			
		}		
		return result;
	}
	
	public String[][] getParse(ArrayList<String> collection)
	{
		int size = collection.size();
		String[][] result = new String[size][];
		for(int i = 0; i < size; i ++)
		{
			result[i] = new String[4];
			String[] arr = collection.get(i).trim().split("\t");
			result[i][0]=""+arr[5];
			result[i][1]=""+arr[7];
			result[i][2]=""+arr[8];
			result[i][3]=""+arr[9];
		}		
		return result;
	}
	
	
	public THashSet<String> setOfParticles(THashMap<String,THashSet<String>> mFrameMap)
	{
		THashSet<String> result = new THashSet<String>();
		Set<String> set = mFrameMap.keySet();
		int count = 0;
		for(String string:set)
		{
			THashSet<String> hus = mFrameMap.get(string);
			System.out.println(count+"\t"+string);
			count++;
			for(String hu: hus)
			{
				String[] wps = hu.trim().split(" ");
				if(wps.length==1)
					continue;
				String lastWord = wps[wps.length-1];
				String[] arr = lastWord.split("_");					
			}
		}
		return result;
	}	
	
	
	public String getHighRecallSegmentation(String parse, 
												   THashSet<String> allRelatedWords)
	{				
		StringTokenizer st = new StringTokenizer(parse,"\t");
		int tokensInFirstSent = new Integer(st.nextToken());
		String[][] data = new String[6][tokensInFirstSent];
		for(int k = 0; k < 6; k ++)
		{
			data[k]=new String[tokensInFirstSent];
			for(int j = 0; j < tokensInFirstSent; j ++)
			{
				data[k][j]=""+st.nextToken().trim();
			}
		}
		ArrayList<String> startInds = new ArrayList<String>();
		for(int i = 0; i < data[0].length; i ++)
		{
			startInds.add(""+i);
		}
		String tokNums="";
		for(int i = MAX_LEN; i >= 1; i--)
		{
			for(int j = 0; j <= (data[0].length-i); j ++)
			{
				String ind = ""+j;
				if(!startInds.contains(ind))
					continue;
				String lTok = "";
				for(int k = j; k < j + i; k ++)
				{
					String pos = data[1][k];
					String cPos = pos.substring(0,1);
					String l = data[5][k];    
					lTok+=l+"_"+cPos+" ";
				}
				lTok=lTok.trim();
				if (i > 1) {
					if(allRelatedWords.contains(lTok))
					{
						String tokRep = "";
						for(int k = j; k < j + i; k ++)
						{
							tokRep += k+" ";
							ind = ""+k;
							startInds.remove(ind);
						}
						tokRep=tokRep.trim().replaceAll(" ", "_");
						tokNums+=tokRep+"\t";
					}
				} else {
					String pos = data[1][j];
					String word = data[0][j];
					if (!pos.equals("NNP") && !containsPunc(word)) {
						tokNums+=j + "\t";
						ind = "" + j;
						startInds.remove(ind);
					} 
				}
			}			
		}
		tokNums=tokNums.trim();
		return tokNums;
	}	
	
	public boolean containsPunc(String word) {
		char first = word.toLowerCase().charAt(0);
		char last = word.toLowerCase().charAt(word.length()-1);
		if (Character.isLetter(first) && Character.isLetter(last)) {
			return false;
		} else {
			return true;
		}
	}
	
	public String trimPrepositions(String tokNum, String[][] pData)
	{
		String[] forbiddenWords1 = {"of course", "in particular", "as", "for", "so", "with","a","an","the","it","i"};
		String[] precedingWordsOf = {"only", "member", "one", "most", "many", "some", "few", "part",
									"majority", "minority", "proportion", "half", "third", "quarter", 
									"all", "none", "share", "much", "%", "face","more"};
		String[] followingWordsOf = {"all", "group", "them", "us", "their"};
						
		Arrays.sort(forbiddenWords1);
		Arrays.sort(precedingWordsOf);
		Arrays.sort(followingWordsOf);
			
		String[] candToks = tokNum.trim().split("\t");
		
		String result = "";
		for(String candTok: candToks)
		{
			if(candTok.contains("_"))
			{
				result+=candTok+"\t";
				continue;
			}
			int start = new Integer(candTok);
			int end = new Integer(candTok);
			/*
			 *forbidden words
			 */
			String token = "";
			String pos = "";
			String tokenLemma = "";
			for(int i = start; i <= end; i ++)
			{
				String tok = pData[0][i].toLowerCase();
				String p = pData[1][i];
				tokenLemma = pData[5][i] + " ";
				token += tok+" ";
				pos += p+" ";
			}
			token=token.trim();
			pos = pos.trim();
			tokenLemma = tokenLemma.trim();
			if(Arrays.binarySearch(forbiddenWords1, token)>=0)
			{
				continue;
			}		
						
			if(start==end)
			{
				String POS = pData[1][start];
				if(POS.startsWith("PR")||
						POS.startsWith("CC")||
						POS.startsWith("IN")||
						POS.startsWith("TO")||
						POS.startsWith("LS")||
						POS.startsWith("FW")||
						POS.startsWith("UH")||
						POS.startsWith("W"))
					continue;				
				if(token.equals("course"))
				{
					if(start>=1)
					{
						String precedingWord = pData[0][start-1].toLowerCase();
						if(precedingWord.equals("of"))
							continue;
					}
				}
				if(token.equals("particular"))
				{
					if(start>=1)
					{
						String precedingWord = pData[0][start-1].toLowerCase();
						if(precedingWord.equals("in"))
							continue;
					}
				}
			}			
			/*
			 * the of case
			 */
			if(token.equals("of"))
			{
				String precedingWord = null;
				String precedingPOS = null;
				String precedingNE = null;
				if(start>=1)
				{
					precedingWord = pData[0][start-1].toLowerCase();
					precedingPOS = pData[1][start-1];
					precedingWord = pData[5][start-1];
					precedingNE = pData[4][start-1];
				}
				else
				{
					precedingWord = "";
					precedingPOS = "";
					precedingNE = "";
				}
				if(Arrays.binarySearch(precedingWordsOf, precedingWord)>=0)
				{
					result+=candTok+"\t";
					continue;
				}
				String followingWord = null;
				String followingPOS = null;
				String followingNE = null;
				if(start<pData[0].length-1)
				{
					followingWord = pData[0][start+1].toLowerCase();
					followingPOS = pData[1][start+1];
					followingNE = pData[4][start+1];
				}
				else
				{
					followingWord = "";
					followingPOS = "";
					followingNE = "";
				}
				if(Arrays.binarySearch(followingWordsOf, followingWord)>=0)
				{
					result+=candTok+"\t";
					continue;
				}
				
				if(precedingPOS.startsWith("JJ") || precedingPOS.startsWith("CD"))
				{
					result+=candTok+"\t";
					continue;
				}
					
				if(followingPOS.startsWith("CD"))
				{
					result+=candTok+"\t";
					continue;
				}
				
				if(followingPOS.startsWith("DT"))
				{
					if(start<pData[0].length-1)
					{
						followingPOS = pData[1][start+2];
						if(followingPOS.startsWith("CD"))
						{
							result+=candTok+"\t";
							continue;
						}
					}
				}
				if(followingNE.startsWith("GPE")||followingNE.startsWith("LOCATION"))
				{
					result+=candTok+"\t";
					continue;
				}
				if(precedingNE.startsWith("CARDINAL"))
				{
					result+=candTok+"\t";
					continue;
				}
				
				continue;
			}
			
			/*
			 * the will case
			 */
			if(token.equals("will"))
			{
				if(pos.equals("MD"))
				{
					continue;
				}
				else
				{
					result+=candTok+"\t";
					continue;
				}
			}
			
			
			if(start==end)
			{
				DependencyParse headNode = mNodeList[start+1];
				/*
				 * the have case
				 *
				 */
				String lToken = tokenLemma;
				String hLemma = "have";
				if(lToken.equals(hLemma))
				{
					List<DependencyParse> children = headNode.getChildren();
					boolean found = false;
					for(DependencyParse parse: children)
					{
						String lab = parse.getLabelType();
						if(lab.equals("OBJ"))
						{
							found = true;
						}
					}
					if(found)
					{
						result+=candTok+"\t";
						continue;
					}
					else
					{
						continue;
					}
				}
				
				/*
				 * the be case
				 */
				lToken = tokenLemma;
				hLemma = "be";
				if(lToken.equals(hLemma))
				{
					continue;
				}
			}		
			result+=candTok+"\t";
		}
		return result.trim();
	}
	
	public String removeSupportVerbs(String tokNum, String[][] pData, WordNetRelations mWNR, String[][] nData)
	{
		String result = "";
		String[] candToks = tokNum.trim().split("\t");
		for(String candTok: candToks)
		{
			
		}
		return result;
	}
	
	public ArrayList<String> findSegmentationForTest(ArrayList<String> tokenNums, 
			ArrayList<String> parses, 
			THashSet<String> allRelatedWords)
	{
		ArrayList<String> result = new ArrayList<String>();
		for(String tokenNum: tokenNums)
		{
			String[] toks = tokenNum.split("\t");
			String gold = "";
			for(int i = 0; i < toks.length-1; i ++)
				gold+=toks[i]+"\t";
			gold=gold.trim();
			int sentNum = new Integer(toks[toks.length-1]);
			String parse = parses.get(sentNum);
			String tokNums = getHighRecallSegmentation(parse,allRelatedWords);
			StringTokenizer st = new StringTokenizer(parse.trim(),"\t");
			int tokensInFirstSent = new Integer(st.nextToken());
			String[][] data = new String[6][tokensInFirstSent];
			for(int k = 0; k < 6; k ++)
			{
				data[k]=new String[tokensInFirstSent];
				for(int j = 0; j < tokensInFirstSent; j ++)
				{
					data[k][j]=""+st.nextToken().trim();
				}
			}
			mParse = DependencyParse.processFN(data, 0.0);
			mNodeList = DependencyParse.getIndexSortedListOfNodes(mParse);
			mParse.processSentence();
			if(!tokNums.trim().equals(""))
				tokNums=trimPrepositions(tokNums, data);
			String line1 = getTestLine(gold,tokNums,data).trim()+"\t"+sentNum;
			line1=line1.trim();
			// System.out.println(line1+"\n"+mParse.getSentence()+"\n");
			result.add(line1.trim());
		}		
		return result;
	}
	
	public String getTestLine(String goldTokens, String actualTokens, String[][] data)
	{
		String result = "";
		ArrayList<String> goldList = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(goldTokens.trim(),"\t");
		while(st.hasMoreTokens())
		{
			goldList.add(st.nextToken());
		}
		ArrayList<String> actList = new ArrayList<String>();
		st = new StringTokenizer(actualTokens.trim(),"\t");
		while(st.hasMoreTokens())
		{
			actList.add(st.nextToken());
		}		
		
		int goldSize = goldList.size();
		for(int i = 0; i < goldSize; i ++)
		{
			result+=goldList.get(i).trim()+"#true"+"\t";
		}	
		result=result.trim()+"\t";
		int actSize = actList.size();
		for(int i = 0; i < actSize; i ++)
		{
			String tokNum = actList.get(i).trim();
			if(!goldList.contains(tokNum))
			{
				result+=tokNum+"#true"+"\t";
			}
		}
		return result.trim();
	}
	

	
	public ArrayList<String> findSegmentation(ArrayList<String> tokenNums, 
													 ArrayList<String> parses, 
													 THashSet<String> allRelatedWords, 
													 WordNetRelations mWNR)
	{
		ArrayList<String> result = new ArrayList<String>();
		for(String tokenNum: tokenNums)
		{
			String[] toks = tokenNum.split("\t");
			String gold = "";
			for(int i = 0; i < toks.length-1; i ++)
				gold+=toks[i]+"\t";
			gold=gold.trim();
			int sentNum = new Integer(toks[toks.length-1]);
			String parse = parses.get(sentNum);
			String tokNums = getHighRecallSegmentation(parse,allRelatedWords);
			StringTokenizer st = new StringTokenizer(parse.trim(),"\t");
			int tokensInFirstSent = new Integer(st.nextToken());
			String[][] data = new String[5][tokensInFirstSent];
			for(int k = 0; k < 5; k ++)
			{
				data[k]=new String[tokensInFirstSent];
				for(int j = 0; j < tokensInFirstSent; j ++)
				{
					data[k][j]=""+st.nextToken().trim();
				}
			}
			mParse = DependencyParse.processFN(data, 0.0);
			mNodeList = DependencyParse.getIndexSortedListOfNodes(mParse);
			mParse.processSentence();
			if(!tokNums.trim().equals(""))
				tokNums=trimPrepositions(tokNums, data);
			//if(!tokNums.trim().equals(""))
			//	tokNums=removeSupportVerbs(tokNums, data, mWNR, newParse);
			String line = "zzzz\t"+gold+"#"+tokNums;
			String line1 = getActualTokenLine(gold,tokNums,data);
			String sentence = mParse.getSentence();
			System.out.println(line1+"\n"+mParse.getSentence()+"\n");
			result.add(line);
		}		
		return result;
	}	
	
	public String getActualTokenLine(String goldTokens, String actualTokens, String[][] data)
	{
		String result = "";
		ArrayList<String> goldList = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(goldTokens.trim(),"\t");
		while(st.hasMoreTokens())
		{
			goldList.add(st.nextToken());
		}
		ArrayList<String> actList = new ArrayList<String>();
		st = new StringTokenizer(actualTokens.trim(),"\t");
		while(st.hasMoreTokens())
		{
			actList.add(st.nextToken());
		}		
		
		int goldSize = goldList.size();
		for(int i = 0; i < goldSize; i ++)
		{
			String tokNum = goldList.get(i).trim();
			String[] toks = tokNum.split("_");
			String token = "";
			for(int j = 0; j < toks.length; j ++)
			{
				int num = new Integer(toks[j]);
				token+=data[0][num]+"_"+data[1][num]+" ";
			}
			token=token.trim();
			result+=token+"_"+tokNum+"\t";
		}	
		result=result.trim()+"\n";
		int actSize = actList.size();
		for(int i = 0; i < actSize; i ++)
		{
			String tokNum = actList.get(i).trim();
			String[] toks = tokNum.split("_");
			String token = "";
			for(int j = 0; j < toks.length; j ++)
			{
				int num = new Integer(toks[j]);
				token+=data[0][num]+"_"+data[1][num]+" ";
			}
			token=token.trim();
			result+=token+"_"+tokNum+"\t";
		}
		return result.trim();
	}	
}
