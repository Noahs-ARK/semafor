/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * LRIdentificationModelHadoop.java is part of SEMAFOR 2.0.
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
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.hadoop.mapred.Reporter;

import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import edu.cmu.cs.lti.ark.util.optimization.LDouble;
import edu.cmu.cs.lti.ark.util.optimization.LogFormula;
import edu.cmu.cs.lti.ark.util.optimization.LDouble.IdentityElement;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;


public class LRIdentificationModelHadoop extends LRIdentificationModelSingleNode
{
	private String mTrainOrTest = "train";	
	private THashMap<String, THashSet<String>> mWnRelationsCache;
	public LRIdentificationModelHadoop(TObjectDoubleHashMap<String> paramList, String reg, double l, WordNetRelations mwnr, THashMap<String, THashSet<String>> frameMap, String trainOrTest, THashMap<String, THashSet<String>> wnRelationCache)
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
		mTrainOrTest=""+trainOrTest;
		mWnRelationsCache = wnRelationCache;
	}
	
	/**
	 * @param frame
	 * @param intTokNums
	 * @param data
	 * @param reporter
	 * @return
	 * @see LRIdentificationModelSingleNode#getFormulaForFrame(String, int[], String[][])
	 */
	protected LogFormula getFormulaForFrame(String frame, int[] intTokNums, String[][] data, Reporter reporter)	
	{
		THashSet<String> hiddenUnits = mFrameMap.get(frame);
		LogFormula result = getFormulaObject(LogFormula.Op.PLUS);
		DependencyParse parse = DependencyParse.processFN(data, 0.0);
		for (String unit : hiddenUnits)
		{
			FeatureExtractor featex = new FeatureExtractor();
			IntCounter<String> valMap = featex.extractFeatures(frame, intTokNums, unit, data, mWNR, mTrainOrTest, mWnRelationsCache,null,parse);	// last arg different from superclass method's call
			Set<String> features = valMap.keySet();
			LogFormula featSum = getFormulaObject(LogFormula.Op.PLUS);
			
			for (String feat : features)
			{
				double val = valMap.getT(feat);
				LogFormula prod = getFormulaObject(LogFormula.Op.TIMES);
				LogFormula featVal = getFormulaObject(LDouble.convertToLogDomain(val));
				prod.add_arg(featVal);
				LogFormula paramFormula = getLazyLookupParam(feat, mTrainOrTest);
				prod.add_arg(paramFormula);
				featSum.add_arg(prod);
				if(reporter!=null)
					reporter.setStatus("Found feature:"+feat);	// not in superclass method
			}
			LogFormula expFormula = getFormulaObject(LogFormula.Op.EXP);
			expFormula.add_arg(featSum);
			result.add_arg(expFormula);
		}
		return result;
	}
	
	public String getBestFrame(String frameLine, String parseLine, Reporter reporter)
	{
		String result = null;
		Set<String> set = mFrameMap.keySet();
		double maxVal = -Double.MIN_VALUE;
		for(String frame: set)
		{
			String[] toks = frameLine.split("\t");
			String newFrameLine = frame+"\t"+toks[1]+"\t"+toks[2];
			LogFormula formula = getNumeratorFormula(newFrameLine, parseLine, reporter);
			double val = formula.evaluate(this).exponentiate();
			if(val>maxVal)
			{
				maxVal = val;
				result=""+frame;
			}
			if(reporter!=null)
				reporter.setStatus("Considered "+frame+" for frameLine:"+frameLine);
			System.out.println("Considered "+frame+" for frameLine:"+frameLine);
		}
		return result;
	}
	
	protected LogFormula getNumeratorFormula(String frameLine, String parseLine, Reporter reporter)
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
		// Create the formula subtree
		LogFormula numerator = getFormulaForFrame(frameName, intTokNums,data, reporter);
		LogFormula ret = getFormulaObject(LogFormula.Op.LOG);
		ret.add_arg(numerator);	
		return ret;
	}
	
	/**
	 * @param frameLine
	 * @param parseLine
	 * @param reporter
	 * @return
	 * @see LRIdentificationModelSingleNode#getFormula(int)
	 */
	protected LogFormula getFormula(String frameLine, String parseLine, Reporter reporter)
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
		
		// Create the formula subtree
		LogFormula ratio = getFormulaObject(LogFormula.Op.DIVIDE);
		LogFormula numerator = getFormulaForFrame(frameName, intTokNums,data, reporter);	// last arg not in superclass method
		/*
		 * building denominator
		 */
		Set<String> frameSet = mFrameMap.keySet();
		LogFormula denominator = getFormulaObject(LogFormula.Op.PLUS);
		for (String frameDashed : frameSet)
		{
			LogFormula denomComponent = getFormulaForFrame(frameDashed, intTokNums,data, reporter);	// last arg not in superclass method
			denominator.add_arg(denomComponent);
		}
		ratio.add_arg(numerator);
		ratio.add_arg(denominator);
		
		LogFormula ret = getFormulaObject(LogFormula.Op.LOG);
		ret.add_arg(ratio);
		
		// Superclass method calls getRegularizationTerm()
		
		return ret;	
	}
	
	public LogFormula getRegularizationTerm() {
		
		m_current = 0;
		m_llcurrent = 0;	
		
		// (* -0.5 lambda (w . w))
		LogFormula ret = getFormulaObject(LogFormula.Op.TIMES);
		// -0.5
		LogFormula term1 = getFormulaObject(LDouble.convertToLogDomain(-0.5));
		// lambda
		LogFormula term2 = getFormulaObject(LDouble.convertToLogDomain(mLambda));
		// w . w
		LogFormula featweightsum = getFormulaObject(LogFormula.Op.PLUS);		
		String[] keys = new String[mParamList.size()];
		mParamList.keys(keys);
		totalNumberOfParams = keys.length;

		for(String param:keys)
		{
			LogFormula featweight = getFormulaObject(LogFormula.Op.TIMES);
			LogFormula formula = getLazyLookupParam(param, mTrainOrTest);
			featweight.add_arg(formula);
			featweight.add_arg(formula);
			featweightsum.add_arg(featweight);
		}
		ret.add_arg(term1);
		ret.add_arg(term2);
		ret.add_arg(featweightsum);
		return ret;
	}
	
	public THashMap<String,LDouble> getAllGradients(TObjectDoubleHashMap<String> paramMap)
	{	
		THashMap<String,LDouble> gradientMap = new THashMap<String,LDouble>();
		String[] keys = new String[paramMap.size()];
		paramMap.keys(keys);
		int len = keys.length;
		for(int i = 0; i < len; i ++)
		{
			int paramIndex = localA.get(keys[i]);
			LDouble gradient = G[paramIndex];
			gradientMap.put(keys[i], gradient);
		}		
		return gradientMap;
	}
	
}
