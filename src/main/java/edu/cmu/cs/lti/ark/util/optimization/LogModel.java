/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * LogModel.java is part of SEMAFOR 2.0.
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

import edu.cmu.cs.lti.ark.util.optimization.LDouble.IdentityElement;
import edu.cmu.cs.lti.ark.fn.optimization.Alphabet;
import gnu.trove.TObjectDoubleHashMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A model manages a couple of things.
 * It creates formulas that can be used in evaluating a function that depends on model parameters.
 * The formula could just be a model parameter (e.g., a multinomial probability), or it could be
 * a formula whose leaves are model parameters.
 * 
 * The model also stores those parameters and provides methods for accessing and changing them, 
 * and does the same for their gradients.
 * 
 * The model also contains methods for doing training. This abstract class contains methods for 
 * running LBFGS and stochastic gradient ascent to maximize functions represented as Formula objects.
 * For optimizing large formulas, LazyBigFormula should be used.
 * 
 * @author Kevin Gimpel
 * @date 3/2007
 */
public abstract class LogModel {
	/**
	 * Mappings from indices to parameter values and gradients
	 */
	public LDouble[] V;
	protected LDouble[] G;
	protected Alphabet A;
	protected TObjectDoubleHashMap<String> savedValues;
	/**
	 * The following ArrayLists store Formula and LazyLookupFormula objects for re-use 
	 * to avoid the need to constantly create and destroy Java objects, which can be costly.
	 */
	protected ArrayList<LogFormula> m_savedFormulas;
	/**
	 * The index of the next Formula in m_savedFormulas that can be used. 
	 */
	public int m_current;
	public ArrayList<LazyLookupLogFormula> m_savedLLFormulas;
	/**
	 * The index of the next LazyLookupFormula in m_savedLLFormulas that can be used. 
	 */
	public int m_llcurrent;

	protected static int PARAMETER_TABLE_INITIAL_CAPACITY = 2000000;
	protected static int FORMULA_LIST_INITIAL_CAPACITY = 500000;
	protected static int LLFORMULA_LIST_INITIAL_CAPACITY = 500000;
	
	/**
	 * Gets parameter value corresponding to given index, or null if none could be found. 
	 * @param index key to find parameter value in HashMap 
	 * @return the value if it could be found, or null otherwise
	 */
	public LDouble getValue(int index) {
		return V[index];
	}

	public void setValue(int index, LDouble newValue) {
		if (index >= V.length) {
			V = doubleArray(V);
		}
		V[index] = newValue;
	}	
	
	/**
	 * Copies value of given LDouble to entry in array.
	 * @param index
	 * @param newValue
	 */
	public void setValueInPlace(int index, LDouble newValue) {
		if (index >= V.length) {
			V = doubleArray(V);
		}
		V[index].reset(newValue);
	}

	/**
	 * Copies value of given double to entry in array.
	 * @param index
	 * @param newValue
	 */
	public void setValueInPlace(int index, double newValue) {
		if (index >= V.length) {
			V = doubleArray(V);
		}
		V[index].reset(newValue);
	}

	/**
	 * Copies value of given double to entry in array.
	 * @param index
	 * @param newValue
	 */
	public void setValueInPlace(int index, double newValue, boolean sign) {
		if (index >= V.length) {
			V = doubleArray(V);
		}
		V[index].reset(newValue, sign);		
	}
	
	/**
	 * Gets gradient value corresponding to given index, or null if none could be found.
	 * @param index key to find gradient value in HashMap
	 * @return the gradient if it could be found, or null otherwise
	 */
	public LDouble getGradient(int index) {
		return G[index];
	}

	/**
	 * Uses given LDouble as entry in HashMap.
	 * @param index
	 * @param newGradient
	 */
	public void setGradient(int index, LDouble newGradient) {
		if (index >= G.length) {
			G = doubleArray(G);
		}
		G[index] = newGradient;		
	}
	
	/**
	 * Copies value of given LDouble to entry in HashMap.
	 * @param index
	 * @param newGradient
	 */
	public void setGradientInPlace(int index, LDouble newGradient) {
		if (index >= G.length) {
			G = doubleArray(G);
		}
		G[index].reset(newGradient);		
	}

	/**
	 * Copies value of given LDouble to entry in HashMap.
	 * @param index
	 * @param newGradient
	 */
	public void setGradientInPlace(int index, double newGradient) {
		if (index >= G.length) {
			G = doubleArray(G);
		}
		G[index].reset(newGradient);		
	}
	
	/**
	 * Should be called when we run out of space in a[]. Creates a new array of 
	 * size 2*a.length and copies all elements in a to the new array. Then returns 
	 * the reference to the new array.  
	 * @param a LDouble[] to be expanded
	 */
	private LDouble[] doubleArray(LDouble[] a) {
		LDouble[] b = new LDouble[a.length * 2];
		System.arraycopy(a, 0, b, 0, a.length);
		return b;
	}
	
	protected abstract LogFormula getNextFormula();
	
	protected abstract LogFormula getFormula(int index);

	public abstract int getNumTrainingExamples(); 

	public double extractFunctionValueForLBFGS(LDouble functionValue, boolean maximize)
	{
		double f_of_x = functionValue.exponentiate();
		if(maximize)
			f_of_x = -1.0*f_of_x;
		return f_of_x;
	}
	
