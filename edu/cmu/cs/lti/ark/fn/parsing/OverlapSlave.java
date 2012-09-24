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

import java.util.Arrays;
import java.util.Collections;

import edu.cmu.cs.lti.ark.fn.utils.BitOps;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;

public class OverlapSlave implements Slave {

	public double[] mObjVals;
	public int[] mIndices;
	public double[] oldAs;
	public double[] oldZs;
	
	
	public OverlapSlave(double[] objVals, 
						int[] indices) {
		mObjVals = objVals;
		mIndices = indices;
		oldAs = null;
		oldZs = null;
	}
	
	@Override
	public void setObjVals(double[] objVals) {
		mObjVals = objVals;
	}
	
	public double computeDual(double[] objVals, double rho, double[] us, double[] lambdas,
			double[] zs) {
		setObjVals(objVals);
		double value = 0.0;
		for (int i = 0; i < mIndices.length; i++) {
			value += zs[mIndices[i]] * (mObjVals[mIndices[i]] + lambdas[mIndices[i]]);
			value -= lambdas[mIndices[i]] * us[mIndices[i]];
			value -= (zs[mIndices[i]] - us[mIndices[i]]) * (zs[mIndices[i]] - us[mIndices[i]]) * rho / 2.0;
		}	
		return value;
	}

	public double[] makeZUpdate(double[] objVals, double rho, double[] us, double[] lambdas,
			double[] zs) {
		setObjVals(objVals);
		double[] as = new double[mIndices.length];
		for (int i = 0; i < as.length; i++) {
			double a = us[mIndices[i]] + 
					   (1.0 / rho) * (mObjVals[mIndices[i]] + lambdas[mIndices[i]]);
			as[i] = a;
		}
		if (checkEquals(as)) {
			return oldZs;
		}
		double[] updZs = new double[mObjVals.length];
		Arrays.fill(updZs, 0);
		double sum = 0.0;
		for (int i = 0; i < mIndices.length; i++) {
			updZs[mIndices[i]] = Math.min(1.0, Math.max(as[i], 0));
			sum += updZs[mIndices[i]];
		}
		if (sum <= 1.0) {
			return updZs;
		}		
		Double[] bs = new Double[as.length];
		for (int i = 0; i < bs.length; i++) {
			bs[i] = as[i];
		}
		Arrays.sort(bs, Collections.reverseOrder());
		double[] sums = new double[as.length];
		Arrays.fill(sums, 0);
		sums[0] = bs[0];
		for (int i = 1; i < as.length; i++) {
			sums[i] = sums[i-1] + bs[i];
		}
		int tempRho = -1;
		for (int i = 0; i < as.length; i++) {
			double temp = bs[i] - (1.0 / (double)(i+1)) * (sums[i] - 1.0);
			if (temp > 0) {
				if (i > tempRho) {
					tempRho = i;
				}
			}
		}
		if (tempRho == -1) {
			System.out.println("Problem. tempRho is -1");
			System.exit(-1);
		}
		double tau = (1.0 / (double)(tempRho+1)) * (sums[tempRho] - 1.0);
		Arrays.fill(updZs, 0);
		sum = 0.0;
		for (int i = 0; i < mIndices.length; i++) {
			updZs[mIndices[i]] = Math.max(as[i] - tau, 0);
			sum += updZs[mIndices[i]];
		}
		cache(as, updZs);
		return updZs;
	}

	@Override
	public void cache(double[] as, double[] zs) {
		oldAs = Arrays.copyOf(as, as.length);
		oldZs = Arrays.copyOf(zs, zs.length);
	}
	
	@Override
	public boolean checkEquals(double[] as) {
		// TODO Auto-generated method stub
		if (oldAs == null) {
			return false;
		}
		return BitOps.nearlyEquals(as, oldAs, FNModelOptions.TOL);
	}
}