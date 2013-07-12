/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * ParseOptions.java is part of SEMAFOR 2.0.
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

/**
 * Processes command-line options and stores a variety of configuration parameters. 
 * Options should be formatted as argname:value (or simply the argname if boolean).
 * See the code for details.
 * 
 * @author dipanjan
 *
 */
public final class ParseOptions {
	public String frameNetMapFile = null;
	public String wnConfigFile = null;
	public String stopWordsFile = null;
	
	public String testParseFile = null;
	public String testTokenizedFile = null;
	public String testFEPredictionsFile = null;
	public String segParamFile = null;
	public String idParamFile = null;
	public String allRelatedWordsFile = null;
	public String wnRelatedWordsForWordsFile = null;
	public String wnMapFile = null;
	public String wnHiddenWordsCacheFile = null;
	public String hvCorrespondenceFile = null;
	public String goldFrameTokenFile = null;
	public String tempFile = null;
	public String frameElementsFile = null;
	
	public int startIndex = -1;
	public int endIndex = -1;
	
	public String outputFile = null;
	
	public ParseOptions(String[] args) {
		for (String arg : args) {
			System.out.println(arg);
			String[] pair = new String[]{
					arg.substring(0, arg.indexOf(':')),
					arg.substring(arg.indexOf(':') + 1),
			};
			if (pair[0].equals("frameNetMapFile")) {
				frameNetMapFile = pair[1].trim();
			} else if (pair[0].equals("wnConfigFile")) {
				wnConfigFile = pair[1].trim();
			} else if (pair[0].equals("stopWordsFile")) {
				stopWordsFile = pair[1].trim();
			} else if (pair[0].equals("testParseFile")) {
				testParseFile = pair[1].trim();
			} else if (pair[0].equals("testTokenizedFile")) {
				testTokenizedFile = pair[1].trim();
			} else if (pair[0].equals("testFEPredictionsFile")) {
				testFEPredictionsFile = pair[1].trim();
			} else if (pair[0].equals("segParamFile")) {
				segParamFile = pair[1].trim();
			} else if (pair[0].equals("idParamFile")) {
				idParamFile = pair[1].trim();
			} else if (pair[0].equals("allRelatedWordsFile")) {
				allRelatedWordsFile = pair[1].trim();
			} else if (pair[0].equals("startIndex")) {
				startIndex = Integer.parseInt(pair[1].trim());
			} else if (pair[0].equals("endIndex")) {
				endIndex = Integer.parseInt(pair[1].trim());
			} else if (pair[0].equals("outputFile")) {
				outputFile = pair[1].trim();
			} else if (pair[0].equals("wnRelatedWordsForWordsFile")) {
				wnRelatedWordsForWordsFile = pair[1].trim();
			} else if (pair[0].equals("wnMapFile")) {
				wnMapFile = pair[1].trim();
			} else if (pair[0].equals("wnHiddenWordsCacheFile")) {
				wnHiddenWordsCacheFile = pair[1].trim();
			} else if (pair[0].equals("hvCorrespondenceFile")) {
				hvCorrespondenceFile = pair[1].trim();
			} else if (pair[0].equals("goldFrameTokenFile")) {
				goldFrameTokenFile = pair[1].trim();
			} else if (pair[0].equals("tempfile")) {
				tempFile = pair[1].trim();
			} else if (pair[0].equals("framelementsfile")) {
				frameElementsFile = pair[1].trim();
			}
		}
	}	
}
