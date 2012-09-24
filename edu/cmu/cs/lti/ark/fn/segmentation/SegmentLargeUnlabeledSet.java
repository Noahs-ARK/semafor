/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * SegmentLargeUnlabeledSet.java is part of SEMAFOR 2.0.
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.StringTokenizer;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.fn.evaluation.ParseUtils;
import edu.cmu.cs.lti.ark.fn.identification.RequiredDataForFrameIdentification;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import gnu.trove.THashSet;

public class SegmentLargeUnlabeledSet {
	public static void main(String[] args) {
		FNModelOptions options = 
			new FNModelOptions(args);
		String inFile = options.trainParseFile.get();
		RequiredDataForFrameIdentification r = 
			(RequiredDataForFrameIdentification)SerializedObjects.readSerializedObject(options.fnIdReqDataFile.get());
		THashSet<String> allRelatedWords = r.getAllRelatedWords();
		THashSet<String> predicateSet = new THashSet<String>();
		try {
			BufferedReader bReader = 
				new BufferedReader(new FileReader(inFile));
			for (int i = 0; i < 1000000; i = i + 1000) {
				ArrayList<String> tokenNums = new ArrayList<String>();
				ArrayList<String> parses = new ArrayList<String>();
				for (int j = i; j < i + 1000; j ++) {
					String line = bReader.readLine();
					line = line.trim();
					parses.add(line);
					tokenNums.add(""+(j-i));
				}
				RoteSegmenter seg = new RoteSegmenter();
				ArrayList<String> segs = seg.findSegmentationForTest(tokenNums, parses, allRelatedWords);
				ArrayList<String> modified = ParseUtils.getRightInputForFrameIdentification(segs);
				Set<String> setOfWords = getSetOfWords(modified, parses);
				predicateSet.addAll(setOfWords);
				MoreRelaxedSegmenter seg2 = new MoreRelaxedSegmenter();
				segs = seg2.findSegmentationForTest(tokenNums, parses, allRelatedWords);
				modified = ParseUtils.getRightInputForFrameIdentification(segs);
				setOfWords = getSetOfWords(modified, parses);
				predicateSet.addAll(setOfWords);
				System.out.println("Done:"+(i+1000));
			}
			bReader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		String[] arr = new String[predicateSet.size()];
		predicateSet.toArray(arr);
		Arrays.sort(arr);
		System.out.println("Size of arr:" + arr.length);
		ArrayList<String> list = new ArrayList<String>();
		for (String a: arr) {
			list.add(a);
		}
		ParsePreparation.writeSentencesToTempFile(options.outputPredicatesFile.get(), list);
	}
	
	public static Set<String> 
		getSetOfWords(ArrayList<String> modSegments, ArrayList<String> parses) {
		Set<String> words = new THashSet<String>();
		for (String seg: modSegments) {
			String[] toks = seg.trim().split("\t");
			int sentNum = new Integer(toks[2]);
			if (toks[1].contains("_"))
				continue;
			int ind = new Integer(toks[1]);
			StringTokenizer st = new StringTokenizer(parses.get(sentNum),"\t");
			int tokensInFirstSent = new Integer(st.nextToken());
			String[][] data = new String[6][tokensInFirstSent];
			for(int k = 0; k < 6; k ++)
			{
				data[k]=new String[tokensInFirstSent];
				for(int j = 0; j < tokensInFirstSent; j ++)
				{
					data[k][j]=""+st.nextToken().trim();
				}
			}
			String word = data[0][ind].toLowerCase();
			words.add(word);
		}
		return words;
	}
}
