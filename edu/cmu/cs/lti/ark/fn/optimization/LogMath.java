/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * LogMath.java is part of SEMAFOR 2.0.
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

/**
 * Routines for basic mathematical operations (+,-,*,/,^) in the log domain.
 * 
 * @author Original (C++) code by Noah Smith; translated to Java and expanded by Kevin Gimpel
 */
public class LogMath {
	static double negInf = Math.log(0.0);
	
	/**
	 * If lx.sign == true, treat exp(lx.value) as positive; otherwise, treat it as negative.
	 * So,  
	 * If lx.sign == true and ly.sign == true, returns ln(exp(lx.value) + exp(ly.value)).
	 * If lx.sign == false and ly.sign == true, returns ln(-exp(lx.value) + exp(ly.value)).
	 * If lx.sign == true and ly.sign == false, returns ln(exp(lx.value) - exp(ly.value)).
	 * If lx.sign == false and ly.sign == false, returns ln(-exp(lx.value) - exp(ly.value)).
	 * @param lx
	 * @param ly
	 * @return
	 */
	public static LDouble logplus(LDouble lx, LDouble ly) {
		//double negInf = Math.log(0.0);
		// if the numbers have opposite signs, we can just use logminus instead (with a sign flip for the negative one)
		if (lx.sign && !ly.sign) return logminus(lx, new LDouble(ly.value, true));
		if (!lx.sign && ly.sign) return logminus(ly, new LDouble(lx.value, true));
						
		if (ly.value == negInf || lx.value - ly.value > 700.0) return lx;
		if (lx.value == negInf || ly.value - lx.value > 700.0) return ly;
		if (lx.value > ly.value) 
			return new LDouble(Math.log(1.0 + Math.exp(ly.value - lx.value)) + lx.value, lx.sign);
		else 
			return new LDouble(Math.log(1.0 + Math.exp(lx.value - ly.value)) + ly.value, lx.sign);
	}

	public static void logplus(LDouble lx, LDouble ly, LDouble result) {
		// if the numbers have opposite signs, we can just use logminus instead (with a sign flip for the negative one)
		if (lx.sign && !ly.sign) {
			logminus(lx, new LDouble(ly.value, true), result);
			return;
		}
		if (!lx.sign && ly.sign) {
			logminus(ly, new LDouble(lx.value, true), result);
			return;
		}
		  
		if (ly.value == negInf || lx.value - ly.value > 700.0) {
			result.reset(lx.value, lx.sign);
			return;
		}
		if (lx.value == negInf || ly.value - lx.value > 700.0) {
			result.reset(ly.value, ly.sign);
			return;
		}
		
		if (lx.value > ly.value) {
			result.reset(Math.log(1.0 + Math.exp(ly.value - lx.value)) + lx.value, lx.sign);
		} else {
			result.reset(Math.log(1.0 + Math.exp(lx.value - ly.value)) + ly.value, lx.sign);
		}
	}


	public static LDouble logtimes(LDouble lx, LDouble ly) {
		return new LDouble(lx.value + ly.value, lx.sign == ly.sign);
	}

	/**
	 * Places result into third argument.
	 * @param lx
	 * @param ly
	 * @param result
	 */
	public static void logtimes(LDouble lx, LDouble ly, LDouble result) {
		result.reset(lx.value + ly.value, lx.sign == ly.sign);
	}

	public static LDouble logdivide(LDouble lx, LDouble ly) {
		return new LDouble(lx.value - ly.value, lx.sign == ly.sign);
	}

	/**
	 * Places result into third argument.
	 * @param lx
	 * @param ly
	 * @param result
	 */
	public static void logdivide(LDouble lx, LDouble ly, LDouble result) {
		result.reset(lx.value - ly.value, lx.sign == ly.sign);
	}
	
	public static LDouble logminus(LDouble lx, LDouble ly) {
		// if the numbers have opposite signs, we can just use logplus instead (with a sign flip for the second operand)
		if (lx.sign && !ly.sign) return logplus(lx, new LDouble(ly.value, true));
		if (!lx.sign && ly.sign) return logplus(lx, new LDouble(ly.value, false));

		// if we've gotten this far, we know that lx.sign == ly.sign
		
		// check if we need to perform the subtraction
		// we might be able to just return lx if ly is the zero element(or ly with an inverted 
		// sign if lx is the zero element)
		if (ly.value == negInf || lx.value - ly.value > 700.0) return lx;
		if (lx.value == negInf || ly.value - lx.value > 700.0) return new LDouble(ly.value, !ly.sign);
		
		// if lx is greater, 
		if (lx.value > ly.value) 
			return new LDouble(Math.log(1.0 - Math.exp(ly.value - lx.value)) + lx.value, lx.sign);
		else 
			return new LDouble(Math.log(1.0 - Math.exp(lx.value - ly.value)) + ly.value, !lx.sign);
	}
	
	public static void logminus(LDouble lx, LDouble ly, LDouble result) {
		// if the numbers have opposite signs, we can just use logplus instead (with a sign flip for the second operand)
		if (lx.sign && !ly.sign) {
			logplus(lx, new LDouble(ly.value, true), result);
			return;
		}
		if (!lx.sign && ly.sign) {
			logplus(lx, new LDouble(ly.value, false), result);
			return;
		}

		// if we've gotten this far, we know that lx.sign == ly.sign
		
		// check if we need to perform the subtraction
		// we might be able to just return lx if ly is the zero element(or ly with an inverted 
		// sign if lx is the zero element)
		if (ly.value == negInf || lx.value - ly.value > 700.0) {
			result.reset(lx.value, lx.sign);
			return;
		}
		if (lx.value == negInf || ly.value - lx.value > 700.0) {
			result.reset(ly.value, !ly.sign);
			return;
		}
		
		// if lx is greater, 
		if (lx.value > ly.value) {
			result.reset(Math.log(1.0 - Math.exp(ly.value - lx.value)) + lx.value, lx.sign);
		} else {
			result.reset(Math.log(1.0 - Math.exp(lx.value - ly.value)) + ly.value, !lx.sign);
		}
	}

	
	public static LDouble logpower(LDouble lx, LDouble ly) {
		return new LDouble((logexp(ly).value * lx.value), lx.sign);
		//return logtimes(logexp(lx), ly);
	}
	public static void logpower(LDouble lx, LDouble ly, LDouble result) {
		// todo: hangle sign properly
		// if sign of lx is negative,...?
		result.reset((logexp(ly).value * lx.value), lx.sign);
		//logtimes(logexp(ly), lx, scratch);
	}

	public static LDouble logexp(LDouble lx) {
		LDouble ret = new LDouble(Math.exp(lx.value));
		if (!lx.isPositive()) {
			ret.value = -1.0 * ret.value;
		}
		return ret;
	}
	public static LDouble logexp(LDouble lx, LDouble result) {
		result.reset(Math.exp(lx.value));
		if (!lx.isPositive()) {
			result.value = -1.0 * result.value;
		}
		return result;
	}
}
