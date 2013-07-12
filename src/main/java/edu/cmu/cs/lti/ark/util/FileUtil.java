/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * FileUtil.java is part of SEMAFOR 2.0.
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
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.common.io.LineProcessor;

import java.util.*;
import java.io.*;
public class FileUtil {
	public static final LineProcessor<Integer> LINE_COUNTER = new LineProcessor<Integer>() {
		int lineCount = 0;
		@Override public boolean processLine(String line) throws IOException {
			lineCount++;
			return true;
		}
		@Override public Integer getResult() {
			return lineCount;
		} };

	public static Scanner openInFile(String filename){
		Scanner localsc=null;
		try
		{
			localsc=new Scanner (new FileInputStream(filename));

		}catch(IOException ioe){
			// TODO: NO! stop swallowing exceptions
			System.out.println(ioe.getMessage());
		}
		return localsc;
	}
	public static PrintStream  openOutFile(String filename){
		PrintStream localps=null;
		try
		{
			localps=new PrintStream (new FileOutputStream(filename));

		}catch(IOException ioe){
			// TODO: NO! stop swallowing exceptions
			System.out.println(ioe.getMessage());
		}
		return localps;
	}

	public static int countLines(String filename) throws IOException {
		return countLines(Files.newReaderSupplier(new File(filename), Charsets.UTF_8));
	}

	public static int countLines(InputSupplier<InputStreamReader> inputSupplier) throws IOException {
		return CharStreams.readLines(inputSupplier, LINE_COUNTER);
	}
}
