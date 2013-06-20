/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * CombineAlphabets.java is part of SEMAFOR 2.0.
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

import edu.cmu.cs.lti.ark.util.BasicFileIO;
import gnu.trove.THashMap;

import java.io.*;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Combines the multiple alphabet files created by AlphabetCreationThreaded into one
 * alphabet file
 */
public class CombineAlphabets {
	public static void main(String[] args) throws IOException {
		String dir = args[0];
		String outFile = args[1];
		File f = new File(dir);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File arg0, String arg1) {
				return arg1.startsWith("alphabet.dat_");
			}
		};
		String[] files = f.list(filter);
		Map<String, Integer> alphabet = new THashMap<String, Integer>();
		for (String file: files) {
			String path = dir + "/" + file;
			Map<String, Integer> temp = 
				readAlphabetFile(path);
			Set<String> keys = temp.keySet();
			for (String key: keys) {
				if (!alphabet.containsKey(key)) {
					alphabet.put(key, alphabet.size() + 1);
				}
			}
		}
		writeAlphabetFile(outFile, alphabet);
	}
	
	private static void writeAlphabetFile(String file, Map<String, Integer> alphabet)
	{
		try
		{
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(file));
			bWriter.write(alphabet.size()+"\n");
			Set<String> set = alphabet.keySet();
			for(String key:set)
			{
				bWriter.write(key+"\t"+alphabet.get(key)+"\n");
			}
			bWriter.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static Map<String, Integer> readAlphabetFile(String file) throws IOException {
		final Map<String, Integer> alphabet = new THashMap<String, Integer>();
		final BufferedReader bReader = BasicFileIO.openFileToRead(file);
		try {
			int num = Integer.parseInt(bReader.readLine());
			for (int i = 0; i < num; i ++) {
				final String line = bReader.readLine().trim();
				final String[] toks = line.split("\t");
				alphabet.put(toks[0], Integer.parseInt(toks[1]));
			}
			return alphabet;
		} finally {
			closeQuietly(bReader);
		}
	}
}

