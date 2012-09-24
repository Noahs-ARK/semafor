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

public interface Slave {
	public double[] makeZUpdate(double[] objVals, double rho, double[] us, double[] lambdas, double[] zs);
	public void cache(double[] as, double[] zs);
	public boolean checkEquals(double[] as);
	public void setObjVals(double[] objVals);
	public double computeDual(double[] objVals, double rho, double[] us, double[] lambdas, double[] zs);
}
