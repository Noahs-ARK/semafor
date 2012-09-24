/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * FilterNewLines.java is part of SEMAFOR 2.0.
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
import java.io.FileReader;
import java.io.FileWriter;


public class FilterNewLines
{
	public static void main(String[] args) throws Exception
	{
		String root = "/usr2/dipanjan/experiments/FramenetParsing/FrameStructureExtraction/mstscripts/data";
		String[] infiles = {"semeval.fulltrain.berkeley.parsed", "semeval.fulldev.berkeley.parsed", "semeval.fulltest.berkeley.parsed"};
		String[] outfiles = {"semeval.fulltrain.berkeley.parsed.trimmed", "semeval.fulldev.berkeley.parsed.trimmed", "semeval.fulltest.berkeley.parsed.trimmed"};
		
		for(int i = 0; i < 3; i ++)
		{
			BufferedReader bReader = new BufferedReader(new FileReader(root+"/"+infiles[i]));
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(root+"/"+outfiles[i]));
			String line = null;
			int count=0;
			while((line=bReader.readLine())!=null)
			{
				line=line.trim();
				if(!line.equals(""))
					bWriter.write(line+"\n");
				if(count%1000==0)
					System.out.print(count+" ");
				if(count%10000==0)
					System.out.println();
				count++;
			}
			bReader.close();
			bWriter.close();
		}
		
	}
}
