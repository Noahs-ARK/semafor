/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * TrainSGAModel.java is part of SEMAFOR 2.0.
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

import edu.cmu.cs.lti.ark.fn.optimization.LDouble;
import edu.cmu.cs.lti.ark.fn.optimization.LogFormula;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.util.optimization.SGA;

import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import static edu.cmu.cs.lti.ark.fn.parsing.Training.getRandArray;


public class TrainSGAModel extends TrainBatchModel
{
	private int batchSize=1;
	private int TOTAL_PASSES = 100;
	private Random rand;
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
		int batchSize = io.batchSize.get();
		TrainSGAModel tbm = new TrainSGAModel(alphabetFile,eventsFilePrefix,modelFile,reg, lambda,restartFile,batchSize,storeInMemory);
		tbm.trainModel();
	}	
	
	public TrainSGAModel(String alphabetFile, String eventsFilePrefix,
			String modelFile, String reg, double lambda, String restartFile,int batchSize, boolean storeInMemory)
	{
		super(alphabetFile, eventsFilePrefix, modelFile, reg, lambda, restartFile,storeInMemory);
		this.batchSize=batchSize;
		rand = new Random(new Date().getTime());
	}
	
	public void trainModel()
	{
		initializeParameterIndexes();
		initializeParameters();
		int sizeOfData = this.getNumTrainingExamples();
		int maxUpdates = (int)(((double)TOTAL_PASSES*(double)sizeOfData)/(double)batchSize);
		int totalUpdates=0;
		int countPasses = 0;
		int countDataEncountered=0;
		System.out.println("Max updates:"+maxUpdates);
		double[] m_estimate = new double[modelSize];
		getValuesForOptimizingRoutine(m_estimate);
		while(totalUpdates<maxUpdates)
		{
			int[] arr = getRandArray(batchSize, sizeOfData, rand);
			double[] sumDers = new double[modelSize];
			Arrays.fill(sumDers, 0.0);
			this.resetAllGradients();
			for(int j = 0; j < arr.length; j ++)
			{
				int sampleIndex = arr[j];
				LogFormula f = getFormula(sampleIndex);
				f.backprop(this, new LDouble(LDouble.IdentityElement.TIMES_IDENTITY));
				System.out.println("Encountered sample:"+sampleIndex);
			}	
			getGradientsForOptimizingRoutine(true,sumDers);
			countDataEncountered+=batchSize;
			m_estimate = SGA.updateGradient(m_estimate, sumDers,0.1);
			setValues(m_estimate);
			totalUpdates++;
			System.out.println("Update number:"+totalUpdates);
			if(countDataEncountered>=sizeOfData)
			{
				System.out.println("\nCompleted pass number:"+(countPasses+1)+" total updates till now:"+totalUpdates);
				saveModel(this.modelFile+"_"+countPasses);
				countPasses++;
				countDataEncountered=0;
			}
		}
	}
}
