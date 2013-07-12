/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * TrainBatchModel.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.identification.latentmodel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import riso.numerical.LBFGS;

import edu.cmu.cs.lti.ark.fn.optimization.*;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import gnu.trove.TObjectDoubleHashMap;


public class TrainBatchModel extends LogModel
{
	protected double[] params = null;
	protected String[] eventFiles = null;	
	protected String modelFile = null;
	protected int modelSize = 0;
	protected String mReg = null;
	protected double mLambda = 0.0;
	protected LogFormula[] mLookupChart;
	protected boolean storeInMemory = false;
	protected int[][][][] allEvents = null;
	
	public static void main(String[] args)
	{
		FNModelOptions io = new FNModelOptions(args);
		String alphabetFile = io.alphabetFile.get();
		String eventsFilePrefix = io.eventsFile.get();
		String modelFile = io.modelFile.get();
		String reg = io.reg.get();
		double lambda = io.lambda.get();
		String restartFile = io.restartFile.get();
		boolean storeInMemory = io.storeInMemory.get().equals("true");
		if(restartFile.equals("null"))
			restartFile=null;
		TrainBatchModel tbm = new TrainBatchModel(alphabetFile, 
				eventsFilePrefix, 
				modelFile, 
				reg, lambda, 
				restartFile, 
				storeInMemory);
		tbm.trainModel();
	}	
	
