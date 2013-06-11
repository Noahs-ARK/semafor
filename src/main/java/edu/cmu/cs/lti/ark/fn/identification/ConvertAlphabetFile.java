/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * ConvertAlphabetFile.java is part of SEMAFOR 2.0.
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

import edu.cmu.cs.lti.ark.fn.optimization.LDouble;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Script to combine the alphabet file (containing a map from feature id -> feature name)
 * and a model file (containing learned parameters, one per line, in order) into a model file
 * containing feature name -> learned weight for feature.
 */
public class ConvertAlphabetFile {
	public static void main(String[] args) throws Exception {
		final String alphabetFile = args[0];
		final String modelFile = args[1];
		final String outFile = args[2];

		// read in map from  feature id -> feature name
		final TIntObjectHashMap<String> featureNameById = new TIntObjectHashMap<String>();
		final BufferedReader alphabetReader = new BufferedReader(new FileReader(alphabetFile));
		// first line is the number of features
		final int numFeatures = Integer.parseInt(alphabetReader.readLine());
		String line;
		int count = 1;
		while((line=alphabetReader.readLine()) != null) {
			final String[] toks = line.trim().split("\t");
			featureNameById.put(Integer.parseInt(toks[1]), toks[0]);

		}
		assert numFeatures == count;
		closeQuietly(alphabetReader);

		final BufferedWriter bWriter = new BufferedWriter(new FileWriter(outFile));
		final BufferedReader modelReader = new BufferedReader(new FileReader(modelFile));
		modelReader.readLine(); // ignore first line
		count = 1;
		while((line=modelReader.readLine()) != null) {
			final double val = Double.parseDouble(line.trim());
			final String feat = featureNameById.get(count);
			bWriter.write(feat + "\t" + LDouble.convertToLogDomain(val) + "\n");
			count++;
		}
		closeQuietly(modelReader);
		closeQuietly(bWriter);
	}	
}
