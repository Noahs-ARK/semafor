/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * TrainingBatchMain.java is part of SEMAFOR 2.0.
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

import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.util.SerializedObjects;

import java.io.IOException;
import java.util.ArrayList;

public class TrainingBatchMain
{
	/**
	 *
	 * @param args command-line arguments as follows:
	 *             frameFeaturesCacheFile: path to file containing a serialized cache of all of the features
	 *                 extracted from the training data
	 *             alphabetFile: path to file containing the alphabet
	 *             trainFrameFile: path to file containing ?
	 *             reg: type of regularization to use (ignored?)
	 *             lambda: regularization hyperparameter
	 *             binaryOverlapConstraint: ?
	 *             numThreads: the number of parallel threads to run while optimizing
	 *             modelFile: path to output file to write resulting model to. intermediate models will be written to
	 *                 modelFile + "_" + i
	 */
	public static void main(String[] args) throws IOException {
		FNModelOptions opts = new FNModelOptions(args);
		String modelFile = opts.modelFile.get();
		String alphabetFile = opts.alphabetFile.get();
		String frameFeaturesCacheFile = opts.frameFeaturesCacheFile.get();
		String frFile = opts.trainFrameFile.get();
		String reg = opts.reg.get();
		double lambda = opts.lambda.get();
		int numThreads = opts.numThreads.get();
		String binaryFactorPresent = opts.binaryOverlapConstraint.get();
		ArrayList<FrameFeatures> list = (ArrayList<FrameFeatures>)SerializedObjects.readSerializedObject(frameFeaturesCacheFile);
		Training bpt = new Training();
		bpt.init(modelFile, alphabetFile, list, frFile, reg, lambda, numThreads);
		bpt.trainBatch();
	}	
}
