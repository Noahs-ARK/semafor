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
package edu.cmu.cs.lti.ark.fn.identification.training;

import com.google.common.base.Charsets;
import com.google.common.collect.BiMap;
import com.google.common.io.Files;

import java.io.BufferedWriter;
import java.io.File;

import static edu.cmu.cs.lti.ark.fn.identification.training.AlphabetCreationThreaded.readAlphabetFile;
import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;
import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Script to combine the alphabet file (containing a map from feature id -> feature name)
 * and a model file (containing learned parameters, one per line, in order) into a model file
 * containing feature name -> learned weight for feature.
 */
public class ConvertAlphabetFile {
	// parameters whose abs value are less than or equal to threshold are discarded
	private static final double DEFAULT_THRESHOLD = 0.001;

	public static void main(String[] args) throws Exception {
		final String alphabetFile = args[0];
		final String modelFile = args[1];
		final String outFile = args[2];
		final String featureType = args[3];
		final double threshold = args.length >= 5 ? Double.parseDouble(args[4].trim()) : DEFAULT_THRESHOLD;

		// read in map from feature id -> feature name
		final BiMap<Integer, String> featureNameById = readAlphabetFile(new File(alphabetFile)).inverse();
		// read in parameter values
		final double[] parameters = TrainBatch.loadModel(modelFile);
		// write out list of (feature name, feature value) pairs
		final BufferedWriter output = Files.newWriter(new File(outFile), Charsets.UTF_8);
		try {
			output.write(String.format("%s\n", featureType));
			for (int i : xrange(parameters.length)) {
				final double val = parameters[i];
				if (Math.abs(val) <= threshold) continue;
				output.write(String.format("%s\t%s\n", featureNameById.get(i), val));
			}
		} finally {
			closeQuietly(output);
		}
	}
}
