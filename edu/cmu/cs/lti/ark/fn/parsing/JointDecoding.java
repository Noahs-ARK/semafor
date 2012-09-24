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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;
import java.util.Set;
import java.util.Map;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.util.FileUtil;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import gnu.trove.THashMap;

public class JointDecoding extends Decoding {

	private boolean mIgnoreNullSpansWhileJointDecoding;
	private DDDecoding jd = null;
	private double[] w2 = null;
	private double secondModelWeight = 0.0;
	public static final String ILP_DECODING = "ilp";
	public static final String DD_DECODING = "dd";
	public static final String FILE_DECODING_ADMM = "file_admm";
	public static final String FILE_DECODING_LP = "file_lp";
	public static final String FILE_DECODING_ILP = "file_ilp";
	public static final String FILE_DECODING_ADMMILP = "file_admmilp";
	
	private int mNumThreads = 1;
	protected String mFactorFile = null;

	public JointDecoding(boolean exact) {
		jd = new DDDecoding(exact);
		mIgnoreNullSpansWhileJointDecoding = false;
	}

	public void wrapUp() {
		if (jd != null) {
			jd.end();
		}
	}

	public void init(String modelFile, 
			String alphabetFile,
			String predictionFile,
			ArrayList<FrameFeatures> list,
			ArrayList<String> frameLines)
	{
		super.init(modelFile, alphabetFile, predictionFile, list, frameLines);
		mIgnoreNullSpansWhileJointDecoding = false;
	}

	public void setFactorsFile(String factorsFile) {
		mFactorFile = factorsFile;
		jd.setFactorFile(mFactorFile);
	}
	
	public void init(String modelFile, 
			String alphabetFile,
			String predictionFile,
			ArrayList<FrameFeatures> list,
			ArrayList<String> frameLines,
			boolean ignoreNullSpansWhileJointDecoding,
			int numThreads)
	{
		super.init(modelFile, alphabetFile, predictionFile, list, frameLines);
		mIgnoreNullSpansWhileJointDecoding = ignoreNullSpansWhileJointDecoding;
		mNumThreads = numThreads;
	}	

	public void init(String modelFile, String alphabetFile) {
		super.init(modelFile, alphabetFile);
		mIgnoreNullSpansWhileJointDecoding = false;
	}

	public void setSecondModel(String secondModelFile, double weight) {
		System.out.println("Setting second model from: " + secondModelFile);
		w2 = new double[numLocalFeatures];
		Scanner paramsc = FileUtil.openInFile(secondModelFile);
		for (int i = 0; i < numLocalFeatures; i++) {
			double val = Double.parseDouble(paramsc.nextLine());
			w2[i] = val;
		}
		paramsc.close();
		secondModelWeight = weight;
		System.out.println("Interpolation weight: " + secondModelWeight);
	}

	public String getNonOverlappingDecision(FrameFeatures mFF, 
			String frameLine, 
			int offset,
			boolean costAugmented,
			FrameFeatures goldFF) {
		return getNonOverlappingDecision(
				mFF, 
				frameLine, 
				offset, 
				localW,
				costAugmented,
				goldFF,
				false);
	}

	public String getNonOverlappingDecision(FrameFeatures mFF, 
			String frameLine, 
			int offset, 
			boolean returnScores) {
		return getNonOverlappingDecision(
				mFF, 
				frameLine, 
				offset, 
				localW,
				false,
				null,
				returnScores);
	}

	public ArrayList<String> decodeAll(String overlapCheck, 
			int offset,
			boolean returnScores) {
		int size = mFrameList.size();
		ArrayList<String> result = new ArrayList<String>();
		for(int i = 0; i < size; i ++)
		{
			System.out.println("Decoding index:"+i);
			String decisionLine = decode(i,overlapCheck, offset, returnScores);
			result.add(decisionLine);
		}
		if (mPredictionFile != null) {
			ParsePreparation.writeSentencesToTempFile(mPredictionFile, result);
		}
		return result;
	}
	
