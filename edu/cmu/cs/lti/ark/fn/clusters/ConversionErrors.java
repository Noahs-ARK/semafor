/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * ConversionErrors.java is part of SEMAFOR 2.0.
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

import java.io.*;
import java.util.ArrayList;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;

public class ConversionErrors
{
	public static ArrayList<String> seenFiles = new ArrayList<String>();
	static
	{
		seenFiles.add("54.out");
	}
	
	public static void main(String[] args)
	{
		String rootDir = "/mal2/dipanjan/experiments/FramenetParsing/FrameStructureExtraction/mstscripts";
		String outFolder = rootDir+"/out";
		String errFolder = rootDir+"/error";
		
		File errDir = new File(errFolder);
		FilenameFilter filter = new FilenameFilter(){
			public boolean accept(File dir, String name) {
				return name.endsWith(".out");
			}
		};
		String[] files = errDir.list(filter);
		for(String file:files)
		{
			String filename = errFolder+"/"+file;
			ArrayList<String> lines = ParsePreparation.readSentencesFromFile(filename);
			boolean errFlag = false;
			for(String line:lines)
			{
				if(line.contains("java"))
				{
					errFlag=true;
					System.out.println(line);
				}
				if(errFlag)
				{
					String outFile=outFolder+"/"+file;
					ArrayList<String> outLines = ParsePreparation.readSentencesFromFile(outFile);
					for(String ol:outLines)
						System.out.println(ol);
					System.out.println();
					//System.exit(0);
				}
			}
		}
		
	}
	
	
}
