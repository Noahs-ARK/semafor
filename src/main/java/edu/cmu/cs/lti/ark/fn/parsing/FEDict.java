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

import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import gnu.trove.THashSet;

import java.io.*;
import java.util.Map;

/**
 * A map from frames to their frame elements
 */
public class FEDict {
	public static final String DEFAULT_FILENAME = "framenet.frame.element.map";

	private Map<String, THashSet<String>> frameElementsForFrame;

	public static class LoadingException extends IOException {
		public LoadingException(Exception e) { super(e); }
	}

	private static class SingletonHolder {
		private static final FEDict INSTANCE;
		static {
			try {
				INSTANCE = new FEDict(readInput(new InputSupplier<InputStream>() {
					@Override
					public InputStream getInput() throws IOException {
						return getClass().getClassLoader().getResourceAsStream(DEFAULT_FILENAME);
					}
				}));
			} catch (LoadingException e) { throw new RuntimeException(e); }
		}
	}

	public FEDict(Map<String, THashSet<String>> frameElementsForFrame){
		this.frameElementsForFrame = frameElementsForFrame;
	}

	/**
	 * Lazy-loading singleton
	 */
	public static FEDict getInstance() {
		return SingletonHolder.INSTANCE;
	}

	/**
	 * Initialize from a serialized HashMap
	 * @param dictFilename the path to the file containing the serialized HashMap
	 */
	public static FEDict fromFile(String dictFilename) throws LoadingException {
		return new FEDict(readInput(Files.newInputStreamSupplier(new File(dictFilename))));
	}

	private static Map<String, THashSet<String>> readInput(final InputSupplier<? extends InputStream> inputSupplier)
			throws LoadingException {
		try {
			return SerializedObjects.readObject(new InputSupplier<ObjectInputStream>() {
				@Override public ObjectInputStream getInput() throws IOException {
					return new ObjectInputStream(new BufferedInputStream(inputSupplier.getInput()));
				} });
		} catch (Exception e) { throw new LoadingException(e); }
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
}
