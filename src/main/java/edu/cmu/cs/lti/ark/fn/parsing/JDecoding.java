/*******************************************************************************
 * Copyright (c) 2012 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * CommandLineOptions.java is part of SEMAFOR 2.1.
 * 
 * SEMAFOR 2.1 is free software: you can redistribute it and/or modify  it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * SEMAFOR 2.1 is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. 
 * 
 * You should have received a copy of the GNU General Public License along
 * with SEMAFOR 2.1.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package edu.cmu.cs.lti.ark.fn.parsing;

import java.util.Map;
import java.util.Set;

import edu.cmu.cs.lti.ark.util.ds.Pair;

public interface JDecoding {
	public void setMaps(Map<String, Set<Pair<String, String>>> excludesMap, 
			Map<String, Set<Pair<String, String>>> requiresMap);
	public Map<String, Pair<String, Double>> decode(Map<String, Pair<int[], Double>[]> scoreMap, 
			  String frame,
			  boolean costAugmented,
			  FrameFeatures goldFF);
	public void end();
	public void setFactorFile(String factorFile);
	public void setFlag(String flag);
}
