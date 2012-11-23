/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * WordnetInteraction.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.wordnet;



import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;


import danbikel.wordnet.Morphy;
import danbikel.wordnet.WordNet;



public class WordnetInteraction
{
	WordNet wn=null;
	Morphy morphy=null;
	
	public WordnetInteraction(String wnFilePath)
	{
		wn = new WordNet(wnFilePath);
		morphy=new Morphy(wn);
	}	
	
	
	public String returnRoot(String word, String POS)
	{
		String wnPOS;
		if(POS.startsWith("V"))
		{
			wnPOS = WordNet.verbPos;
		}
		else if(POS.startsWith("J"))
		{
			wnPOS = WordNet.adjPos;
		}
		else if(POS.startsWith("R"))
		{
			wnPOS = WordNet.advPos;
		}
		else
			wnPOS = WordNet.nounPos;
		
		String[] words=morphy.morphStr(word, wnPOS);
		
		if(words.length==0)
		{
			return word;
		}
		else
		{
			if(words.length>1)
			{
				System.out.println("Verb:"+word+" has more than 1 root.");
			}
			return words[0];
		}
	}
	
}
