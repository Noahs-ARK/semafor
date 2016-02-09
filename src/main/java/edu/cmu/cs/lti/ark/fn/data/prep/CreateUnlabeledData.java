/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * CreateUnlabeledData.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.data.prep;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class CreateUnlabeledData {
	public static void main(String[] args) throws IOException {
		String dir = "/mal2/dipanjan/experiments/FramenetParsing/DistributionalSimilarityFeatures/data/pos";
		File f = new File(dir);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File arg0, String arg1) {
				return arg1.startsWith("apw") && arg1.endsWith("pos.txt");
			}			
		};
		String apText = "/mal2/dipanjan/experiments/FramenetParsing/DistributionalSimilarityFeatures/data/pos/AP.txt";
		BufferedWriter bWriter = new BufferedWriter(new FileWriter(apText));
		String[] files = f.list(filter);
		for (final String file1 : files) {
			String file = dir + "/" + file1;
			List<String> sents =
					ParsePreparation.readLines(file);
			for (String sent : sents) {
				sent = sent.trim();
				String[] toks = getTokens(sent);
				if (toks.length > 100)
					continue;
				String outLine = "";
				for (String tok : toks) {
					int li = tok.lastIndexOf("_");
					outLine += tok.subSequence(0, li) + " ";
				}
				outLine = outLine.trim();
				bWriter.write(outLine + "\n");
			}
		}
		bWriter.close();
	}	
	
	public static String[] getTokens(String line) {
		StringTokenizer st = new StringTokenizer(line, " \t", true);
		ArrayList<String> list = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			String tok = st.nextToken().trim();
			if (tok.equals(""))
				continue;
			list.add(tok);
		}
 		String[] arr = new String[list.size()];
		list.toArray(arr);
		return arr;
	}
}
