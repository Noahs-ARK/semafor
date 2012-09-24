/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * FrameElement.java is part of SEMAFOR 2.0.
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

public class FrameElement implements Comparable<FrameElement> {
	public String n;
	//public int s,e;
	public int choice;
	/**@brief number of candidate spans per frame element
	 */
	public static final int NUM_CANDIDATES=3;
	//candidates picked by making independent decisions
	public int canstart[],canend[];
	public double lprob[];
	public FrameElement(String name, int start, int end){
		n=name;
		canstart=new int [1];
		canend=new int [1];
		canstart[0]=start;
		canend[0]=end;
		choice=0;
	}
	public FrameElement(String name, int start[], int end[],double logProb[]){
		n=name;
		canstart=start;
		canend=end;
		lprob=logProb;

	}
	public FrameElement(String name){
		n=name;
		canstart=new int [1];
		canend=new int [1];
		canstart[0]=-1;
		canend[0]=-1;
		choice=0;
	}

	public String toString(){
		if(choice>=canstart.length || choice<0)return "";
		if(canstart[choice]==-1||canend[choice]==-1)return "";
		if(canstart[choice]==canend[choice])return n+"\t"+canstart[choice] ;
		return n+"\t"+canstart[choice]+":"+canend[choice];
		
	}
	public boolean present(){
		if(canstart[choice]==-1||canend[choice]==-1)return false;
		return true;
	}
	public double getProb(){
		return lprob[choice];
	}
	public int getStart(){
		return canstart[choice];
	}
	public int getEnd(){
		return canend[choice];
	}
	public int compareTo(FrameElement fe){
		if(fe.getStart()>getStart())return -1;
		if(fe.getStart()<getStart())return 1;
		return 0;
	}
	
	public boolean equals(Object o){
		if(! (o instanceof FrameElement))return false;
		FrameElement fe=(FrameElement)o;
		//boolean ret =n.equals(fe.n) && getStart()==fe.getStart() && getEnd()==fe.getEnd();
		return n.equals(fe.n) && getStart()==fe.getStart() && getEnd()==fe.getEnd();
	}
	public int hashCode(){
		return n.hashCode()+(getStart()<<16)+getEnd();
	}
	
}
