/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * CustomOptions.java is part of SEMAFOR 2.0.
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

import java.util.HashMap;

public class CustomOptions {
	private HashMap<String ,String>opts;
	public CustomOptions(String args[]){
		opts=new HashMap<String, String>();
		for(String opt:args){
			String toks[]=opt.split(":");
			if(toks.length>1){
				opts.put(toks[0], toks[1]);
			}
			else if(toks.length>0){
				opts.put(toks[0], "\t");
			}
		}
	}
	public String get(String optionName){
		if(!opts.containsKey(optionName)){
			System.err.println("option not present:"+optionName);
			return "";
		}
		return opts.get(optionName);
	}
	public boolean isPresent(String optionName){
		return opts.containsKey(optionName);
	}
}
