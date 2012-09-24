/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * AllAnnotationsMergingWithoutNE.java is part of SEMAFOR 2.0.
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

import edu.cmu.cs.lti.ark.fn.utils.LemmatizeStuff;

public class AllAnnotationsMergingWithoutNE
{
	public static void main(String[] args)
	{
		ArrayList<String> tokenizedSentences = ParsePreparation.readSentencesFromFile(args[0]);
		ArrayList<String> neSentences = findDummyNESentences(tokenizedSentences);
		ArrayList<ArrayList<String>> parses = OneLineDataCreation.readCoNLLParses(args[1]);
		ArrayList<String> perSentenceParses=OneLineDataCreation.getPerSentenceParses(parses,tokenizedSentences,neSentences);
		ParsePreparation.writeSentencesToTempFile(args[2], perSentenceParses);
		LemmatizeStuff.lemmatize(args[3], args[4], args[2], args[5]);
	}
	
	public static ArrayList<String> findDummyNESentences(ArrayList<String> tokenizedSentences)
	{
		ArrayList<String> res = new ArrayList<String>();
		for(String sent:tokenizedSentences)
		{
			StringTokenizer st = new StringTokenizer(sent.trim());
			String resSent = "";
			while(st.hasMoreTokens())
			{
				resSent+=st.nextToken()+"_O"+" ";
			}
			resSent=resSent.trim();
			res.add(resSent);
		}
		return res;
	}
} 
