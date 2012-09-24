/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * PrintAllRelatedWords.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.evaluation;

import java.util.ArrayList;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import gnu.trove.THashSet;

public class PrintAllRelatedWords {
	public static void main(String[] args) {
		int split = 1;
		String serFile = "/mal2/dipanjan/experiments/FramenetParsing/fndata-1.5/CVSplits/" + split +"/allrelatedwords.ser";
		String txtFile = "/mal2/dipanjan/experiments/FramenetParsing/fndata-1.5/CVSplits/" + split +"/allrelatedwords.txt";
		THashSet<String> set = (THashSet<String>) SerializedObjects.readSerializedObject(serFile);
		ArrayList<String> list = new ArrayList<String>(set);
		ParsePreparation.writeSentencesToTempFile(txtFile, list);
	} 
}
