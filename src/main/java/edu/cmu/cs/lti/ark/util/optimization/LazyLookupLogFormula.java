/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * LazyLookupLogFormula.java is part of SEMAFOR 2.0.
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

import java.util.List;

import edu.cmu.cs.lti.ark.util.optimization.LogFormula.Op;

/**
 * Leaf node of the formula graph, for representing a parameter.
 * 
 * 2010-11-08 (nschneid): Added support for multithreading wherein each training example gets its own thread. 
 * Only {@link LazyLookupLogFormula#add_arg(LogFormula)} nodes may be shared by multiple training examples.
 * {@link #backprop(LogModel, LDouble)} and {@link #lookup_backprop(LogModel, LDouble)} are thus 
 * synchronized.
 * @see RootLogFormula#RootLogFormula(LogModel, Op, String, boolean) 
 */
public class LazyLookupLogFormula extends LogFormula {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1695010089287337618L;
	protected int m_index;
	private LDouble m_tempLDouble3 = new LDouble();
	
	public String toString(LogModel m) {
		return "name=" + m_name + "\tindex=" + m_index + "\tval=" + m.getValue(m_index) + "\tgrad=" + m.getGradient(m_index); 
	}
	
	public LazyLookupLogFormula(int i, String name) {
		super(LogFormula.Op.LOOKUP, name);
		//addParametertoSet(i);
		m_index = i;
	}
	public LazyLookupLogFormula(int i) {
		super(LogFormula.Op.LOOKUP);
		//addParametertoSet(i);
		m_index = i;
	}
	
	/**
	 * DON'T reset m_value or m_gradient since these may be references to elements in the Model's table.
	 * Instead, we'll just replace the references in lookup_evaluate and lookup_backprop below.
	 * @param i
	 * @param name
	 */
	public void reset(int i, String name) {
		reset(LogFormula.Op.LOOKUP, name);
		m_index = i;
	}
	public void reset(int i) {
		reset(LogFormula.Op.LOOKUP);
		m_index = i;
	}
	
	@Override
	synchronized LDouble lookup_evaluate(LogModel m) {	// needs to be synchronized because this will be shared across multiple training examples
		// get a reference to the parameter value
		LDouble ret = m.getValue(m_index); 
		// if it was found,
		if (ret != null) {
			// set m_value to the same value as the one we found
			m_value.reset(ret);
			// return m_value
			return m_value;
		} else {
			System.out.println("Null LDouble value encountered for param " + m_index + ", " + m.A.getString(m_index));
			// reset m_value and return it
			m_value.reset();
			return m_value;
		}
	}

	@Override
	void getParametersAux(List<LazyLookupLogFormula> runningList) {
		runningList.add(this);
	}
	
	@Override
	void getParameterIndicesAux(List<Integer> runningList,List<String> sList) {
		if(runningList.indexOf(m_index)<0)
			runningList.add(m_index);
	}
	
	@Override
	public synchronized void backprop(LogModel m, LDouble inc_val) {	// synchronized since this formula may be shared by multiple data points
		super.backprop(m, inc_val);
	}
	
	@Override
	synchronized void lookup_backprop(LogModel m, LDouble inc_val) {	// synchronized since this formula may be shared by multiple data points
		//System.out.print("Calling subclass's lookup_backprop, index = " + m_index + ", gradient = ");
		
		/*LDouble currentGradient = m.getGradient(m_index);
		// add inc_val "in place"
		LogMath.logplus(currentGradient, inc_val, currentGradient);
		m_gradient = currentGradient;*/

		LDouble currentGradient = m.getGradient(m_index);
		if (currentGradient != null) {
			// add inc_val "in place"
			LogMath.logplus(currentGradient, inc_val, currentGradient);
			//LDouble newGradient = LogMath.logplus(currentGradient, inc_val);
			//m.setGradient(m_index, newGradient);
			m_gradient.reset(currentGradient);
			//m_gradient = newGradient;
		} else {
			//System.out.println("Null LDouble gradient encountered for param " + m_index);
			LDouble newGradient = new LDouble(inc_val);
			//LogMath.logplus(newGradient, inc_val, newGradient);
			//LDouble newGradient = LogMath.logplus(new LDouble(), inc_val);
			//m.initGradient(m_index, newGradient);
			m.setGradient(m_index, newGradient);
			m_gradient.reset(newGradient);
			//m_gradient = newGradient;
		}
		
		//System.out.print("" + m_owner.getGradient(m_index));
		//System.out.println();
		//m_owner.gradient(index) = logplus(owner->gradient(index), inc_val); 
	}

	@Override
	void lookup_backpropLogValues(LogModel m, LDouble inc_val) {
		if (0==0)
			throw new RuntimeException("LazyLookupLogFormula.lookup_backpropLogValues(): Unsupported? Not sure if this is obsolete. -nschneid");
		/*LDouble currentGradient = m.getGradient(m_index);
		// add inc_val "in place"
		LogMath.logplus(currentGradient, inc_val, currentGradient);
		m_gradient = currentGradient;*/

		LDouble currentGradient = m.getGradient(m_index);
		if (currentGradient != null) {
			LDouble val = m.getValue(m_index);
			LogMath.logtimes(inc_val, val, m_tempLDouble3);
			// add inc_val "in place"
			LogMath.logplus(currentGradient, m_tempLDouble3, currentGradient);
			m_gradient.reset(currentGradient);
		} else {
			//System.out.println("Null LDouble gradient encountered for param " + m_index);
			LDouble newGradient = new LDouble();
			LDouble val = m.getValue(m_index);
			LogMath.logtimes(inc_val, val, newGradient);
			m.setGradient(m_index, newGradient);
			m_gradient.reset(newGradient);
		}
		//System.out.print("" + m_owner.getGradient(m_index));
		//System.out.println();
		//m_owner.gradient(index) = logplus(owner->gradient(index), inc_val); 
	}
	
	@Override
	public String treeToString(LogModel m, boolean showFormulas, int dummy) {
		if (!showFormulas)
			return ("" + m.getValue(m_index).exponentiate());
		return ("" + m.getValue(m_index).exponentiate() + " " + m.getParamString(m_index));
	}
}
