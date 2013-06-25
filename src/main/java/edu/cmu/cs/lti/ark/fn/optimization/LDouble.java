/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * LDouble.java is part of SEMAFOR 2.0.
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

/**
 * Log-Double. A class to represent a double value along with a boolean indicating whether 
 * the exponentiated value should be positive (true) or negative (false). 
 * 
 * @author Kevin Gimpel
 *
 */
public class LDouble implements Serializable, Comparable<LDouble> { //Writable, Comparable<LDouble> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6988066946059321823L;

	public static enum IdentityElement {PLUS_IDENTITY, TIMES_IDENTITY}
	protected static double NEG_INF = Math.log(0.0);
	protected double value;
	protected boolean sign;

	/**
	 * Default constructor initializes value to be the plus-identity element in the 
	 * (logplus, plus) semiring. (Initialize as LDouble(0.0) for the times-identity element)
	 *
	 */
	public LDouble() {
		value = NEG_INF;
		sign = true;
	}
	
	public LDouble(IdentityElement ie) {
		if (ie == IdentityElement.PLUS_IDENTITY) {
			value = NEG_INF;			
		} else if (ie == IdentityElement.TIMES_IDENTITY) {
			value = 0.0;
		} else {
			value = 0.0; // todo: use a better value here
		}
		sign = true;
	}

	public LDouble(LDouble d) {
		value = d.value;
		sign = d.sign;
	}

	public LDouble(double v) {
		value = v;
		sign = true;
	}

	public LDouble(double v, boolean s) {
		value = v;
		sign = s;
	}
	
	public void reset() {
		value = NEG_INF;
		sign = true;
	}
	public static LDouble convertToLogDomain(double x) {
		if (x < 0) {
			return new LDouble(Math.log(-1.0 * x), false);
		} else {
			return new LDouble(Math.log(x), true);
		}
	}
	public static void convertToLogDomain(double x, LDouble result) {
		if (x < 0) {
			result.reset(Math.log(-1.0 * x), false);
		} else {
			result.reset(Math.log(x), true);
		}
	}
	public boolean equalsZero() {
		return (value == NEG_INF);
	}
	public boolean notEqualsZero() {
		return (value != NEG_INF);
	}
	public void reset(IdentityElement ie) {
		if (ie == IdentityElement.PLUS_IDENTITY) {
			value = NEG_INF;			
		} else if (ie == IdentityElement.TIMES_IDENTITY) {
			value = 0.0;
		} else {
			value = 0.0; // todo: use a better value here
		}
		sign = true;
	}

	public void reset(LDouble ld) {
		value = ld.value;
		sign = ld.sign;
	}
	public void reset(double v) {
		value = v;
		sign = true;		
	}
	
	public void reset(double v, boolean s) {
		value = v;
		sign = s;		
	}

	public boolean isPositive() {
		return sign;
	}
	public double getValue() {
		return value;
	}
	public String toString() {
		return "" + value + ", " + sign;
	}
	public String toStringConvert() {
		return "" + value + ", " + sign + "; \t " + exponentiate();
	}
	
	public double exponentiate() {
		double ret = Math.exp(value);
		if (!sign) {
			ret = -1.0 * ret;
		}
		return ret;
	}

	public void readFields(DataInput in) throws IOException
	{
		value=in.readDouble();
		sign=in.readBoolean();
	}

	public void write(DataOutput out) throws IOException {
		out.writeDouble(value);
		out.writeBoolean(sign);
	}

	@Override
	public int compareTo(LDouble other) {
		final Double value = getValue();
		final Double otherValue = other.getValue();
		if(isPositive()) {
			return other.isPositive() ? value.compareTo(otherValue) : 1;
		} else {
			return other.isPositive() ? -1 : otherValue.compareTo(value);
		}
	}
}
