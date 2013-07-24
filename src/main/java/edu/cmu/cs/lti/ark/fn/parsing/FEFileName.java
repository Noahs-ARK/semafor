/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * FEFileName.java is part of SEMAFOR 2.0.
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

/**
 * File names listed here are mere examples. i.e. they are not hard coded. they
 * will be read from the command-line argument.
 * */
public class FEFileName {
	public static String feFilename = "lrdata/semeval.fulltrain.sentences.frame.elements";
	public static String eventFilename = "lrdata/parser.train.events.bin";
	public static String alphafilename = null;
	/**
	 * for each line of feature indices, the corresponding span
	 * sentence number , frame name, frame element name. 
	 */
	public static String spanfilename=null;
	public static String candidateFilename="candidate_file_not_in_use";
	public static String tagFilename="lrdata/semeval.fulldev.sentences.lemma.tags";
	public static int startIndex,endIndex;

	public static int KBestParse = 1;
	public static String KBestParseDirectory = null;
}
