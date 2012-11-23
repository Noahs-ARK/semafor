/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * RootLogFormula.java is part of SEMAFOR 2.0.
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

import java.util.ArrayList;
import java.util.Random;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;

public class RootLogFormula extends LogFormula {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1983416993175438308L;
	/**
	 * Holds shuffled indices of phrase pairs, for use with stochastic gradient descent
	 */
	TIntArrayList m_shuffledIndices = new TIntArrayList();
	/**
	 * Saves index of last data item used in training, for use with stochastic gradient descent.
	 */
	protected int m_lastDatumUsed = -1;
	
	protected LogModel m_owner;
	protected TIntHashSet m_paramIndices;
	protected int[] m_paramIndicesArray; 
	protected boolean m_onceThrough;
	protected int m_numNull = 0;
	protected LDouble[] m_savedFunctionValues;
	
	protected static int INITIAL_CAPACITY = 10000;
	
	public RootLogFormula(LogModel m, Op o, String name) {
		super(o, name);
		m_owner = m;
		initCommon(INITIAL_CAPACITY);		
	}

	public RootLogFormula(LogModel m, Op o, String name, int batchsize) {
		super(o, name);
		m_owner = m;
		initCommon(INITIAL_CAPACITY, batchsize);
	}

	private void initCommon(int capacity, int batchsize) {
		m_paramIndices = new TIntHashSet();
		m_onceThrough = false;
		m_savedFunctionValues = new LDouble[batchsize];
		for (int i = 0; i < batchsize; i++) {
			m_savedFunctionValues[i] = new LDouble();
		}
	}

	private void initCommon(int capacity) {
		m_paramIndices = new TIntHashSet();
		m_onceThrough = false;
	}
		
	/**
	 * Traverses the formula tree and finds all LazyLookupFormula nodes, collecting 
	 * them together into a List, which is returned.
	 * @return
	 */
	public int[] getParameterIndicesArray() {
		return m_paramIndicesArray;
	}
	
	protected void shuffleData(int size) {
		Random rgen = new Random(System.currentTimeMillis());
		
		ArrayList<Integer> nums = new ArrayList<Integer>();
		for (int i = 0; i < size; i++) {
			nums.add(i);
		}
		// now, remove all the elements from nums in random order
		while (nums.size() > 0) {
			int next = rgen.nextInt(nums.size());
			m_shuffledIndices.add(nums.get(next));
			nums.remove(next);
		}		
	}

	/**
	 * Creates a new node in the tree that's the next child of this node with the given name. 
	 * @param o Operation for the new formula
	 * @param name Name for the new node
	 * @return a reference to the Formula Object just created
	 */	
	/*public Formula new_arg(Op o, String name) {
		Formula f = new Formula(o, name);
		m_args.add(f);
		if (o == Op.LOOKUP) {
			List<Integer> inds = f.getParameterIndices();
			for (int i = 0; i < inds.size(); i++) {
				m_paramIndices.add(inds.get(i));
			}
		}
		return m_args.get(m_args.size() - 1);
	}*/

	/**
	 * Adds the given node as a child of this node.  
	 * @param f Formula to add as a child
	 */
	/*public void add_arg(Formula f) {
		m_args.add(f);
		List<Integer> inds = f.getParameterIndices();
		for (int i = 0; i < inds.size(); i++) {
			m_paramIndices.add(inds.get(i));
		}
	}*/
	
	/**
	 * Subclasses of RootFormula should override this method to supply methods like evaluate() and 
	 * backprop() with a formula for the next training example. The returned Formula should be 
	 * null if and only if there are no more training examples for the current training iteration. 
	 */
	
