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


public class FixTokenization {
	public static String replaceBritishWords(String line, THashMap<String,String> conv) {
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
