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

/**
 * A map from frames to their frame elements
 */
public class FEDict {
	private THashMap<String, THashSet<String>> frameElementsForFrame;

	public FEDict(THashMap<String, THashSet<String>> frameElementsForFrame){
		this.frameElementsForFrame = frameElementsForFrame;
	}

	/**
	 * Initialize from a serialized HashMap
	 * @param dictFilename the path to the file containing the serialized HashMap
	 */
	public FEDict(String dictFilename) throws LoadingException {
		try {
			frameElementsForFrame = SerializedObjects.readObject(dictFilename);
		} catch (Exception e) {
			throw new LoadingException(e);
		}
	}

	/**
	 * Get the frame elements for the given frame
	 * @param frame the frame to look up
	 * @return an array of frame elements of `frame`
	 */
	public String [] lookupFrameElements(String frame){
		THashSet<String> frameElements = frameElementsForFrame.get(frame);
		if(frameElements == null) return new String[0];
		return frameElements.toArray(new String[frameElements.size()]);
	}

	public static class LoadingException extends Exception {
		public LoadingException(Exception e) { super(e); }
	}
}
