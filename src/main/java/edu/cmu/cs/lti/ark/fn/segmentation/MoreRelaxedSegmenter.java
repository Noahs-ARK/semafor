/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * MoreRelaxedSegmenter.java is part of SEMAFOR 2.0.
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

import com.google.common.collect.Lists;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MoreRelaxedSegmenter extends Segmenter {
	public static final int MAX_LEN = 4;

	protected final Set<String> allRelatedWords;

	public MoreRelaxedSegmenter(Set<String> allRelatedWords) {
		this.allRelatedWords = allRelatedWords;
	}

	public List<List<Integer>> getSegmentation(Sentence sentence) {
		String[][] data = sentence.toAllLemmaTagsArray();
		ArrayList<String> startInds = new ArrayList<String>();
		for(int i = 0; i < data[0].length; i ++) {
			startInds.add(""+i);
		}
		final List<List<Integer>> tokNums = Lists.newArrayList();
		for(int i = MAX_LEN; i >= 1; i--) {
			for(int j = 0; j <= (data[0].length-i); j ++) {
				String ind = ""+j;
				if(!startInds.contains(ind))
					continue;
				String lTok = "";
				for(int k = j; k < j + i; k ++) {
					String pos = data[1][k];
					String cPos = pos.substring(0,1);
					String l = data[5][k];    
					lTok+=l+"_"+cPos+" ";
				}
				lTok=lTok.trim();
				if (i > 1) {
					if(allRelatedWords.contains(lTok)) {
						final List<Integer> tokRep = Lists.newArrayList();
						for(int k = j; k < j + i; k ++) {
							tokRep.add(k);
							startInds.remove(""+k);
						}
						tokNums.add(tokRep);
					}
				} else {
					String pos = data[1][j];
					String word = data[0][j];
					if (!pos.equals("NNP") && !containsPunctuation(word)) {
						tokNums.add(Lists.newArrayList(j));
						startInds.remove("" + j);
					} 
				}
			}			
		}
		return trimPrepositions(tokNums, data);
	}	
	
	private boolean containsPunctuation(String word) {
		char first = word.toLowerCase().charAt(0);
		char last = word.toLowerCase().charAt(word.length()-1);
		return !(Character.isLetter(first) && Character.isLetter(last));
	}
}
