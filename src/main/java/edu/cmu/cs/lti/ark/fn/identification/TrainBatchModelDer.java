/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * TrainBatchModelDer.java is part of SEMAFOR 2.0.
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

import edu.cmu.cs.lti.ark.fn.constants.FNConstants;
import edu.cmu.cs.lti.ark.fn.optimization.*;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import gnu.trove.THashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;


public class TrainBatchModelDer
{
	protected double[] params = null;
	protected String[] eventFiles = null;	
	protected String modelFile = null;
	protected int modelSize = 0;
	protected String mReg = null;
	protected double mLambda = 0.0;
	protected double[] gradients = null;
	
	public static void main(String[] args)
	{
		FNModelOptions io = new FNModelOptions(args);
		String alphabetFile = io.alphabetFile.get();
		String eventsFilePrefix = io.eventsFile.get();
		String modelFile = io.modelFile.get();
		String reg = io.reg.get();
		double lambda = io.lambda.get();
		String restartFile = io.restartFile.get();
		if(restartFile.equals("null"))
			restartFile=null;
		TrainBatchModelDer tbm = new TrainBatchModelDer(alphabetFile, 
				eventsFilePrefix, 
				modelFile, 
				reg, lambda, 
				restartFile);
		tbm.trainModel();
	}	
	
	private void setFeatNum(String alphabetFile) {
		try
		{
			BufferedReader bReader = new BufferedReader(new FileReader(alphabetFile));
			String line = bReader.readLine().trim();
			line=line.trim();
			modelSize = (new Integer(line))+1;
			bReader.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	private void setEventFiles(String eventsFilePrefix) {
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
				int i1 = new Integer(o1.substring(lastIndex+1));
				lastIndex = o2.lastIndexOf("_");
				int i2 = new Integer(o2.substring(lastIndex+1));
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
		for(int i = 0; i < files.length; i ++)
		{
			eventFiles[i]=dirFile.getAbsolutePath()+"/"+files[i];
		}
		System.out.println();
		System.out.println("Total number of datapoints:"+eventFiles.length);
	}
	
	private void initParams(String restartFile) {
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
	}
	
	public TrainBatchModelDer(String alphabetFile, 
			String eventsFilePrefix, 
			String modelFile0, 
			String reg, 
			double lambda, 
			String restartFile)
	{
		setFeatNum(alphabetFile);
		modelFile = modelFile0;
		setEventFiles(eventsFilePrefix);
		mReg = reg;
		mLambda=lambda;
		initParams(restartFile);
	}
	
	
	
	public void trainModel()
	{
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
	
	public void saveModel(String modelFile)
	{
		try
		{
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(modelFile));
			for(int i = 0; i < modelSize; i ++)		
			{
				bWriter.write(params[i]+"\n");
			}
			bWriter.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private double getValuesAndGradients() {
		int numDataPoints = eventFiles.length;
		double value = 0.0;
		Arrays.fill(gradients, 0.0);
		for (int index = 0; index < numDataPoints; index ++) {
			int[][][] featureArray=(int[][][])SerializedObjects.readSerializedObject(eventFiles[index]);
			int featArrLen = featureArray.length;
			double exp[][] = new double[featArrLen][];
			double sumExp[] = new double[featArrLen];
			double totalExp = 0.0;
			for(int i = 0; i < featArrLen; i ++) {
				exp[i] = new double[featureArray[i].length];
				sumExp[i] = 0.0;		
				for(int j = 0; j < exp[i].length; j ++) {
					double weiFeatSum = 0.0;
					int[] feats = featureArray[i][j];
					for (int k = 0; k < feats.length; k++)
					{
						weiFeatSum += params[feats[k]];
					}
					exp[i][j] = Math.exp(weiFeatSum);
					sumExp[i] += exp[i][j];
				}
				totalExp += sumExp[i];
			}
			value -= Math.log(sumExp[0]) - Math.log(totalExp);			
			for(int i = 0; i < featArrLen; i ++) {
				for(int j = 0; j < exp[i].length; j ++) {
					double Y = 0.0;
					if(i==0) {
						Y = exp[i][j]/sumExp[i];
					}
					double YMinusP = Y - (exp[i][j]/totalExp);
					int[] feats = featureArray[i][j];
					for(int k = 0; k < feats.length; k ++) {
						gradients[feats[k]] -= YMinusP;
					}
				}
			}
			if (index % 100 == 0) { System.out.print("."); };
		}
		if (mReg.equals("reg")) {
			for (int i = 0; i < params.length; ++i) {
				double weight = params[i];
				value += mLambda * (weight * weight);
				gradients[i] += 2 * mLambda * weight;
			}
		}		
		System.out.println();
		return value;
	}
	
	public void runCustomLBFGS() throws Exception
	{    
		double[] diagco = new double[modelSize];
		int[] iprint = new int[2];
		iprint[0] = FNConstants.m_debug?1:-1;
		iprint[1] = 0; //output the minimum level of info
		int[] iflag = new int[1];
		iflag[0] = 0;
		gradients = new double[modelSize];
		int iteration = 0;
		do {
			Arrays.fill(gradients, 0.0);
			double m_value = getValuesAndGradients();
			System.out.println("Function Value:"+m_value);
			LBFGS.lbfgs(modelSize,
					FNConstants.m_num_corrections, 
					params, 
					m_value,
					gradients, 
					false, //true if we're providing the diag of cov matrix Hk0 (?)
					diagco, //the cov matrix
					iprint, //type of output generated
					FNConstants.m_eps,
					FNConstants.xtol, //estimate of machine precision
					iflag //i don't get what this is about
			);
			iteration++;
			if(iteration%FNConstants.save_every_k==0)
				saveModel(modelFile+"_"+iteration);
		} while (iteration <= FNConstants.m_max_its&&iflag[0] != 0);
	}	
}