	protected double extractFunctionValueForLBFGSDashed(LDouble functionValue, boolean maximize)
	{
		double f_of_x = functionValue.value;
		if(maximize)
			f_of_x = -1.0*f_of_x;
		return f_of_x;
	}
	
	
	protected double extractGradientForLBFGS(LDouble gradientLDouble, boolean maximize) {
		double ret;
		ret = gradientLDouble.exponentiate();
		if (maximize) ret = -1.0 * ret;
		return ret;
	}
	

	protected String getParamString(int paramIndex) {
		return A.getString(paramIndex) + "; " + paramIndex + "; " + getValue(paramIndex) + "; " + getGradient(paramIndex);
	}
	
	protected abstract double classify();
	protected abstract double classifyTest();

	public abstract void saveModel(String modelFile);		

	protected void removeDuplicates(List<LazyLookupLogFormula> list) {
		HashSet<Integer> seenIndices = new HashSet<Integer>();		
		int i = 0;
		while (i < list.size()) {
			// if we've already seen this element
			if (seenIndices.contains(list.get(i).m_index)) {
				// remove it and don't increment the counter
				list.remove(i);
			} else {
				// if we're not removing anything, add the index to our hash set and increment the counter
				seenIndices.add(list.get(i).m_index);
				i++;
			}
		}
	}

	protected void removeDuplicateIndices(List<Integer> list) {
		HashSet<Integer> seenIndices = new HashSet<Integer>();		
		int i = 0;
		while (i < list.size()) {
			// if we've already seen this element
			if (seenIndices.contains(list.get(i))) {
				// remove it and don't increment the counter
				list.remove(i);
			} else {
				// if we're not removing anything, add the index to our hash set and increment the counter
				seenIndices.add(list.get(i));
				i++;
			}
		}
	}

	protected void removeDuplicateIndices(int[] arr) {
		HashSet<Integer> seenIndices = new HashSet<Integer>();		
		int i = 0;
		while (i < arr.length) {
			// if we've already seen this element
			if (seenIndices.contains(arr[i])) {
				// remove it and don't increment the counter
				System.out.println("Repeat: " + arr[i]);
				//list.remove(i);
				i++;
			} else {
				// if we're not removing anything, add the index to our hash set and increment the counter
				seenIndices.add(arr[i]);
				i++;
			}
		}
	}

	public Alphabet getAlphabet()
	{
		return A;
	}
	
	
	public void readFormulaMap()
	{
		return;
	}
	
	/*********************************************************************************************/
	/*************************** Methods for Getting Formula Objects *****************************/
	/*********************************************************************************************/
	public LogFormula getFormulaObject(LogFormula.Op o, String name) {
		LogFormula f;
		if (m_current == m_savedFormulas.size()) {
			// create a new one
			f = new LogFormula(o, name);
			m_savedFormulas.add(f);
			//System.out.println("All formulas: " + m_savedFormulas.size());
			m_current++;
			return f;
		} else {
			f = m_savedFormulas.get(m_current);
			f.reset(o, name);
			m_current++;
			return f;
		}
	}
	public LogFormula getFormulaObject(LogFormula.Op o) {
		LogFormula f;
		if (m_current == m_savedFormulas.size()) {
			// create a new one
			f = new LogFormula(o);
			m_savedFormulas.add(f);
			m_current++;
			return f;
		} else {
			f = m_savedFormulas.get(m_current);
			f.reset(o);
			m_current++;
			return f;
		}
	}
	
	public LogFormula getFormulaObject(LDouble v, String name) {
		LogFormula f;
		if (m_current == m_savedFormulas.size()) {
			// create a new one
			f = new LogFormula(v, name);
			m_savedFormulas.add(f);
			//System.out.println("All formulas: " + m_savedFormulas.size());
			m_current++;
			return f;
		} else {
			f = m_savedFormulas.get(m_current);
			f.reset(v, name);
			m_current++;
			return f;
		}
	}

	public LogFormula getFormulaObject(LDouble v) {
		LogFormula f;
		if (m_current == m_savedFormulas.size()) {
			// create a new one
			f = new LogFormula(v);
			m_savedFormulas.add(f);
			m_current++;
			return f;
		} else {
			f = m_savedFormulas.get(m_current);
			f.reset(v);
			m_current++;
			return f;
		}
	}
	public LogFormula getFormulaObject(IdentityElement ie) {
		LogFormula f;
		if (m_current == m_savedFormulas.size()) {
			// create a new one
			f = new LogFormula(ie);
			m_savedFormulas.add(f);
			m_current++;
			return f;
		} else {
			f = m_savedFormulas.get(m_current);
			f.reset(ie);
			m_current++;
			return f;
		}
	}
	public LogFormula getFormulaObject(double v) {
		LogFormula f;
		if (m_current == m_savedFormulas.size()) {
			// create a new one
			f = new LogFormula(v);
			m_savedFormulas.add(f);
			m_current++;
			return f;
		} else {
			f = m_savedFormulas.get(m_current);
			f.reset(v);
			m_current++;
			return f;
		}
	}
	
	public double getLambda()
	{
		return 0.0;
	}
	
	public String getReg()
	{
		return null;
	}
	
}
