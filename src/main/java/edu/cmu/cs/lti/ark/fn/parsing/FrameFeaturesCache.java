/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * FrameFeaturesCache.java is part of SEMAFOR 2.0.
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

import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.util.SerializedObjects;

import java.io.IOException;
import java.util.ArrayList;


public class FrameFeaturesCache {
	public static void main(String[] args) throws IOException {
		FNModelOptions opts = new FNModelOptions(args);
		String eventsFile = opts.eventsFile.get();
		String spanFile = opts.spansFile.get();
		String frFile = opts.trainFrameFile.get();
		LocalFeatureReading lfr = new LocalFeatureReading(eventsFile, spanFile, frFile);
		ArrayList<FrameFeatures> frameFeaturesList = lfr.readLocalFeatures();
		SerializedObjects.writeSerializedObject(frameFeaturesList, opts.frameFeaturesCacheFile.get());
	}	
}
