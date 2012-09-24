/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * BasicFileIO.java is part of SEMAFOR 2.0.
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.logging.Logger;

public class BasicFileIO {

	/*
	 * A logger for the class.
	 */
	private static Logger log = Logger.getLogger(BasicFileIO.class.getCanonicalName());

	public static BufferedReader openFileToRead(String file) {
		try {
			BufferedReader bReader = null;
			if (file.endsWith(".gz")) {
				bReader = new BufferedReader(new InputStreamReader(
						new GZIPInputStream(new FileInputStream(file))));
			} else {
				bReader = new BufferedReader(new FileReader(file));
			}
			return bReader;
		} catch (IOException e) {
			e.printStackTrace();
			log.severe("Could not open file:" + file);
			System.exit(-1);
		}
		return null;
	}

	public static BufferedWriter openFileToWrite(String file) {
		try {
			BufferedWriter bWriter = null;
			if (file.endsWith(".gz")) {
				bWriter = new BufferedWriter(new OutputStreamWriter(
						new GZIPOutputStream(new FileOutputStream(file))));
			} else {
				bWriter = new BufferedWriter(new FileWriter(file));
			}
			return bWriter;
		} catch (IOException e) {
			e.printStackTrace();
			log.severe("Could not open file for writing:" + file);
			System.exit(-1);
		}
		return null;
	}

	public static void closeFileAlreadyRead(BufferedReader bReader) {
		try {
			bReader.close();
		} catch (IOException e) {
			e.printStackTrace();
			log.severe("Could not close file.");
			System.exit(-1);
		}
	}	

	public static void closeFileAlreadyWritten(BufferedWriter bWriter) {
		try {
			bWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			log.severe("Could not close file.");
			System.exit(-1);
		}
	}	

	public static String getLine(BufferedReader bReader) {
		try {
			String line = bReader.readLine();
			return line;
		} catch(IOException e) {
			e.printStackTrace();
			log.severe("Could not read line from file.");
			System.exit(-1);
		}
		return null;
	}

	public static void writeLine(BufferedWriter bWriter, String line) {
		try {
			bWriter.write(line + "\n");
		} catch(IOException e) {
			e.printStackTrace();
			log.severe("Could not write line to file.");
			System.exit(-1);
		}
	}
}