	public TrainBatchModel(String alphabetFile, String eventsFilePrefix, String modelFile, String reg, double lambda, String restartFile, boolean storeInMemory)
	{
		try
		{
			BufferedReader bReader = new BufferedReader(new FileReader(alphabetFile));
			String line = bReader.readLine().trim();
			line=line.trim();
			modelSize = (Integer.parseInt(line))+1;
			bReader.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		this.modelFile = modelFile;
		File dirFile = new File(eventsFilePrefix);	
		FilenameFilter filter = new FilenameFilter(){
			public boolean accept(File dir, String name) {
				return name.startsWith("feats")&&name.endsWith(".jobj");
			}			
		};
		String[] files = dirFile.list(filter);
		Comparator<String> comp = new Comparator<String>(){
			public int compare(String o1, String o2) {
				o1=o1.substring(0,o1.length()-5);
				o2=o2.substring(0,o2.length()-5);
				int lastIndex = o1.lastIndexOf("_");
				int i1 = Integer.parseInt(o1.substring(lastIndex+1));
				lastIndex = o2.lastIndexOf("_");
				int i2 = Integer.parseInt(o2.substring(lastIndex+1));
				if(i1<i2)
					return -1;
				else if(i1==i2)
					return 0;
				else 
					return 1;
			}
		};
		Arrays.sort(files,comp);
		eventFiles=new String[files.length];
		if(storeInMemory)
		{
			System.out.println("Storing in memory...");
			allEvents=new int[eventFiles.length][][][];
		}
		else
			System.out.println("Not storing in Memory");
		for(int i = 0; i < files.length; i ++)
		{
			eventFiles[i]=dirFile.getAbsolutePath()+"/"+files[i];
			if(storeInMemory)
			{
				allEvents[i]=(int[][][])SerializedObjects.readSerializedObject(eventFiles[i]);
				System.out.print(".");
				if(i%100==0)
					System.out.print(i+" ");
			}
		}
		System.out.println();
		System.out.println("Total number of datapoints:"+eventFiles.length);
		mReg = reg;
		mLambda=lambda/eventFiles.length;
		params = new double[modelSize];
		for(int i = 0; i < modelSize; i ++)
			params[i] = 1.0;
		if(restartFile!=null)
		{
			int i = 0;
			try
			{
				BufferedReader bReader = new BufferedReader(new FileReader(restartFile));
				String line = null;
				while((line=bReader.readLine())!=null)
				{
					params[i]=new Double(line.trim());
					i++;
				}
				bReader.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}
		mLookupChart = new LogFormula[modelSize];
		for(int i = 0; i < modelSize; i ++)
		{
			mLookupChart[i]=null;
		}
	}
	
	public void trainModel()
	{
		initializeParameterIndexes();
		initializeParameters();
		try
		{
			runCustomLBFGS();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void initializeParameters()
	{
		for(int i = 0; i < modelSize; i ++)
		{
			LDouble lDoubleVal = LDouble.convertToLogDomain(params[i]);
			setValue(i, lDoubleVal);
			setGradient(i, new LDouble(LDouble.IdentityElement.PLUS_IDENTITY));
		}
	}	

	public void initializeParameterIndexes()
	{
		V = new LDouble[PARAMETER_TABLE_INITIAL_CAPACITY];
		G = new LDouble[PARAMETER_TABLE_INITIAL_CAPACITY];
		savedValues = new TObjectDoubleHashMap<String>(1000);
		m_savedFormulas = new ArrayList<LogFormula>(FORMULA_LIST_INITIAL_CAPACITY);
		m_current = 0;
		m_savedLLFormulas = new ArrayList<LazyLookupLogFormula>(LLFORMULA_LIST_INITIAL_CAPACITY);
		m_llcurrent = 0;
	}	
	
	protected double classify() {
		// TODO Auto-generated method stub
		return 0;
	}

	protected double classifyTest() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public LogFormula checkLookupChart(Integer ind)
	{
		return mLookupChart[ind];
	}
	
	public LogFormula addToLookUpChart(int ind, String name)
	{
		LogFormula f = new LazyLookupLogFormula(ind, name);
		mLookupChart[ind]=f;
		return f;
	}
	
	public LogFormula getLazyLookupParam(int index)
	{	
		LogFormula f;
		f = checkLookupChart(index);	// get cached version, if possible
		if(f == null)
		{
			f = addToLookUpChart(index,""+index);	// otherwise, create a new formula node and cache it
		}
		return f;
	}
	
	
	protected LogFormula getFormulaForFrame(int[][] feats)	
	{
		LogFormula result = null;
		if(feats.length!=1)
			result = getFormulaObject(LogFormula.Op.PLUS);
		for (int[] arr : feats)
		{
			LogFormula featSum = getFormulaObject(LogFormula.Op.PLUS);
			for (int feature:arr)
			{
				LogFormula prod = getFormulaObject(LogFormula.Op.TIMES);
				LogFormula featVal = getFormulaObject(LDouble.convertToLogDomain(1.0));
				prod.add_arg(featVal);
				LogFormula paramFormula = getLazyLookupParam(feature);	// the feature's weight
				prod.add_arg(paramFormula);	// feature's value * weight
				featSum.add_arg(prod);	// sum over all features' value * weight
			}
			LogFormula expFormula = getFormulaObject(LogFormula.Op.EXP);
			expFormula.add_arg(featSum);	// exp(featSum)
			if(result==null)
				return expFormula;
			result.add_arg(expFormula);	// sum_{hiddenUnits (latent variables)} exp(featSum)
		}
		return result;
	}	
	
	
	protected LogFormula getFormula(int index)
	{
		// Parse information from the specified line
		m_current = 0;
		m_llcurrent = 0;
		
		int[][][] feats = null;
		if(!storeInMemory)
			feats = (int[][][])SerializedObjects.readSerializedObject(eventFiles[index]);
		else
			feats = allEvents[index];
		int len = feats.length;
		
		// Create the formula subtree
		LogFormula ratio = getFormulaObject(LogFormula.Op.DIVIDE);
		LogFormula numerator = getFormulaForFrame(feats[0]);
		/*
		 * building denominator
		 */
		LogFormula denominator = getFormulaObject(LogFormula.Op.PLUS);
		for (int i = 0; i < len; i ++)
		{
			LogFormula denomComponent = getFormulaForFrame(feats[i]);
			denominator.add_arg(denomComponent);
		}
		ratio.add_arg(numerator);
		ratio.add_arg(denominator);
		LogFormula ret = getFormulaObject(LogFormula.Op.LOG);
		ret.add_arg(ratio);
		
		if(mReg.equals("reg"))	// Regularization
		{	
			LogFormula ret2 = getFormulaObject(LogFormula.Op.PLUS);
			LogFormula regTerm = getRegularizationTerm();
			ret2.add_arg(ret);
			ret2.add_arg(regTerm);
			return ret2;
		}
		else
		{
			return ret;
		}
	}
	
	protected LogFormula getRegularizationTerm() {
		// (* -0.5 lambda (w . w))
		LogFormula ret = getFormulaObject(LogFormula.Op.TIMES);
		// -0.5
		LogFormula term1 = getFormulaObject(LDouble.convertToLogDomain(-1.0));
		// lambda
		LogFormula term2 = getFormulaObject(LDouble.convertToLogDomain(mLambda));
		// w . w
		LogFormula featweightsum = getFormulaObject(LogFormula.Op.PLUS);		
		
		for(int i = 0; i < modelSize; i ++)
		{
			LogFormula featweight = getFormulaObject(LogFormula.Op.TIMES);
			LogFormula formula = getLazyLookupParam(i);
			featweight.add_arg(formula);
			featweight.add_arg(formula);
			featweightsum.add_arg(featweight);
		}
		ret.add_arg(term1);
		ret.add_arg(term2);
		ret.add_arg(featweightsum);
		return ret;
	}

	protected LogFormula getNextFormula() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getNumTrainingExamples() {
		return eventFiles.length;
	}

	public void saveModel(String modelFile)
	{
		try
		{
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(modelFile));
			double[] result = new double[modelSize];
			for(int i = 0; i < modelSize; i ++)		
			{
				LDouble val = getValue(i);
				result[i]=val.exponentiate();
				bWriter.write(result[i]+"\n");
			}
			bWriter.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void resetAllGradients()
	{
		for(int i = 0; i < modelSize; i ++)		
		{
			setGradient(i, new LDouble(LDouble.IdentityElement.PLUS_IDENTITY));
		}
	}
	
	public void getGradientsForOptimizingRoutine(boolean maximize, double[] result)
	{
		int count = 0;
		double factor;
		if(maximize)
			factor = -1.0;
		else
			factor = 1.0;
		for(int i = 0; i < modelSize; i ++)		
		{
			LDouble grad = getGradient(i);
			result[i]=grad.exponentiate()*factor;
		}
	}
	
	public void setValues(double[] values)
	{
		for(int i = 0; i < modelSize; i ++)		
		{
			setValue(i, LDouble.convertToLogDomain(values[i]));
		}
	}
	
	public void getValuesForOptimizingRoutine(double[] result)
	{
		for(int i = 0; i < modelSize; i ++)		
		{
			LDouble val = getValue(i);
			result[i]=val.exponentiate();
		}
	}
	
	
	public void runCustomLBFGS() throws Exception
	{    
		LogFormula.Op Type = LogFormula.Op.PLUS;
		RootLogFormula fullLogLike = new RootLogFormula(this,Type,"lazyroot");
		double[] diagco = new double[modelSize];
		int[] iprint = new int[2];
		iprint[0] = Lbfgs.DEBUG ?1:-1;
		iprint[1] = 0; //output the minimum level of info
		int[] iflag = new int[1];
		iflag[0] = 0;
		double[] gradient = new double[modelSize];
		double[] m_estimate = new double[modelSize];
		int iteration = 0;
		boolean maximize = true;
		do {
			resetAllGradients();
			LDouble l_value = fullLogLike.evaluateAndBackProp(this);
			double m_value = extractFunctionValueForLBFGS(l_value, maximize);
			System.out.println("Function Value:"+m_value);
			getGradientsForOptimizingRoutine(maximize,gradient);
			getValuesForOptimizingRoutine(m_estimate);
			LBFGS.lbfgs(modelSize,
					Lbfgs.NUM_CORRECTIONS,
					m_estimate, 
					m_value,
					gradient, 
					false, //true if we're providing the diag of cov matrix Hk0 (?)
					diagco, //the cov matrix
					iprint, //type of output generated
					Lbfgs.STOPPING_THRESHOLD,
					Lbfgs.XTOL, //estimate of machine precision
					iflag //i don't get what this is about
			);
			setValues(m_estimate);
			iteration++;
			fullLogLike.changedParamValues();
			if(iteration% Lbfgs.SAVE_EVERY_K ==0)
				saveModel(modelFile+"_"+iteration);
		}while (iteration <= Lbfgs.MAX_ITERATIONS &&iflag[0] != 0);
	}	
}
