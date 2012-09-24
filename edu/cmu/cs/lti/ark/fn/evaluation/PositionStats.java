/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * PositionStats.java is part of SEMAFOR 2.0.
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

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;

public class PositionStats {
	public static void main(String[] args) {
		String sdir = 
			"/mal2/dipanjan/experiments/FramenetParsing/fndata-1.5/CVSplits";
		String adir = 
			"/mal2/dipanjan/experiments/FramenetParsing/SSFrameStructureExtraction";
		File fdir = new File(adir);
		for (int i = 0; i <= 10; i++) {
			System.out.println("Split: " + i);
			final int j = i;
			String gdir = sdir + "/" + i;
			String fefile = gdir + "/cv.test.sentences.frame.elements";
			String tfile = gdir + "/cv.test.sentences";
			String pfile = gdir + "/cv.test.sentences.all.lemma.tags";
			
			ArrayList<String> tsents = ParsePreparation.readSentencesFromFile(tfile);
			
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.startsWith("temp") && name.contains("_" + j + "_");
				}
			};
			
			String[] folders = fdir.list(filter);
			if (folders.length > 1) {
				System.out.println("Problem with folders: " + j + ". Exiting");
			}
			String autofile = adir + "/" + folders[0] +"/file.predict.frame.elements";
			ArrayList<String> goldfes = ParsePreparation.readSentencesFromFile(fefile);
			ArrayList<String> autofes = ParsePreparation.readSentencesFromFile(autofile);
			if (goldfes.size() != autofes.size()) {
				System.out.println("Size of fes different:" + j + ". Exiting.");
				System.out.println("Gold size:" + goldfes.size());
				System.out.println("Auto size:" + autofes.size());
				System.exit(-1);
			}
			double total = 0.0;
			double correct = 0.0;
			ArrayList<String> goldPs = new ArrayList<String>();
			ArrayList<String> autoPs = new ArrayList<String>();
			for (int k = 0; k < goldfes.size(); k ++) {
				String goldfe = goldfes.get(k);
				String[] goldtoks = goldfe.trim().split("\t");
				String autofe = autofes.get(k);
				String[] autotoks = autofe.trim().split("\t");
				
				String goldframe = goldtoks[1];
				if (goldframe.equals("Change_position_on_a_scale") ||
				    goldframe.equals("Position_on_a_scale") || 
				    goldframe.equals("Cause_change_of_position_on_a_scale")) {
					total += 1.0;
					if (goldframe.equals(autotoks[2])) {
						correct += 1.0;
					}
					goldPs.add("0\t"+goldfe);
					autoPs.add(autofe);
				} else {
					continue;
				}
			}
			ParsePreparation.writeSentencesToTempFile("gold.fes", goldPs);
			ParsePreparation.writeSentencesToTempFile("auto.fes", autoPs);
			PrepareFullAnnotationXML.generateXMLForPrediction("gold.fes", 
					new Range0Based(0,tsents.size(),false), 
					pfile, 
					tfile, 
					gdir + "/gold.position.xml");
			PrepareFullAnnotationXML.generateXMLForPrediction("auto.fes", 
					new Range0Based(0,tsents.size(),false), 
					pfile, 
					tfile, 
					gdir + "/auto.position.xml");
			System.out.println("Total number of position frames:" + total);
			System.out.println("Total number of position frames correct:" + correct);
			System.out.println();
		} 
		
	}
}




