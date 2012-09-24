/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * Decoding.java is part of SEMAFOR 2.0.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.util.FileUtil;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import edu.cmu.cs.lti.ark.fn.optimization.*;

public class Decoding
{
	protected int numLocalFeatures;
	protected double[] localW;
	private String mLocalAlphabetFile;
	private String mLocalModelFile;
	public static long count=0;
	protected ArrayList<FrameFeatures> mFrameList;
	protected String mPredictionFile;	
	protected ArrayList<String> mFrameLines; 
	
	public Decoding()
	{
		
	}
	
	public void init(String modelFile, 
					 String alphabetFile,
					 String predictionFile,
					 ArrayList<FrameFeatures> list,
					 ArrayList<String> frameLines)
	{
		mLocalModelFile = modelFile;
		mLocalAlphabetFile = alphabetFile;
		readModel();
		mFrameList=list;
		mPredictionFile = predictionFile;
		mFrameLines=frameLines;
	}
	
	
	public void init(String modelFile, String alphabetFile) {
		mLocalModelFile = modelFile;
		mLocalAlphabetFile = alphabetFile;
		readModel();
	}
	
	public void setData(String predictionFile,
					 ArrayList<FrameFeatures> list,
					 ArrayList<String> frameLines) {
		mFrameList=list;
		mPredictionFile = predictionFile;
		mFrameLines=frameLines;
	}
	
	public void readModel() {	
		Scanner localsc = FileUtil.openInFile(mLocalAlphabetFile);
		Scanner paramsc = FileUtil.openInFile(mLocalModelFile);
		numLocalFeatures = localsc.nextInt() + 1;
		localsc.close();
		localW = new double[numLocalFeatures];
		for (int i = 0; i < numLocalFeatures; i++) {
			double val = Double.parseDouble(paramsc.nextLine());
			localW[i] = val;
		}
	}
	
	public ArrayList<String> decodeAll(String overlapCheck, 
									   int offset)
	{
		int size = mFrameList.size();
		ArrayList<String> result = new ArrayList<String>();
		for(int i = 0; i < size; i ++)
		{
			System.out.println("Decoding index:"+i);
			String decisionLine = decode(i,overlapCheck, offset);
			result.add(decisionLine);
		}
		if (mPredictionFile != null) {
			ParsePreparation.writeSentencesToTempFile(mPredictionFile, result);
		}
		return result;
	}
	
	public String decode(int index, String overlapCheck, int offset)
	{
		FrameFeatures f = mFrameList.get(index);
		String dec = null;
		if(overlapCheck.equals("overlapcheck"))
			dec = getNonOverlappingDecision(f,mFrameLines.get(index), offset);
		else
			dec = getDecision(f,mFrameLines.get(index), offset);
		return dec;
	}
	
	public double getWeightSum(int[] feats, double[] w)
	{
		double weightSum = w[0];
		for (int k = 0; k < feats.length; k++)
		{
			if(feats[k]!=0)
			{
				weightSum+=w[feats[k]];
			}
		}
		return weightSum;
	}
	
