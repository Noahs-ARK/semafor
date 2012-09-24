/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * DependencyParsesCaching.java is part of SEMAFOR 2.0.
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
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParses;



public class DependencyParsesCaching
{
	public static WordNetRelations wnr = new WordNetRelations("lrdata/stopwords.txt", "file_properties.xml");
	
	public static void main(String[] args) throws Exception
	{
		String prefix = args[0];
		String inputDir = args[1];
		String outputDir = args[2];
		
		cacheParses(prefix,inputDir,outputDir);			
	}
	
	
	public static void cacheParses(final String prefix, String inputDir, String outputDir) throws Exception
	{
		File dir = new File(inputDir);
		FilenameFilter filter = new FilenameFilter(){
			public boolean accept(File dir, String name) {
				return name.startsWith(prefix)&&name.endsWith("converted");
			}			
		};
		String[] files = dir.list(filter);
		String prevSent = "";
		outputDir=outputDir+"/"+prefix;
		int count = 0;
		ArrayList<String> parses = new ArrayList<String>();
		String bSent = "";
		boolean passed = false;
		Arrays.sort(files);
		for(String file:files)
		{
			String convertedParseFile = inputDir+"/"+file;
			BufferedReader bReader = new BufferedReader(new FileReader(convertedParseFile));
			String line = null;
			while(true)
			{
				String parse = "";
				bSent="";
				while((line=bReader.readLine())!=null)
				{
					line=line.trim();
					if(line.equals(""))
						break;
					String[] toks = line.split("\t");
					bSent+=toks[1]+" ";
					parse+=toks[3]+"_"+toks[7]+"_"+toks[6]+" ";
				}
				if(line==null)
					break;
				bSent=bSent.trim();
				parse=parse.trim();
				if(!bSent.equals(prevSent))
				{
					if(passed)
					{
						writeParsesString(parses,prevSent,count,outputDir);
						count++;
					}
					passed=true;
					parses=new ArrayList<String>();
					parses.add(parse);
				}
				else
				{
					parses.add(parse);
				}
				prevSent=""+bSent;
			}
			bReader.close();
		}
		writeParsesString(parses,prevSent,count,outputDir);
	}
	
