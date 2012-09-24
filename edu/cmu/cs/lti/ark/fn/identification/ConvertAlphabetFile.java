/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * ConvertAlphabetFile.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.identification;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import edu.cmu.cs.lti.ark.fn.optimization.LDouble;
import gnu.trove.TIntObjectHashMap;


public class ConvertAlphabetFile
{
	
	public static void main(String[] args) throws Exception
	{
		String alphabetFile = args[0];
		String modelFile = args[1];
		String outFile = args[2];	
		
		TIntObjectHashMap<String> map = new TIntObjectHashMap<String>();
		try
		{
			String line = null;
			int count = 0;
			BufferedReader bReader = new BufferedReader(new FileReader(alphabetFile));
			while((line=bReader.readLine())!=null)
			{
				if(count==0)
				{
					count++;
					continue;
				}
				String[] toks = line.trim().split("\t");
				map.put(new Integer(toks[1]),toks[0]);
				
			}
			bReader.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}		
		
		try
		{
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(outFile));
			BufferedReader bReader = new BufferedReader(new FileReader(modelFile));
			int count = 0;
			String line = null;
			while((line=bReader.readLine())!=null)
			{
				if(count==0)
				{
					count++;
					continue;
				}
				double val = new Double(line.trim());
				String feat = map.get(count);
				bWriter.write(feat+"\t"+(LDouble.convertToLogDomain(val))+"\n");
				count++;
			}			
			bReader.close();
			bWriter.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}	
	}	
}
