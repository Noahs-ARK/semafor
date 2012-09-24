/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * AllPossibleSpansSemEval.java is part of SEMAFOR 2.0.
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import gnu.trove.THashSet;


public class AllPossibleSpansSemEval
{
	public static void main(String[] args)
	{
		String[] files = {"lrdata/semeval.fulltrain.sentences",
						  "lrdata/semeval.fulldev.sentences",
						  "/usr2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/semeval.fulltest.sentences"
						 };
		THashSet<String> arguments = new THashSet<String>();
		for(int i = 0; i < 3; i ++)
		{
			ArrayList<String> sents = ParsePreparation.readSentencesFromFile(files[i]);
			for(String sent:sents)
			{
				StringTokenizer st = new StringTokenizer(sent," \t",true);
				ArrayList<String> tokens = new ArrayList<String>();
				while(st.hasMoreTokens())
				{
					String tok = st.nextToken().trim();
					if(tok.equals(""))
						continue;
					tokens.add(tok);
				}
				String[] arr = new String[tokens.size()];
				tokens.toArray(arr);
				for(int j = 0; j < arr.length; j ++)
				{
					for(int k = 0; k < arr.length; k ++)
					{
						if(j==k)
							continue;
						if(k<j)
							continue;
						String span = "";
						for(int l = j; l <= k; l ++)
							span+= arr[l]+" ";
						span=span.trim().toLowerCase();
						System.out.println(span);
						arguments.add(span);
					}
				}
			}
		}
		String[] allSpans = new String[arguments.size()];
		arguments.toArray(allSpans);
		Arrays.sort(allSpans);
		try
		{
			BufferedWriter bWriter = new BufferedWriter(new FileWriter("lrdata/allSemEvalSpans"));
			for(int i = 0; i < allSpans.length; i ++)
			{
				bWriter.write(allSpans[i]+"\n");
			}
			bWriter.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}	
	
}
