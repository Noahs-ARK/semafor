/*******************************************************************************
 * Copyright (c) 2012 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * CommandLineOptions.java is part of SEMAFOR 2.1.
 * 
 * SEMAFOR 2.1 is free software: you can redistribute it and/or modify  it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * SEMAFOR 2.1 is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. 
 * 
 * You should have received a copy of the GNU General Public License along
 * with SEMAFOR 2.1.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package edu.cmu.cs.lti.ark.fn.parsing;

import com.google.common.collect.Lists;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import gnu.trove.THashMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AD^3
 */
public class JointDecoding extends Decoding {
	private DDDecoding jd = null;
	private double[] w2 = null;
	private boolean ignoreNullSpansWhileJointDecoding;

	public JointDecoding(double[] modelWeights, DDDecoding jd, double[] w2, boolean ignoreNullSpansWhileJointDecoding) {
		super(modelWeights);
		this.jd = jd;
		this.w2 = w2;
		this.ignoreNullSpansWhileJointDecoding = ignoreNullSpansWhileJointDecoding;
	}

	public static JointDecoding fromFile(String modelFile, String alphabetFile, boolean ignoreNullSpansWhileJointDecoding, boolean exact) {
		return new JointDecoding(readModel(modelFile, alphabetFile), new DDDecoding(exact), null, ignoreNullSpansWhileJointDecoding);
	}

	@Override
	public void wrapUp() {
		if (jd != null) {
			jd.end();
		}
	}

	public String getNonOverlappingDecision(FrameFeatures mFF, String frameLine, int offset, boolean returnScores) {
		return getNonOverlappingDecision(mFF, frameLine, offset, modelWeights, false, null, returnScores);
	}

	public ArrayList<String> decodeAll(List<FrameFeatures> frameFeaturesList, List<String> frameLines, int offset, int kBestOutput) {
		int size = frameFeaturesList.size();
		ArrayList<String> result = Lists.newArrayList();
		for(int i = 0; i < size; i ++) {
			System.out.println("Decoding index:"+i);
			String decisionLine = decode(frameFeaturesList, frameLines, i, offset, false);
			result.add(decisionLine);
		}
		return result;
	}

	public String decode(List<FrameFeatures> frameFeaturesList, List<String> frameLines, int index, int offset, boolean returnScores) {
		FrameFeatures f = frameFeaturesList.get(index);
		return getNonOverlappingDecision(f, frameLines.get(index), offset, returnScores);
	}

	public Pair<Map<String, String>, Double> 
			getDecodedMap(FrameFeatures mFF, double[] w, boolean costAugmented, FrameFeatures goldFF) {
		String frameName = mFF.frameName;
		System.out.println("Frame:"+frameName);
		List<SpanAndCorrespondingFeatures[]> featsList = mFF.fElementSpansAndFeatures;
		List<String> frameElements = mFF.fElements;
		int listSize = featsList.size();
		// maps each FE to a list of spans and their corresponding scores
		Map<String,Pair<int[], Double>[]> vs = 
			new THashMap<String,Pair<int[], Double>[]>();
			for(int i = 0; i < listSize; i ++) {
				SpanAndCorrespondingFeatures[] featureArray = featsList.get(i);
				int featArrLen = featureArray.length;
				Pair<int[], Double>[] arr = new Pair[featArrLen];
				double maxProb = -Double.MAX_VALUE;
				String outcome = null;
				for(int j = 0; j < featArrLen; j ++) {
					int[] feats = featureArray[j].features;
					double weightFeatSum = getWeightSum(feats, w);
					if (w2 != null) {
						double secondModelWeight = 0.0;
						weightFeatSum = (1.0 - secondModelWeight) * weightFeatSum +
						(secondModelWeight) * getWeightSum(feats, w2);
					}
					arr[j] = new Pair<int[], Double>(featureArray[j].span, weightFeatSum);
					if (weightFeatSum > maxProb) {
						maxProb = weightFeatSum;
						outcome = featureArray[j].span[0]+"_"+featureArray[j].span[1];
					}
				}			
				// null span is the best span
				if ("-1_-1".equals(outcome)) {
					if (!ignoreNullSpansWhileJointDecoding) {
						vs.put(frameElements.get(i), arr);
					}
				} else {
					vs.put(frameElements.get(i), arr);
				}
				System.out.println("Frame element:"+frameElements.get(i)+" Found span:"+outcome);
			}
			Map<String, Pair<String, Double>> feMap = jd.decode(vs, frameName, costAugmented, goldFF);
			double score = -1.0;
			Map<String, String> retMap = new THashMap<String, String>();
			Set<String> keys = feMap.keySet();
			if (keys.size() > 0) {
				score = 0.0;
				for (String fe: keys) {
					score += feMap.get(fe).second;
					retMap.put(fe, feMap.get(fe).first);
				}
				score /= (double) keys.size();
			}
		return Pair.of(retMap, score);
	}

	public String getNonOverlappingDecision(FrameFeatures mFF,
			String frameLine, 
			int offset, double[] w,
			boolean costAugmented,
			FrameFeatures goldFF,
			boolean returnScores) {
		String frameName = mFF.frameName;
		String decisionLine=getInitialDecisionLine(frameLine, offset);
		if(mFF.fElements.size()==0) {
			decisionLine="0\t1"+"\t"+decisionLine.trim();
			return decisionLine;
		}
		if(mFF.fElements.size()==0) {
			decisionLine="0\t1"+"\t"+decisionLine.trim();
			return decisionLine;
		}
		System.out.println("Frame:"+frameName);
		// vs is the set of FEs on which joint decoding has to be done
		Pair<Map<String, String>, Double> pair = getDecodedMap(mFF, w, costAugmented, goldFF);
		Map<String, String> feMap = pair.first;
		Set<String> keySet = feMap.keySet();
		int count = 1;
		for(String fe:keySet) {
			String outcome = feMap.get(fe);
			if(outcome.equals("-1_-1"))
				continue;
			count++;
			String[] ocToks = outcome.split("_");
			String modToks;
			if(ocToks[0].equals(ocToks[1])) {
				modToks=ocToks[0];
			}
			else
			{
				modToks=ocToks[0]+":"+ocToks[1];
			}
			decisionLine+=fe+"\t"+modToks+"\t";
		}		
		decisionLine="0\t"+count+"\t"+decisionLine.trim();
		if (returnScores) {
			decisionLine = decisionLine + "\t" + pair.second;
		}
		System.out.println(decisionLine);
		return decisionLine;
	}

	public void setMaps(String requiresMap, String excludesMap) throws IOException, ClassNotFoundException {
		Map<String, Set<Pair<String, String>>> exclusionMap = SerializedObjects.readObject(excludesMap);
		Map<String, Set<Pair<String, String>>> requiresMapObj = SerializedObjects.readObject(requiresMap);
		jd.setMaps(exclusionMap, requiresMapObj);
	}
}