	public static boolean deleteDirectory(File path) {
	    if( path.exists() ) {
	      File[] files = path.listFiles();
	      for(int i=0; i<files.length; i++) {
	         if(files[i].isDirectory()) {
	           deleteDirectory(files[i]);
	         }
	         else {
	           files[i].delete();
	         }
	      }
	    }
	    return( path.delete() );
	  }

	
	public static void writeParsesString(ArrayList<String> parses, String bSent, int count, String outputDir)
	{
		System.out.println("Writing parses for sentence:"+count);
		System.out.println(bSent);
		outputDir = outputDir+"/parseDir_"+count;
		File oDir = new File(outputDir);
		if(!oDir.exists())
		{
			if(!oDir.mkdir())
			{
				System.out.println("Could not create directory:"+oDir.getAbsolutePath());
				System.exit(0);
			}
		}
		else
		{
			deleteDirectory(oDir);
			if(!oDir.mkdir())
			{
				System.out.println("Could not create directory:"+oDir.getAbsolutePath());
				System.exit(0);
			}
		}
		bSent=bSent.trim();
		String[] toks = bSent.split(" ");
		ArrayList<String> reducedSet = new ArrayList<String>();
		String lastParse="";
		for(String parse:parses)
		{
			parse=parse.trim();
			String[] pToks = parse.split(" ");
			if(pToks.length!=toks.length)
			{
				System.out.println("Problem with length:");
				System.out.println(bSent);
				System.out.println(parse);
				System.exit(0);
			}	
			if(parse.equals(lastParse))
				continue;
			lastParse=""+parse;
			reducedSet.add(parse);
		}
		int size = reducedSet.size();
		System.out.println("Size of unique dependency parses:"+size);
		int countDP = 0;
		for(String parse:reducedSet)
		{
			int len = toks.length;
			String[] data = new String[6];
			String[] pToks = parse.trim().split(" ");
			String[][] pArr = new String[3][len];
			pArr[0]=new String[len];
			pArr[1]=new String[len];
			pArr[2]=new String[len];
			for(int i = 0; i < pToks.length; i ++)
			{
				String[] arr = pToks[i].split("_");
				pArr[0][i]=arr[0]+"";
				pArr[1][i]=arr[1]+"";
				pArr[2][i]=arr[2]+"";
			}
			for(int k = 0; k < 6; k ++)
				data[k]="";
			
			for(int j = 0; j < len; j ++)
			{
				data[0]+=toks[j]+"\t";
				String pos = pArr[0][j];
				String lemma = wnr.getLemmaForWord(toks[j], pos).toLowerCase();
				for(int k = 1; k <= 3; k ++)
				{
					data[k]+=""+pArr[k-1][j]+"\t";
				}
				data[4]+="O\t";
				data[5]+=lemma+"\t";
			}
			for(int k = 0; k < 6; k ++)
				data[k]=data[k].trim();
			String line = ""+len+"\t";
			for(int k = 0; k < 6; k ++)
				line+=data[k]+"\t";
			line=line.trim();
			ArrayList<String> list = new ArrayList<String>();
			list.add(line);
			ParsePreparation.writeSentencesToTempFile(outputDir+"/parse_"+countDP+".parse",list);
			countDP++;
		}
	}
	
	
	public static void writeParses(ArrayList<String> parses, String bSent, int count, String outputDir)
	{
		System.out.println("Writing parses for sentence:"+count);
		System.out.println(bSent);
		String outputFile = outputDir+"/parse_"+count+".jobj";
		bSent=bSent.trim();
		String[] toks = bSent.split(" ");
		ArrayList<String> reducedSet = new ArrayList<String>();
		String lastParse="";
		for(String parse:parses)
		{
			parse=parse.trim();
			String[] pToks = parse.split(" ");
			if(pToks.length!=toks.length)
			{
				System.out.println("Problem with length:");
				System.out.println(bSent);
				System.out.println(parse);
				System.exit(0);
			}	
			if(parse.equals(lastParse))
				continue;
			lastParse=""+parse;
			reducedSet.add(parse);
		}
		int size = reducedSet.size();
		System.out.println("Size of unique dependency parses:"+size);
		DependencyParse[] dArr = new DependencyParse[size];
		int countDP = 0;
		for(String parse:reducedSet)
		{
			int len = toks.length;
			String[] data = new String[6];
			String[] pToks = parse.trim().split(" ");
			String[][] pArr = new String[3][len];
			pArr[0]=new String[len];
			pArr[1]=new String[len];
			pArr[2]=new String[len];
			for(int i = 0; i < pToks.length; i ++)
			{
				String[] arr = pToks[i].split("_");
				pArr[0][i]=arr[0]+"";
				pArr[1][i]=arr[1]+"";
				pArr[2][i]=arr[2]+"";
			}
			for(int k = 0; k < 6; k ++)
				data[k]="";
			
			for(int j = 0; j < len; j ++)
			{
				data[0]+=toks[j]+"\t";
				String pos = pArr[0][j];
				String lemma = wnr.getLemmaForWord(toks[j], pos).toLowerCase();
				for(int k = 1; k <= 3; k ++)
				{
					data[k]+=""+pArr[k-1][j]+"\t";
				}
				data[4]+="O\t";
				data[5]+=lemma+"\t";
			}
			for(int k = 0; k < 6; k ++)
				data[k]=data[k].trim();
			DependencyParse[] theparses = DependencyParse.buildParseTrees(data, 0.0);
			if(theparses.length!=1)
			{
				System.out.println("Error in creating dependency trees. parse:"+parse);
				System.exit(0);
			}
			dArr[countDP]=theparses[0];
			countDP++;
		}
		DependencyParses dps = new DependencyParses(dArr);
		SerializedObjects.writeSerializedObject(dps, outputFile);
	}
	
}
