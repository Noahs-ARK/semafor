/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * TestDependencyParses.java is part of SEMAFOR 2.0.
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


import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.Range1Based;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParses;


public class TestDependencyParses
{
	public static void main(String[] args)
	{
		DependencyParses parses = (DependencyParses)SerializedObjects.readSerializedObject("../sampleParses/parse_981.jobj");
		System.out.println("Size of parses:"+parses.size());
		DependencyParse best = parses.get(0);
		DependencyParse[] arr = DependencyParse.getIndexSortedListOfNodes(best);
		for(int i = 1; i < arr.length; i ++)
		{
			for(int j = i; j < arr.length; j ++)
			{
				if(i==j)
					continue;
				String string = "";
				for(int k = i; k <= j; k ++)
					string+=arr[k].getWord()+" ";
				string=string.trim();
				Range1Based span = new Range1Based(i,j);
				Pair<Integer, DependencyParse> p = parses.matchesSomeSubtree(span);
				if(p!=null)
				{
					System.out.println(p+"\t\t\t\t"+string+"\t\theadIndex:"+p.getSecond().getIndex());
				}
			}
		}
		
//		
//		best.processSentence();
//		System.out.println(best.getSentence());
//		Range1Based span = new Range1Based(9,11);
//		Pair<Integer, DependencyParse> p = parses.matchesSomeSubtree(span);
//		System.out.println(p);
	}	
}