	// does not return scores
	public ArrayList<String> decodeAll(String overlapCheck, 
			int offset) {
		int size = mFrameList.size();
		ArrayList<String> result = new ArrayList<String>();
		for(int i = 0; i < size; i ++)
		{
			System.out.println("Decoding index:"+i);
			String decisionLine = decode(i,overlapCheck, offset, false);
			result.add(decisionLine);
		}
		if (mPredictionFile != null) {
			ParsePreparation.writeSentencesToTempFile(mPredictionFile, result);
		}
		return result;
	}

	public String decode(int index, String overlapCheck, int offset, boolean returnScores)
	{
		FrameFeatures f = mFrameList.get(index);
		String dec = null;
		if(overlapCheck.equals("overlapcheck"))
			dec = getNonOverlappingDecision(f,mFrameLines.get(index), offset, returnScores);
		else
			dec = getDecision(f,mFrameLines.get(index), offset);
		return dec;
	}

	public Pair<Map<String, String>, Double> 
	getDecodedMap(FrameFeatures mFF, 
			String frameLine, 
			int offset, double[] w,
			boolean costAugmented,
			FrameFeatures goldFF) {
		String frameName = mFF.frameName;
		System.out.println("Frame:"+frameName);
		ArrayList<SpanAndCorrespondingFeatures[]> featsList = mFF.fElementSpansAndFeatures;
		ArrayList<String> frameElements = mFF.fElements;
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
				if (outcome.equals("-1_-1")) {
					if (!mIgnoreNullSpansWhileJointDecoding) {
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
					score += feMap.get(fe).getSecond();
					retMap.put(fe, feMap.get(fe).getFirst());
				}
				score /= (double) keys.size();
			}			
			Pair<Map<String, String>, Double> ret = 
				new Pair<Map<String, String>, Double>(retMap, score);
			return ret;
	}

	public Pair<Map<String, String>, Double> getNonOverlappingDecision(FrameFeatures mFF, 
			String frameLine, 
			int offset, double[] w,
			boolean returnMap,
			boolean costAugmented,
			FrameFeatures goldFF,
			boolean returnScores) {
		Map<String, String> feMap = new THashMap<String, String>();
		if(mFF.fElements.size()==0) {
			Pair<Map<String, String>, Double> p = new Pair<Map<String, String>, Double>(feMap, -1.0);
			return p;
		}
		if(mFF.fElements.size()==0) {
			Pair<Map<String, String>, Double> p = new Pair<Map<String, String>, Double>(feMap, -1.0);
			return p;
		}
		// vs is the set of FEs on which joint decoding has to be done
		Pair<Map<String, String>, Double> pair = getDecodedMap(mFF, frameLine, offset, w, costAugmented, goldFF);
		return pair;
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
		Pair<Map<String, String>, Double> pair = getDecodedMap(mFF, frameLine, offset, w, costAugmented, goldFF);
		Map<String, String> feMap = pair.getFirst();
		Set<String> keySet = feMap.keySet();
		int count = 1;
		for(String fe:keySet) {
			String outcome = feMap.get(fe);
			if(outcome.equals("-1_-1"))
				continue;
			count++;
			String[] ocToks = outcome.split("_");
			String modToks;
			if(ocToks[0].equals(ocToks[1]))
			{
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
			decisionLine = decisionLine + "\t" + pair.getSecond();
		}
		System.out.println(decisionLine);
		return decisionLine;
	}

	public void setMaps(String requiresMap, String excludesMap) {
		Map<String, Set<Pair<String, String>>> exclusionMap = 
			(Map<String, Set<Pair<String, String>>>) SerializedObjects.readSerializedObject(excludesMap);
		Map<String, Set<Pair<String, String>>> requiresMapObj = 
			(Map<String, Set<Pair<String, String>>>) SerializedObjects.readSerializedObject(requiresMap);
		jd.setMaps(exclusionMap, requiresMapObj);
	}
}


class WeightComparator implements Comparator<Pair<int[], Double>> {
	public int compare(Pair<int[], Double> o1, Pair<int[], Double> o2) {
		if (o1.getSecond() > o2.getSecond()) {
			return -1;
		} else if (o1.getSecond() == o1.getSecond()) {
			return 0;
		} else {
			return 1;
		}
	}
}