	/**
	 * Computes the value of the formula with given values for the parameters.
	 * @return computed value 
	 */
	public LDouble evaluate(LogModel m) {
		if (m_valueComputed) return m_value;
		switch (m_operation) {
		case TIMES:
			m_value = new LDouble(LDouble.IdentityElement.TIMES_IDENTITY);
			int num = m.getNumTrainingExamples();
			for(int k = 0; k < num; k ++)
			{
				LogFormula f = m.getFormula(k);
				LogMath.logtimes(m_value, f.evaluate(m_owner), m_value);
			}
			break;
		case PLUS:
			m_value = new LDouble(LDouble.IdentityElement.PLUS_IDENTITY);
			num = m.getNumTrainingExamples();
			for(int k = 0; k < num; k ++)
			{
				LogFormula f = m.getFormula(k);
				LogMath.logplus(m_value, f.evaluate(m_owner), m_value);
			}
			break;
		case DIVIDE:
			System.out.println("Can't use division with a RootFormula.");
			break;
		case MINUS:
			System.out.println("Can't use minus with a RootFormula.");
			break;
		case NEG:
			System.out.println("Can't use negation with a RootFormula.");
			break;
		case EXP:
			System.out.println("Can't use exp() with a RootFormula.");
			break;
		case LOG:
			System.out.println("Can't use log with a RootFormula.");
			break;			
		case LOOKUP:
			System.out.println("Can't use lookup with a RootFormula.");
			break;
		case CONSTANT:
			System.out.println("Can't use a constant as a RootFormula.");
			break;
		}
		m_valueComputed = true;		
		return m_value;
	}
	
	
	public LDouble evaluateAndBackProp(LogModel m) {
		if (m_valueComputed) return m_value;
		switch (m_operation) {
		case TIMES:
			m_value = new LDouble(LDouble.IdentityElement.TIMES_IDENTITY);
			int num = m.getNumTrainingExamples();
			for(int k = 0; k < num; k ++)
			{
				LogFormula f = m.getFormula(k);
				LogMath.logtimes(m_value, f.evaluate(m_owner), m_value);
			}
			if (m_value.notEqualsZero())
			{
				for(int k = 0; k < num; k ++)
				{
					LogFormula f = m.getFormula(k);
					LDouble temp = new LDouble();
					LogMath.logdivide(m_value, f.evaluate(m), temp);
					f.backprop(m, temp);
				}
			}
			break;
		case PLUS:
			m_value = new LDouble(LDouble.IdentityElement.PLUS_IDENTITY);
			num = m.getNumTrainingExamples();
			for(int k = 0; k < num; k ++)
			{
				LogFormula f = m.getFormula(k);
				LogMath.logplus(m_value, f.evaluate(m_owner), m_value);
				System.out.println(k+" value:"+f.evaluate(m_owner).exponentiate());
				f.backprop(m, new LDouble(LDouble.IdentityElement.TIMES_IDENTITY));
			}
			System.out.println();
			break;
		case DIVIDE:
			System.out.println("Can't use division with a RootFormula.");
			break;
		case MINUS:
			System.out.println("Can't use minus with a RootFormula.");
			break;
		case NEG:
			System.out.println("Can't use negation with a RootFormula.");
			break;
		case EXP:
			System.out.println("Can't use exp() with a RootFormula.");
			break;
		case LOG:
			System.out.println("Can't use log with a RootFormula.");
			break;			
		case LOOKUP:
			System.out.println("Can't use lookup with a RootFormula.");
			break;
		case CONSTANT:
			System.out.println("Can't use a constant as a RootFormula.");
			break;
		}
		m_valueComputed = true;		
		return m_value;
	}
	
	
	
	/**
	 * Propagates gradient down the formula.
	 * @param inc_val 
	 */
	public void backprop(LogModel m, LDouble inc_val) {
		m_gradient = LogMath.logplus(m_gradient, inc_val);
		int i = 0;
		switch (m_operation) {
		case TIMES:
			try {
				// compute/get the product of all the arguments/operands
				evaluate(m); // the product will now be in m_value
				// only proceed if none of the operands equalled zero
				if (m_value.notEqualsZero()) {
					// go through the full set of training data
					LogFormula f = m.getNextFormula();					
					while (f != null) {
						// back-propagate each read-in Formula
						// call backprop on each operand, log-dividing the total product in m_value by the operand
						// divide total product by the current operand's value
						LogMath.logdivide(m_value, f.evaluate(m_owner), m_tempLDouble);
						// multiply the result by the increment value
						LogMath.logtimes(m_tempLDouble, inc_val, m_tempLDouble);
						// call backprop, passing the result
						f.backprop(m_owner, m_tempLDouble);
						if (i % 5000 == 0) System.out.print(i + " ");
						i++;
						f = m.getNextFormula();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(e);
			}								
			break;
		case PLUS:
			// for log-plus-nodes, simply call backprop on each operand
			try {
				// go through full set of training data
				LogFormula f = m.getNextFormula();					
				while (f != null) {
					// back-propagate each phrase pair
					f.backprop(m_owner, inc_val);
					if (i % 5000 == 0) System.out.print(i + " ");
					i++;
					f = m.getNextFormula();
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(e);
			}								
			break;
		case DIVIDE:
			System.out.println("Can't use division with a LazyBigFormula.");
			break;
		case NEG:
			System.out.println("Can't use negation with a LazyBigFormula.");
			break;
		case LOOKUP:
			System.out.println("Can't use lookup with a LazyBigFormula.");
			break;
		}
		m_gradientComputed = true;
	}
	
	
	
	public boolean finishedData() {
		return (m_lastDatumUsed == (m_owner.getNumTrainingExamples() - 1));
	}
	
}
