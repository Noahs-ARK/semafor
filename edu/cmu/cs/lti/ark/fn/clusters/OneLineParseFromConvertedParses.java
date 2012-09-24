/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * OneLineParseFromConvertedParses.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.clusters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;



public class OneLineParseFromConvertedParses
{
	public static void main(String[] args) throws Exception
	{
		String lemmaTagsFile = args[0];
		String convertedParseFileDirectory = args[1];
		final String filePrefix = args[2];
		String outputFile = args[3];
		
		File dir = new File(convertedParseFileDirectory);
		FilenameFilter filter = new FilenameFilter()
		{
			public boolean accept(File arg0, String arg1)
			{
				return arg1.startsWith(filePrefix)&&arg1.endsWith("converted");
			}	
		};
		
		String[] files = dir.list(filter);		
		ArrayList<String> sentences = ParsePreparation.readSentencesFromFile(lemmaTagsFile);
		//produceOneLineParseFile(sentences,convertedParseFile,outputFile);
		writeUniqueSentences(convertedParseFileDirectory,files,outputFile);
	}
	
	
	public static void writeUniqueSentences(String dir,String[] files,String outputFile) throws Exception
	{
		Arrays.sort(files);
		ArrayList<String> allSents = new ArrayList<String>();
		String prevSent="";
		for(String file:files)
		{
			String convertedParseFile = dir+"/"+file;
			BufferedReader bReader = new BufferedReader(new FileReader(convertedParseFile));
			String line = null;
			while(true)
			{
				String bSent = "";
				while((line=bReader.readLine())!=null)
				{
					line=line.trim();
					if(line.equals(""))
						break;
					String[] toks = line.split("\t");
					bSent+=toks[1]+" ";
				}
				if(line==null)
					break;
				bSent=bSent.trim();
				if(!bSent.equals(prevSent))
				{
					allSents.add(bSent);
					System.out.println(bSent);
				}
				prevSent=""+bSent;
			}
			bReader.close();
		}
		ParsePreparation.writeSentencesToTempFile(outputFile, allSents);
		System.out.println("Size of sentences:"+allSents.size());
	}
	
	public static void produceOneLineParseFile(ArrayList<String> sentences, String convertedParseFile, String outputFile) throws Exception
	{
		BufferedReader bReader = new BufferedReader(new FileReader(convertedParseFile));
		BufferedWriter bWriter = new BufferedWriter(new FileWriter(outputFile));
		for(String parsedSent:sentences)
		{
			StringTokenizer st = new StringTokenizer(parsedSent,"\t");
			int tokensInFirstSent = new Integer(st.nextToken());
			String[][] data = new String[5][tokensInFirstSent];
			String sent = "";
			for(int k = 0; k < 5; k ++)
			{
				data[k]=new String[tokensInFirstSent];
				for(int j = 0; j < tokensInFirstSent; j ++)
				{
					data[k][j]=""+st.nextToken().trim();
					if(k==0)
						sent+=data[k][j].toLowerCase()+" ";
				}
			}
			sent=sent.trim();
			System.out.println(sent);
			String line = null;
			String bSent = "";
			while(!(line=bReader.readLine()).trim().equals(""))
			{
				line=line.trim();
				String[] toks = line.split("\t");
				bSent+=toks[1].toLowerCase()+" ";
			}			
			bSent=bSent.trim();
			System.out.println(bSent);
			
		}	
		bReader.close();
		bWriter.close();
	}
	
	
}
