/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * PaperEvaluation.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.evaluation;

import java.util.ArrayList;
import java.util.StringTokenizer;



public class PaperEvaluation
{
	public static String[] getTokens(String line)
	{
		StringTokenizer st = new StringTokenizer(line,"\t",true);
		ArrayList<String> toks = new ArrayList<String>();
		while(st.hasMoreTokens())
		{
			String tok = st.nextToken().trim();
			if(tok.equals(""))
			{
				continue;
			}
			toks.add(tok);
		}
		
		String[] arr = new String[toks.size()];
		return toks.toArray(arr);
	}
	
}
