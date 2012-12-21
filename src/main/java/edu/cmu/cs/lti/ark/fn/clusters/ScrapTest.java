/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * ScrapTest.java is part of SEMAFOR 2.0.
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

import gnu.trove.THashMap;

import java.io.BufferedReader;
import java.io.FileReader;


public class ScrapTest {
	public static THashMap<String,Integer> getSpansWithHeads(String file)
	{
		THashMap<String, Integer> spans = new THashMap<String, Integer>();
		THashMap<String, Integer> countMap = new THashMap<String,Integer>();
		try
		{
			String line = null;
			BufferedReader bReader = new BufferedReader(new FileReader(file));
			while((line=bReader.readLine())!=null)
			{
				String[] toks = line.trim().split("\t");
				char first = toks[0].charAt(0);
				if((first>='a'&&first<='z')||(first>='A'&&first<='Z')||(first>='0'&&first<='9'))
				{
					String word = toks[0].trim();
					int count = new Integer(toks[2].trim());
					if(countMap.contains(word))
					{
						if(countMap.get(word)<count)
						{
							countMap.put(word, count);
							spans.put(word, new Integer(toks[1].trim()));
						}
					}
					else
					{
						countMap.put(word, count);
						spans.put(word, new Integer(toks[1].trim()));
					}
				}
			}
			bReader.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		System.out.println("Size of spans:"+spans.size());
		return spans;
	}
}