	public String getDecision(FrameFeatures mFF,String frameLine, int offset)
	{
		String frameName = mFF.frameName;
		System.out.println("Frame:"+frameName);
		String decisionLine=getInitialDecisionLine(frameLine, offset);
		if(mFF.fElements.size()==0)
		{
			decisionLine="0\t1"+"\t"+decisionLine.trim();
			return decisionLine;
		}
		int count = 1;
		ArrayList<SpanAndCorrespondingFeatures[]> featsList = mFF.fElementSpansAndFeatures;
		ArrayList<String> frameElements = mFF.fElements;
		int listSize = featsList.size();
		for(int i = 0; i < listSize; i ++)
		{
			SpanAndCorrespondingFeatures[] featureArray = featsList.get(i);
			int featArrLen = featureArray.length;
			int maxIndex = -1;
			double maxSum = -Double.MAX_VALUE;
			for(int j = 0; j < featArrLen; j ++)
			{
				int[] feats = featureArray[j].features;
				double weightSum=getWeightSum(feats, localW);
				double expVal = Math.exp(weightSum);
				if(expVal>maxSum)
				{
					maxSum = expVal;
					maxIndex=j;
				}
			}
			String maxSpan=featureArray[maxIndex].span[0]+"_"+featureArray[maxIndex].span[1];		
			String fe = frameElements.get(i);
			System.out.println("Frame element:"+fe+" Found span:"+maxSpan);
			if(maxSpan.equals("-1_-1"))
				continue;
			count++;
			String[] ocToks = maxSpan.split("_");
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
		return decisionLine;
	}
	
	public String getInitialDecisionLine(String frameLine, int offset)
	{
		String[] frameToks = frameLine.split("\t");
		String decisionLine="";
		for(int i = 1; i <= 5; i ++)
		{
			String tok = frameToks[i];
			if (i == 5) {
				int num = new Integer(tok);
				num = num + offset;
				tok = ""+num;
			}
			decisionLine+=tok+"\t";
		}	
		return decisionLine;
	}
	
	public static boolean pairwiseOverlap(String one, String two)
	{
		if(one.equals("-1_-1"))
			return false;
		if(two.equals("-1_-1"))
			return false;
		String[] toks = one.split("_");
		int oneStart = new Integer(toks[0]);
		int oneEnd = new Integer(toks[toks.length-1]);
		
		toks = two.split("_");
		int twoStart = new Integer(toks[0]);
		int twoEnd = new Integer(toks[toks.length-1]);
		
		if(oneStart<twoStart)
		{
			if(oneEnd<twoStart)
			{
				return false;
			}
			else
			{
				return true;
			}
		}
		else
		{
			if(twoEnd<oneStart)
			{
				return false;
			}
			else
			{
				return true;
			}
		}
		
	}
	
	public static THashMap<String,String> getOnlyOverlappingFes(THashMap<String,String> map)
	{
		Set<String> keys = map.keySet();
		THashMap<String,String> tempMap = new THashMap<String,String>();
		for(String fe: keys)
		{
			String span = map.get(fe);
			if(!span.equals("-1_-1"))
			{
				tempMap.put(fe, span);
			}
		}	
		return tempMap;
	}	
	
	public static boolean isThereOverlap(String spans,THashSet<String> seenSpans)
	{
		String[] toks = spans.split(":");
		for(int i = 0; i < toks.length-1; i ++)
		{
			for(int j = i +1; j < toks.length; j ++)
			{
				if(pairwiseOverlap(toks[i],toks[j]))
					return true;
				if(seenSpans.contains(toks[i])||seenSpans.contains(toks[j]))
					return true;
			}
		}
		return false;
	}
	
	public static THashMap<String,String> getCubePruningDecoding(THashMap<String,String> oMap, 
																 ArrayList<String> feList, 
																 THashMap<String,THashMap<String,LDouble>> vs, 
																 int k, 
																 THashSet<String> seenSpans)
	{
		THashMap<String,String> result = new THashMap<String,String>();
		if(oMap.size()==0)
			return result;
		String[] fes = new String[oMap.size()];
		int[] vIndex = new int[oMap.size()];
		int count = 0;
		int i = 0;
		String[] vKeys = new String[vs.size()];
		vs.keySet().toArray(vKeys);
		for(String v:vKeys)
		{
			String fe = v;
			if(!oMap.contains(fe))
			{
				i++;
				continue;
			}
			fes[count]=""+fe;
			vIndex[count]=i;
			i++;
			count++;
		}
		Comparator<Pair<String,LDouble>> comp = new Comparator<Pair<String,LDouble>>()
		{
			public int compare(Pair<String, LDouble> o1, Pair<String, LDouble> o2)
			{
				int check = isGreater(o1.getSecond(), o2.getSecond());
				if(check==1)
					return -1;
				else if(check==0)
					return 0;
				else 
					return 1;
			}
		};
		ArrayList<Pair<String,LDouble>> finalSpans = new ArrayList<Pair<String,LDouble>>();
		for(i = 0; i < fes.length; i ++)
		{
			Map<String,LDouble> map = vs.get(fes[i]);
			Set<String> set = map.keySet();
			int size = set.size();
			Pair<String,LDouble>[] pArray = new Pair[size];
			int j = 0;
			for(String key:set)
			{
				pArray[j] = new Pair<String,LDouble>(key,map.get(key));
				j++;
			}
			Arrays.sort(pArray,comp);
			if(i==0)
			{
				int min = pArray.length;
				if(k<min)
					min=k;
				for(int m = 0; m < min; m ++)
				{
					finalSpans.add(new Pair<String,LDouble>(pArray[m].getFirst(),pArray[m].getSecond()));
				}
			}
			else
			{
				int oldSize = finalSpans.size();
				int newSize = oldSize*pArray.length;
				Pair<String,LDouble>[] newArray = new Pair[newSize];
				int countFinal = 0;
				for(int m = 0; m < oldSize; m ++)
				{
					for(int n = 0; n < pArray.length; n ++)
					{
						String newSpan = finalSpans.get(m).getFirst()+":"+pArray[n].getFirst();
						LDouble val = LDouble.convertToLogDomain(0); 
						if(!isThereOverlap(newSpan,seenSpans))
						{
							val = LogMath.logtimes(finalSpans.get(m).getSecond(),pArray[n].getSecond());
						}
						newArray[countFinal] = new Pair<String,LDouble>(newSpan,val);
						countFinal++;
					}
				}
				Arrays.sort(newArray,comp);
				int min = newArray.length;
				if(k<min)
					min=k;
				finalSpans = new ArrayList<Pair<String,LDouble>>();
				for(int m = 0; m < min; m ++)
					finalSpans.add(new Pair<String,LDouble>(newArray[m].getFirst(),newArray[m].getSecond()));
			}
		}
		String[] toks = finalSpans.get(0).getFirst().split(":");
		for(i = 0; i < fes.length; i ++)
		{
			result.put(fes[i], toks[i]);
		}		
		return result;
	}
	
	
	public String getNonOverlappingDecision(FrameFeatures mFF, String frameLine, int offset)
	{
		String frameName = mFF.frameName;
		System.out.println("Frame:"+frameName);
		String decisionLine=getInitialDecisionLine(frameLine, offset);
		if(mFF.fElements.size()==0)
		{
			decisionLine="0\t1"+"\t"+decisionLine.trim();
			return decisionLine;
		}
		if(mFF.fElements.size()==0)
		{
			decisionLine="0\t1"+"\t"+decisionLine.trim();
			return decisionLine;
		}
		THashMap<String,String> feMap = new THashMap<String,String>();
		ArrayList<SpanAndCorrespondingFeatures[]> featsList = mFF.fElementSpansAndFeatures;
		ArrayList<String> frameElements = mFF.fElements;
		int listSize = featsList.size();
		for(int i = 0; i < listSize; i ++)
		{
			SpanAndCorrespondingFeatures[] featureArray = featsList.get(i);
			int featArrLen = featureArray.length;
			double weiFeatSum[] = new double[featArrLen];
			int maxIndex = -1;
			double maxSum = -Double.MAX_VALUE;
			for(int j = 0; j < featArrLen; j ++)
			{
				int[] feats = featureArray[j].features;
				weiFeatSum[j]=getWeightSum(feats, localW);
				if(weiFeatSum[j]>maxSum)
				{
					maxSum = weiFeatSum[j];
					maxIndex=j;
				}
			}
			String outcome = featureArray[maxIndex].span[0]+"_"+featureArray[maxIndex].span[1];
			feMap.put(frameElements.get(i), outcome);
			System.out.println("Frame element:"+frameElements.get(i)+" Found span:"+outcome);
		}
		THashMap<String,String> oMap = getOnlyOverlappingFes(feMap);
		if(oMap.size()>0)
		{
			Set<String> tempKeySet = oMap.keySet();
			for(String key:tempKeySet)
			{
				System.out.println(key+"\t"+oMap.get(key));
			}
		}
		THashSet<String> seenSpans = new THashSet<String>();
		Set<String> keySet = feMap.keySet();
		for(String key:keySet)
		{
			String span = feMap.get(key);
			if(span.equals("-1_-1"))
				continue;
			if(!oMap.contains(key))
				seenSpans.add(span);
		}		
		if(seenSpans.size()>0)
			System.out.println("yes");		
		THashMap<String,THashMap<String,LDouble>> vs = new THashMap<String,THashMap<String,LDouble>>();
		for(int i = 0; i < listSize; i ++)
		{
			String fe = frameElements.get(i);
			if(!oMap.contains(fe))
				continue;
			SpanAndCorrespondingFeatures[] featureArray = featsList.get(i);
			THashMap<String,LDouble> valMap = new THashMap<String,LDouble>();
			int featArrLen = featureArray.length;
			double weiFeatSum[] = new double[featArrLen];
			for(int j = 0; j < featArrLen; j ++)
			{
				int[] feats = featureArray[j].features;
				weiFeatSum[j]=getWeightSum(feats, localW);
				double expVal = Math.exp(weiFeatSum[j]);
				LDouble lVal = LDouble.convertToLogDomain(expVal);
				String span = featureArray[j].span[0]+"_"+featureArray[j].span[1];
				valMap.put(span, lVal);
			}
			vs.put(frameElements.get(i), valMap);
		}				
		THashMap<String,String> nonOMap = getCubePruningDecoding(oMap, mFF.fElements, vs, 100, seenSpans);
		keySet = nonOMap.keySet();
		for(String key:keySet) {
			feMap.put(key, nonOMap.get(key));
		}		
		keySet = feMap.keySet();
		int count = 1;
		for(String fe:keySet)
		{
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
		System.out.println(decisionLine);
		return decisionLine;
	}	
	
	public static int isGreater(LDouble one, LDouble two)
	{
		if(one.isPositive()&&!two.isPositive())
		{
			return 1;
		}
		if(!one.isPositive()&&two.isPositive())
		{
			return -1;
		}
		
		boolean sign = one.isPositive();
		double oneValue = one.getValue();
		double twoValue = two.getValue();
		if(sign)
		{
			if(oneValue>twoValue)
				return 1;
			else if(oneValue==twoValue)
				return 0;
			else
				return -1;
		}
		else
		{
			if(oneValue>twoValue)
				return -1;
			else if(oneValue==twoValue)
				return 0;
			else
				return 1;
		
		}
	}
}
