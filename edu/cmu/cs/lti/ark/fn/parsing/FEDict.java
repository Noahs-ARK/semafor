/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * FEDict.java is part of SEMAFOR 2.0.
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

import edu.cmu.cs.lti.ark.util.SerializedObjects;
import gnu.trove.THashMap;
import gnu.trove.THashSet;

public class FEDict {
	//public static String dirname="frame";
	public static String outdictFilename="data/fedict.train";

	private THashMap<String,THashSet<String>>fedict;
	public FEDict(String dictFilename){
		fedict=(THashMap<String,THashSet<String>>)SerializedObjects.readSerializedObject(dictFilename);
	}
	public String [] lookupFes(String frame){
		THashSet<String> feSet=fedict.get(frame);
		if(feSet==null)return new String[0];
		String [] ret=new String[ feSet.size()];
		int count=0;
		for(String fe:feSet){
			ret[count]=fe;
			count++;
		}
		
		return ret;
	}
	public void merge(String filename){
		THashMap<String,THashSet<String>> tempdict=(THashMap<String,THashSet<String>>)SerializedObjects.readSerializedObject(filename);
		for(String key : tempdict.keySet()){
			THashSet newval=tempdict.get(key);
			if(fedict.contains(key)){
				THashSet val=fedict.get(key);
				val.addAll(newval);
			}
			else{
				fedict.put(key, newval);
			}
		}
		fedict.putAll(tempdict);
	}
}
