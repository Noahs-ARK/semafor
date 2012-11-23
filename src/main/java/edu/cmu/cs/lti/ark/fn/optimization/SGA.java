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
package edu.cmu.cs.lti.ark.fn.optimization;


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
	
	public static double[] updateGradient(double[] params, double[] derivatives, double learningRate)
	{
		int length = params.length;
		for(int i = 0; i < length; i ++)
		{
			params[i] = params[i] - learningRate*derivatives[i];
		}
		return params;
	}
}


