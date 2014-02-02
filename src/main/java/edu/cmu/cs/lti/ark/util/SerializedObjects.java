/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * SerializedObjects.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.util;


import com.google.common.io.InputSupplier;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.apache.commons.io.IOUtils.closeQuietly;


public class SerializedObjects {
	public static void writeSerializedObject(Object object, String outFile) {
		ObjectOutputStream output = null;
		try {
			output = getObjectOutputStream(outFile);
			output.writeObject(object);
		} catch(IOException ex){
			// TODO: NONONONONO! stop swallowing errors!
			ex.printStackTrace();
		} finally{
			closeQuietly(output);
		}
	}

	public static Object readSerializedObject(String inputFile) {
		ObjectInputStream input = null;
		Object recoveredObject = null;
		try{
			input = getObjectInputStream(inputFile);
			recoveredObject = input.readObject();
		} catch(Exception ex) {
			// TODO: NONONONONO! stop swallowing errors!
			ex.printStackTrace();
		} finally{
			closeQuietly(input);
		}
		return recoveredObject;
	}

	public static <T> T readObject(final String inputFile) throws IOException, ClassNotFoundException {
		return SerializedObjects.<T>readObject(new InputSupplier<ObjectInputStream>() {
			@Override public ObjectInputStream getInput() throws IOException {
				return getObjectInputStream(inputFile);
			} });
	}

	@SuppressWarnings("unchecked")
	public static <T> T readObject(InputSupplier<ObjectInputStream> inputSupplier)
			throws IOException, ClassNotFoundException {
		final ObjectInputStream input = inputSupplier.getInput();
		try{
			return (T) input.readObject();
		} finally{
			closeQuietly(input);
		}
	}

	private static ObjectInputStream getObjectInputStream(String inputFile) throws IOException {
		return new ObjectInputStream(new BufferedInputStream(getInputStream(inputFile)));
	}

	public static InputStream getInputStream(String inputFile) throws IOException {
		if(inputFile.endsWith(".gz")) {
			return new GZIPInputStream(new FileInputStream(inputFile));
		} else {
			return new FileInputStream(inputFile);
		}
	}

	private static ObjectOutputStream getObjectOutputStream(String outFile) throws IOException {
		OutputStream file = getOutputStream(outFile);
		return new ObjectOutputStream(new BufferedOutputStream(file));
	}

	public static OutputStream getOutputStream(String outFile) throws IOException {
		if(outFile.endsWith(".gz")) {
			return new GZIPOutputStream(new FileOutputStream(outFile));
		} else {
			return new FileOutputStream(outFile);
		}
	}
}
