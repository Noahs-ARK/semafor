/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * SGA.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.util.optimization;


public class SGA
{
	public static final double LEARNING_RATE = 0.001;
	
	public static double[] updateGradient(double[] params, double[] derivatives)
	{
		int length = params.length;
		for(int i = 0; i < length; i ++)
		{
			params[i] = params[i] - LEARNING_RATE*derivatives[i];
		}
		return params;
	}
	
	/** Uses the DCA algorithm as described in a Martins et al. paper draft.
	 * 
	 * From Eq. 15 (for CRFs):
	 *     θ_{t+1} = θ_t − η_t * (E_{θ_t}[φ(x_t, Y_t)] − φ(x_t, y_t))
	 * =??           θ_t − η_t * (gradient)
	 * with m training examples and learning rate η_t = 
	 *     min { 1/(λm), − log(P_{θ_t}(y_t | x_t)) / ||E_{θ_t}[φ(x_t, Y_t)] − φ(x_t, y_t)||^2
	 * =?? min { 1/(λm), − (log-value of the training data under the current model) / (squared L2 norm of the gradient) }
	 * where λ is the regularization parameter.
	 */
	public static double[] updateGradientDCA(double[] params, double[] derivatives, 
			double value, double numTrainingExamples, double regularizationParameter) {
		double learningRate = Math.min(1.0/(regularizationParameter*numTrainingExamples),
									   -value/sqsum(derivatives));
		return updateGradient(params, derivatives, learningRate);
	}
	
	public static double[] updateGradient(double[] params, double[] derivatives, double learningRate)
	{
		int length = params.length;
		for(int i = 0; i < length; i ++)
		{
			params[i] = params[i] - learningRate*derivatives[i];
		}
		return params;
	}
	
	/** Computes the sum of the squares of values in the vector. */
	public static double sqsum(double[] vec) {
		double sqsum = 0.0;
		for (double v : vec) {
			sqsum += v*v;
		}
		return sqsum;
	}
}


