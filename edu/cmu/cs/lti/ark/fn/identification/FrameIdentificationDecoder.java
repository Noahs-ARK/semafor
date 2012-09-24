/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * FrameIdentificationDecoder.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.identification;

import java.util.Arrays;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.hadoop.mapred.Reporter;

import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import edu.cmu.cs.lti.ark.util.optimization.LDouble;
import edu.cmu.cs.lti.ark.util.optimization.LogFormula;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;

/**
 * @author dipanjan
 * This class is for finding the best frame for a span of tokens, given 
 * the annotation for a sentence. It is just a single node implementation.
 */
public class FrameIdentificationDecoder extends LRIdentificationModelSingleNode
{
	private THashMap<String, THashSet<String>> mWnRelationsCache;
	public FrameIdentificationDecoder(TObjectDoubleHashMap<String> paramList, 
									  String reg, 
									  double l, 
									  WordNetRelations mwnr, 
									  THashMap<String, THashSet<String>> frameMap, 
									  THashMap<String, THashSet<String>> wnRelationCache)
	{
		super(paramList,reg,l,mwnr,frameMap);
		initializeParameterIndexes();
		this.mParamList=paramList;
		mReg=reg;
		mLambda=l;
		mWNR=mwnr;
		mFrameMap=frameMap;
		totalNumberOfParams=paramList.size();
		initializeParameters();
		mLookupChart = new TIntObjectHashMap<LogFormula>();
		mWnRelationsCache = wnRelationCache;
	}
	
	private double getNumeratorValue(String frameLine, String parseLine)
	{
		// Parse information from the specified line
		m_current = 0;
		m_llcurrent = 0;	
		String[] toks = frameLine.split("\t");
		String frameName = toks[0];
		String[] tokNums = toks[1].split("_");
		int[] intTokNums = new int[tokNums.length];
		for(int j = 0; j < tokNums.length; j ++)
			intTokNums[j] = new Integer(tokNums[j]);
		Arrays.sort(intTokNums);
		StringTokenizer st = new StringTokenizer(parseLine,"\t");
		int tokensInFirstSent = new Integer(st.nextToken());
		String[][] data = new String[5][tokensInFirstSent];
		for(int k = 0; k < 5; k ++)
		{
			data[k]=new String[tokensInFirstSent];
			for(int j = 0; j < tokensInFirstSent; j ++)
			{
				data[k][j]=""+st.nextToken().trim();
			}
		}		
		double idVal = getValueForFrame(frameName, intTokNums,data);
		return idVal;
	}	
	
	private double getValueForFrame(String frame, int[] intTokNums, String[][] data)	
	{
		THashSet<String> hiddenUnits = mFrameMap.get(frame);
		double result = 0.0;
		DependencyParse parse = DependencyParse.processFN(data, 0.0);
		for (String unit : hiddenUnits)
		{
			FeatureExtractor featex = new FeatureExtractor();
			IntCounter<String> valMap =  featex.extractFeatures(frame, intTokNums, unit, data, mWNR, "test", mWnRelationsCache,null,parse);
			Set<String> features = valMap.keySet();
			double featSum = 0.0;
			for (String feat : features)
			{
				double val = valMap.getT(feat);
				int ind = localA.get(feat);
				double paramVal = V[ind].exponentiate();
				double prod = val*paramVal;
				featSum+=prod;
			}
			double expVal = Math.exp(featSum);
			result+=expVal;
		}
		return result;
	}
	
	
	public String getBestFrame(String frameLine, String parseLine)
	{
		String result = null;
		Set<String> set = mFrameMap.keySet();
		double maxVal = -Double.MIN_VALUE;
		for(String frame: set)
		{
			String[] toks = frameLine.split("\t");
			String newFrameLine = frame+"\t"+toks[1]+"\t"+toks[2];
			double val =  getNumeratorValue(newFrameLine, parseLine);
			if(val>maxVal)
			{
				maxVal = val;
				result=""+frame;
			}
			System.out.println("Considered "+frame+" for frameLine:"+frameLine);
		}
		return result;
	}

}
