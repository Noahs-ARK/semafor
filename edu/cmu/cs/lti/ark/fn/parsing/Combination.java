/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * Combination.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.parsing;

import java.util.ArrayList;
import java.util.Collections;
//import java.util.HashSet;
import java.util.List;

public class Combination implements Comparable<Combination>{
	public double lprob;
	public int choice;
	public FrameElement target;
	public int targetIndex;
	List<FrameElement>felist;
	public Combination(List<FrameElement> frameElementList,int choice){
		felist=frameElementList;
		this.choice=choice;
		for(FrameElement fe:felist){
			lprob+=fe.getProb();
		}
	}
	public int compareTo(Combination c){
		if(c.lprob>lprob){
			return 1;
		}
		if(c.lprob<lprob){
			return -1;
		}
		return 0;
	}
	public Combination (FrameElement Target,String feLine){
		String toks[]=feLine.trim().split("\t");
		felist=new ArrayList<FrameElement>();
		felist.add(Target);
		if(toks.length>1){
			for ( int i=0;i<toks.length-1;i+=2){
				String fename=toks[i];
				String fespantoks[]=toks[i+1].split(":");
				int start=Integer.parseInt(fespantoks[0]);
				int end=start;
				if(fespantoks.length>1){
					end=Integer.parseInt(fespantoks[1]);
				}
				FrameElement fe=new FrameElement(fename,start,end);
				felist.add(fe);
			}
		}
		try{
			lprob=Double.parseDouble(toks[toks.length-1]);
		}catch(NumberFormatException nfe){
			
		}
		Collections.sort(felist);
		targetIndex=felist.indexOf(Target);
	}
	public void setChoice(){
		for(int j=0;j<felist.size();j++){
			FrameElement fe= felist.get(j);
			int choice = Rerank.get_bit( this.choice, j,FrameElement.NUM_CANDIDATES);
			fe.choice=choice;
		}
	}
}
