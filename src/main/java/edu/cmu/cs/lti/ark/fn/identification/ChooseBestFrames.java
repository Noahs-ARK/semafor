/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * ChooseBestFrames.java is part of SEMAFOR 2.0.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;

public class ChooseBestFrames {
	public static void main(String[] args) throws IOException {
		String directory = args[0];
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File arg0, String arg1) {
				return arg1.startsWith("file") && arg1.endsWith("frame.elements");
			}
		};
		File f = new File(directory);
		String[] files = f.list(filter);
		Arrays.sort(files, new FileComparator());
		String outFile = directory + "/best.frame.elements";
		BufferedWriter bWriter = new BufferedWriter(new FileWriter(outFile));
		for (String file: files) {
			ArrayList<String> lines = 
				ParsePreparation.readSentencesFromFile(f.getAbsolutePath() + "/" + file);
			String line = lines.get(0).trim();
			String[] oldToks = line.split("\t");
			int sentNum = new Integer(oldToks[6]);
			String luToks = oldToks[4];
			bWriter.write(oldToks[0] + "\t" + oldToks[1] + "\t" + oldToks[3] + "\t" + oldToks[4] + "\t" + oldToks[5] + "\t" + oldToks[6] + "\n");
			for (int i = 1; i < lines.size(); i++) {
				line = lines.get(i).trim();
				oldToks = line.split("\t");
				int newSentNum = new Integer(oldToks[6]);
				String newLuToks = oldToks[4];
				if (newSentNum == sentNum && newLuToks.equals(luToks)) {
					continue;
				} else {
					bWriter.write(oldToks[0] + "\t" + oldToks[1] + "\t" + oldToks[3] + "\t" + oldToks[4] + "\t" + oldToks[5] + "\t" + oldToks[6] + "\n");
					sentNum = newSentNum;
					luToks = newLuToks;
				}
			}
			System.out.println("Finished with file:" + file);
		}
		bWriter.close();
	}
}

class FileComparator implements Comparator<String> {
	public int compare(String arg0, String arg1) {
		int underscoreIndex = arg0.lastIndexOf("_");
		int dotIndex = arg0.lastIndexOf(".frame");
		int endIndex1 = new Integer(arg0.substring(underscoreIndex+1, dotIndex));
		underscoreIndex = arg1.lastIndexOf("_");
		dotIndex = arg1.lastIndexOf(".frame");
		int endIndex2 = new Integer(arg1.substring(underscoreIndex+1, dotIndex));
		if (endIndex1 < endIndex2) 
			return -1;
		else if (endIndex1 == endIndex2) 
			return 0;
		else
			return 1;
	}
}
