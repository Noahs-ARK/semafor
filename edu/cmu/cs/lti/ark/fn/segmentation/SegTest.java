/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * SegTest.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.segmentation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.lti.ark.fn.identification.RequiredDataForFrameIdentification;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import gnu.trove.THashMap;
import gnu.trove.THashSet;

public class SegTest {
	public static void main(String[] args) {
		String parseFile = "lrdata/semeval.fulltrain.sentences.lemma.tags";
		String tokFile = "lrdata/semeval.fulltrain.sentences";
		ArrayList<String> tokenNums = new ArrayList<String>();
		ArrayList<String> orgSentenceLines = new ArrayList<String>();
		ArrayList<String> originalIndices = new ArrayList<String>();
		ArrayList<String> parses = new ArrayList<String>();
		int count = 0;
		int start = 0;
		int end = 10;
		try
		{
			BufferedReader inParses = new BufferedReader(new FileReader(parseFile));
			BufferedReader inOrgSentences = new BufferedReader(new FileReader(tokFile));
			String line = null;
			int dummy = 0;
			while((line=inParses.readLine())!=null)
			{
				String line2 = inOrgSentences.readLine().trim();
				if(count<start)	// skip sentences prior to the specified range
				{
					count++;
					continue;
				}
				parses.add(line.trim());
				orgSentenceLines.add(line2);
				tokenNums.add(""+dummy);	// ?? I think 'dummy' is just the offset of the sentence relative to options.startIndex
				originalIndices.add(""+count);
				if(count==(end-1))	// skip sentences after the specified range
					break;
				count++;
				dummy++;
			}				
			inParses.close();
			inOrgSentences.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		RequiredDataForFrameIdentification r = 
			(RequiredDataForFrameIdentification)SerializedObjects.readSerializedObject("lrdata/reqData.jobj");
		THashSet<String> allRelatedWords = r.getAllRelatedWords();		
		MoreRelaxedSegmenter seg = new MoreRelaxedSegmenter();
		ArrayList<String> segs = seg.findSegmentationForTest(tokenNums, parses, allRelatedWords);
	}
}
