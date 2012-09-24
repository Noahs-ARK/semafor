/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * CountNumberOfRoles.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.identification;

import java.util.Set;

import edu.cmu.cs.lti.ark.util.SerializedObjects;
import gnu.trove.THashMap;
import gnu.trove.THashSet;


public class CountNumberOfRoles {
	public static void main(String[] args) {
		// String file = "/usr2/dipanjan/experiments/FramenetParsing/fndata-1.5/ACLSplits/5/framenet.frame.element.map";
		String file = "/usr2/dipanjan/experiments/FramenetParsing/FrameStructureExtraction/lrdata/framenet.original.frame.elements.map";
		THashMap<String, THashSet<String>> map = 
			(THashMap<String,THashSet<String>>)SerializedObjects.readSerializedObject(file);
		Set<String> frames = map.keySet();
		System.out.println("Number of frames:" + frames.size());
		THashSet<String> roleSet = new THashSet<String>();
		for (String frame: frames) {
			System.out.println(frame + "\t" + map.get(frame));
			THashSet<String> roles = map.get(frame);
			for (String r: roles) 
				roleSet.add(r);
		}
		System.out.println(roleSet);
		System.out.println("Number of unique roles:" + roleSet.size());
	}
}
