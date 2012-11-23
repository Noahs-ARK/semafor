/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * LogFormula.java is part of SEMAFOR 2.0.
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

import java.io.Serializable;
import java.util.*;

import edu.cmu.cs.lti.ark.util.optimization.LDouble.IdentityElement;

/**
 * Class to represent a function. Format is a tree structure, in which leaf nodes are 
 * table look-ups for parameter values and non-leaf nodes are operations, such as 
 * TIMES, PLUS, DIVIDE, etc. Every node of this "formula tree" is an instance of a 
 * Formula Object. 
 *  
 * @author Kevin Gimpel, Noah Smith
 * 2/21/07
 */
public class LogFormula implements Serializable, Iterable<LogFormula> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3967927293400355614L;
	public enum Op { PLUS, MINUS, TIMES, DIVIDE, NEG, EXP, LOG, POWER, LOOKUP, CONSTANT }

	protected String m_name;
	/**
	 * The operation to be performed at this node
	 */
	protected Op m_operation;
	/**
	 * The terms (children) in the formula
	 */
	protected List<LogFormula> m_args;
	/**
	 * The computed value, if m_valueComputed == true
	 */
	protected LDouble m_value;
	/**
	 * Indicates whether the value of this formula has been computed
	 */
	protected boolean m_valueComputed;
	/**
	 * The gradient of this formula's parent with respect to this formula 
	 */
	protected LDouble m_gradient;
	/**
	 * Indicates whether the gradient of this formula has been computed
	 */
	protected boolean m_gradientComputed;

	protected LDouble m_tempLDouble;

		
	protected LDouble accumulatedGradient;

	protected int inDegrees;
	
	private boolean _minified = false;
	
	private final int _creation_index;
	
	private static int _n_instances = 0;
	
	
	/** Constructors **/
	public LogFormula(Op o, String name) {
		m_name = name;
		m_operation = o;
		m_valueComputed = false;
		m_value = new LDouble(LDouble.IdentityElement.PLUS_IDENTITY);	// initialize as plus-identity element
		_creation_index = _n_instances;
		initCommon();
	}
	public LogFormula(Op o) {
		m_name = null;
		m_operation = o;
		m_valueComputed = false;
		m_value = new LDouble(LDouble.IdentityElement.PLUS_IDENTITY);	// initialize as plus-identity element
		_creation_index = _n_instances;
		initCommon();
	}
	public LogFormula(LDouble v, String name) {
		m_name = name;
		m_operation = Op.CONSTANT;
		m_valueComputed = true;
		m_value = new LDouble(v);
		_creation_index = _n_instances;
		initCommon();
	}
	public LogFormula(LDouble v) {
		m_name = null;
		m_operation = Op.CONSTANT;
		m_valueComputed = true;
		m_value = new LDouble(v);
		_creation_index = _n_instances;
		initCommon();
	}
	public LogFormula(IdentityElement ie) {
		m_name = null;
		m_operation = Op.CONSTANT;
		m_valueComputed = true;
		m_value = new LDouble(ie);
		_creation_index = _n_instances;
		initCommon();
	}
	public LogFormula(double v) {
		m_name = null;
		m_operation = Op.CONSTANT;
		m_valueComputed = true;
		m_value = new LDouble(v);
		_creation_index = _n_instances;
		initCommon();
	}
	private void initCommon() {
		m_args = new ArrayList<LogFormula>();
		m_gradientComputed = false;
		m_gradient = new LDouble(LDouble.IdentityElement.PLUS_IDENTITY); // initialize as plus-identity element
		m_tempLDouble = new LDouble();
		accumulatedGradient = new LDouble(LDouble.IdentityElement.PLUS_IDENTITY);
		inDegrees = 0;
		
		
		_n_instances++;
		if (Math.log10(_n_instances)==Math.floor(Math.log10(_n_instances)))	// _n_instances is a power of 10
			System.err.println(String.format("LogFormula: %d instances",_n_instances));
	}
		

	/** Reset functions **/
	public void reset(Op o, String name) {
		reset(o);
		m_name = name;
	}
	public void reset(Op o) {
		m_name = null;
		m_operation = o;
		m_valueComputed = false;
		m_value.reset();	// reset to plus-identity element
		resetCommon();
	}
	public void reset(LDouble v, String name) {
		reset(v);
		m_name = name;
	}
	public void reset(LDouble v) {
		m_name = null;
		m_operation = Op.CONSTANT;
		m_valueComputed = true;
		m_value.reset(v);	// reset to given value
		resetCommon();
	}
	public void reset(IdentityElement ie) {
		m_name = null;
		m_operation = Op.CONSTANT;
		m_valueComputed = true;
		m_value.reset(ie);	// reset to given value
		resetCommon();
	}
	public void reset(double v) {
		m_name = null;
		m_operation = Op.CONSTANT;
		m_valueComputed = true;
		m_value.reset(v);	// reset to given value
		resetCommon();
	}
	private void resetCommon() {
		m_gradientComputed = false;
		m_gradient.reset(); // initialize as plus-identity element
		m_args.clear();
		accumulatedGradient = new LDouble(LDouble.IdentityElement.PLUS_IDENTITY);
		inDegrees = 0;
	}
	
	
	/**
	 * Creates a new node in the tree that's the next child of this node. 
	 * @param o Operation for the new formula
	 * @return a reference to the Formula Object just created
	 */	
	public LogFormula new_arg(Op o) {
		m_args.add(new LogFormula(o));
		return m_args.get(m_args.size() - 1);
	}

	/**
	 * Creates a new node in the tree that's the next child of this node with the given name. 
	 * @param o Operation for the new formula
	 * @param name Name for the new node
	 * @return a reference to the Formula Object just created
	 */	
	public LogFormula new_arg(Op o, String name) {
		m_args.add(new LogFormula(o, name));
		return m_args.get(m_args.size() - 1);
	}
	
	/**
	 * Adds the given node as a child of this node.  
	 * @param f Formula to add as a child
	 */
	public void add_arg(LogFormula f) {
		m_args.add(f);
		f.incrementInDegree();
	}	
	
	public void finalize() {
		for (int i = 0; i < m_args.size(); i++) {
			m_args.get(i).finalize();
		}
	}

	/**
	 * Since we changed the parameter values, resets flags indicating that 
	 * we've already computed function and gradient values. 
	 *
	 */
	public void changedParamValues() {
		// reset isComputed flags
		m_valueComputed = false;
		m_gradientComputed = false;
		accumulatedGradient = new LDouble(LDouble.IdentityElement.PLUS_IDENTITY);
		inDegrees = 0;
		//System.out.println(m_args.size());
		for (int i = 0; i < m_args.size(); i++) {
			m_args.get(i).changedParamValues();
		}
	}
		
	/**
	 *  For lookup formulas, goes into the appropriate data structure and returns the value. 
	 */
	LDouble lookup_evaluate(LogModel m) {
		System.out.println("can't call lookup_evaluate on a non-lookup formula! Returning null.");
		return null;
	}
	void lookup_backprop(LogModel m, LDouble ld) {
		System.out.println("can't call lookup_backprop on a non-lookup formula!");
	}
	void lookup_backpropLogValues(LogModel m, LDouble ld) {
		System.out.println("can't call lookup_backpropLogValues on a non-lookup formula!");
	}

	
	public Op getOperation() {
		return m_operation;
	}
	

	/*
	 * indegree related operations
	 */
	public int getInDegree(){
		return this.inDegrees;
	}
	
	public void reduceInDegree()
	{
		if(m_operation!=Op.LOOKUP&&m_operation!=Op.CONSTANT)
			this.inDegrees--;
	}

	public void incrementInDegree()
	{	
		if(m_operation!=Op.LOOKUP&&m_operation!=Op.CONSTANT)
			this.inDegrees++;
	}
	
	/*
	 * adding to the accumulated gradient
	 * 
	 */
	public void addGradient(LDouble gradientToAdd)
	{
		accumulatedGradient = LogMath.logplus(accumulatedGradient, gradientToAdd);  
	}
	
	
	/**
	 * Computes the value of the formula with given values for the parameters.
	 * @return computed value 
	 */
	public LDouble evaluate(LogModel m) {
		if (m_valueComputed) return m_value;
		int n = m_args.size(), i;
		switch (m_operation) {
		case POWER:
			LogMath.logpower(m_args.get(0).evaluate(m), m_args.get(1).evaluate(m), m_value);
			m_valueComputed = true;
			break;
		case TIMES:
			// initialize m_value to the multiplicative identity element
			m_value.reset(LDouble.IdentityElement.TIMES_IDENTITY);
			// evaluate each operand and log-multiply onto m_value, placing the result into m_value
			for (i = 0; i < n; i++) {
				LogMath.logtimes(m_value, m_args.get(i).evaluate(m), m_value);
			}
			m_valueComputed = true;
			break;
		case PLUS:
			// if there are more than k terms in the sum and this Formula node has a name, check 
			// the table of saved values
			if (m_name != null) {
				if (m.savedValues.containsKey(m_name)) {
					double v = m.savedValues.get(m_name); 
					m_value.reset(v);
				} else {
					// initialize m_value to the plus identity element
					m_value.reset(LDouble.IdentityElement.PLUS_IDENTITY);
					// evaluate each operand and log-add to m_value
					for (i = 0; i < n; i++) {
						LogMath.logplus(m_value, m_args.get(i).evaluate(m), m_value);
					}
					// save the result under the given name
					m.savedValues.put(m_name, m_value.value);
				}
			} else {
				// reset m_value to the plus identity element
				m_value.reset(LDouble.IdentityElement.PLUS_IDENTITY);
				// evaluate each operand and log-add to m_value
/*if (this.hasCycles()) {
	System.err.println("CYCLES");
	System.exit(1);
}*/
				for (i = 0; i < n; i++) {
					try {
						LogMath.logplus(m_value, m_args.get(i).evaluate(m), m_value);
					}
					catch (StackOverflowError ex) {
						System.out.println(this.treeToString(m, false, 20));
						System.exit(1);
					}
				}
			}
			m_valueComputed = true;
			break;
		case DIVIDE:
			// log-divide the first operand by the second, placing result into m_value
			// if m_args.size() > 2, ignores remaining operands
			LogMath.logdivide(m_args.get(0).evaluate(m), m_args.get(1).evaluate(m), m_value);
			m_valueComputed = true;
			break;
		case EXP:
			// exponentiate the argument (assumed to be only one), placing result into m_value
			LogMath.logexp(m_args.get(0).evaluate(m), m_value);
			m_valueComputed = true;
			break;
		case LOG:
			// take the log of the argument (assumed to be only one, but not checked)
			// if the argument is negative, prints a warning and changes it to positive.. does this make sense?
			m_tempLDouble = m_args.get(0).evaluate(m);
			if (!m_tempLDouble.isPositive()) {
				System.out.println("Tried to take the log of a negative number");
			}
			LDouble.convertToLogDomain(m_tempLDouble.value, m_value);
			m_valueComputed = true;
			break;
		case NEG:
			// todo: implement negation
			break;
		case LOOKUP:
			try {
				// look up the value in a global table for the model (see LazyLookupFormula)
				m_value = lookup_evaluate(m);
			} catch (Exception e) {
				System.out.println(e.toString());
				e.printStackTrace();
			}
			break;
		case CONSTANT:
			// do nothing; i.e., simply return m_value
			break;
		}
		return m_value;
	}
	
	/**
	 * Computes the gradient of the formula with respect to the values of the parameters.
	 * @return computed gradient 
	 */
	public LDouble getGradientWithRespectToValues(LogModel m) {
		if (m_gradientComputed) {
			return m_gradient;
		} else {
			backprop(m, new LDouble(LDouble.IdentityElement.TIMES_IDENTITY));
			m_gradientComputed = true;
			return m_gradient;
		}
	}
	

	/**
	 * Propagates gradient down the formula.
	 * @param accumulatedGradient 
	 */
	public void backprop(LogModel m, LDouble inc_val) {
		LogMath.logplus(m_gradient, inc_val, m_gradient);
		this.addGradient(inc_val);
		reduceInDegree();
		int n = m_args.size(), i;
		switch (m_operation)
		{
		case POWER:
			if(inDegrees>0)
				return;
			// to do this operation, the sign of t1 MUST be positive
			LDouble t1 = m_args.get(0).evaluate(m);
			LDouble t2 = m_args.get(1).evaluate(m);
			if (!t1.isPositive()) {
				System.out.println("Sign of base x in x^y is negative!");
			}
			LDouble temp = new LDouble();
			LogMath.logtimes(t2, LogMath.logpower(t1, LogMath.logminus(t2, new LDouble(LDouble.IdentityElement.TIMES_IDENTITY))), temp);
			m_args.get(0).backprop(m, LogMath.logtimes(temp, accumulatedGradient));
			if (m_valueComputed) {
				LDouble.convertToLogDomain(t1.value * m_value.exponentiate(), temp);
				m_args.get(1).backprop(m, LogMath.logtimes(temp, accumulatedGradient));
			} else {
				LDouble.convertToLogDomain(t1.value * (LogMath.logpower(t1, t2).exponentiate()), temp);
				m_args.get(1).backprop(m, LogMath.logtimes(temp, accumulatedGradient));				
			}
			break;
		case TIMES:
			if(inDegrees>0)
				return;
			evaluate(m);
			if (m_value.notEqualsZero()) {
				// call backprop on each operand, log-dividing the total product in m_value by the operand
				for (i = 0; i < n; i++) {
					LogFormula arg = m_args.get(i);
					// divide total product by the current operand's value
					m_tempLDouble = LogMath.logdivide(m_value, arg.evaluate(m));
					// multiply the result by the increment value
					m_tempLDouble = LogMath.logtimes(m_tempLDouble, accumulatedGradient);
					// call backprop, passing the result						
					arg.backprop(m, m_tempLDouble);
				}
			}
			else
			{
				for (i = 0; i < n; i++) {
					LogFormula arg = m_args.get(i);
					// call backprop, passing zero					
					arg.backprop(m, new LDouble(LDouble.IdentityElement.PLUS_IDENTITY));
				}
			}
			break;
		case PLUS:
			if(inDegrees>0)
				return;
			// for log-plus-nodes, simply call backprop on each operand
			for (i = 0; i < n; i++) {
				m_args.get(i).backprop(m, accumulatedGradient);
			}			
			break;
		case DIVIDE:
			if(inDegrees>0)
				return;
			t1 = m_args.get(0).evaluate(m);
			t2 = m_args.get(1).evaluate(m);
			// the derivative of the root wrt the left child is (1/t2) (times the increment value)
			m_tempLDouble = LogMath.logdivide(accumulatedGradient, t2);
			m_args.get(0).backprop(m, m_tempLDouble);   
			// the derivative of the root wrt the right child is -t1/(t2^2) (times the increment value)
			LDouble tempLDouble1 = new LDouble(t1.value,!t1.sign);
			LDouble tempLDouble2 = LogMath.logtimes(t2, t2);
			// now tempLDouble2 holds (t2^2)
			tempLDouble2 = LogMath.logdivide(accumulatedGradient, tempLDouble2);
			// now tempLDouble2 holds (accumulatedGradient / (t2^2))
			// multiply it by -t1
			tempLDouble1 = LogMath.logtimes(tempLDouble1, tempLDouble2);
			// backprop
			m_args.get(1).backprop(m, tempLDouble1);
			break;
		case EXP:
			if(inDegrees>0)
				return;
			LDouble product = LogMath.logtimes(m_value, accumulatedGradient);
			m_args.get(0).backprop(m, product);
			break;
		case LOG:
			if(inDegrees>0)
				return;
			LogFormula arg = m_args.get(0);
			LDouble val = arg.evaluate(m);
			m_tempLDouble = LogMath.logdivide(accumulatedGradient,val);
			arg.backprop(m, m_tempLDouble);
			break;			
		case NEG:
			if(inDegrees>0)
				return;
			m_args.get(0).backprop(m, new LDouble(accumulatedGradient.value, !accumulatedGradient.sign));
			break;
		case LOOKUP:
			try {
				lookup_backprop(m, inc_val);
			} catch (Exception e) {
				System.out.println(e.toString());
				e.printStackTrace();
			}
			break;
		case CONSTANT:
			// we don't need to do anything here
			break;
		}
		m_gradientComputed = true;
	}
	
	public void checkInDegrees()
	{
		if(inDegrees>0)
		{
			System.out.println("Problem. Indegree greater than zero");
			System.exit(0);
		}
		System.out.println("Ok");
		for(LogFormula f: m_args)
			f.checkInDegrees();
	}
	
	
	/**
	 * Traverses the formula tree and finds all LazyLookupFormula nodes, collecting 
	 * them together into a List, which is returned.
	 * @return
	 */
	public List<LazyLookupLogFormula> getParameters() {
		List<LazyLookupLogFormula> ret = new ArrayList<LazyLookupLogFormula>();
		getParametersAux(ret);
		return ret;
	}

	void getParametersAux(List<LazyLookupLogFormula> runningList) {
		for (int i = 0; i < m_args.size(); i++) {
			m_args.get(i).getParametersAux(runningList);
		}
	}


	void getParameterIndicesAux(List<Integer> runningList,List<String> sList) {
		int size = m_args.size();
		for (int i = 0; i < size; i++) {
			LogFormula f = m_args.get(i);
			String name = f.m_name;
//			if(name!=null)
//				System.out.println(name+"\t"+getStringForOp(f.m_operation));
			if(name!=null&&sList.indexOf(name)>=0)
			{
				//System.out.println("Found name:"+name);
				continue;
			}
			sList.add(name);
			f.getParameterIndicesAux(runningList,sList);
		}
	}

	/**
	 * Traverses the formula tree and finds all softmax Formula nodes, collecting 
	 * them together into a List, which is returned.
	 * todo: do this a better way, by subclassing Formula with a SoftmaxFormula class?
	 * @return
	 */
	public List<LogFormula> getSoftmaxParameters() {
		List<LogFormula> ret = new ArrayList<LogFormula>();
		getSoftmaxParametersAux(ret);
		return ret;
	}

	void getSoftmaxParametersAux(List<LogFormula> runningList) {
		if (m_operation == Op.DIVIDE && m_name.startsWith("softmax_")) {
			runningList.add(this);
			//return runningList;
		} else {
			for (int i = 0; i < m_args.size(); i++) {
				m_args.get(i).getSoftmaxParametersAux(runningList);
			}
		}
	}
	
	/**
	 * Traverses the formula tree and finds all softmax Formula nodes, collecting 
	 * their indices together into a List, which is returned.
	 * @return
	 */
	public List<Integer> getSoftmaxParameterIndices(LogModel m) {
		List<Integer> ret = new ArrayList<Integer>();
		getSoftmaxParameterIndicesAux(ret, m);
		return ret;
	}

	void getSoftmaxParameterIndicesAux(List<Integer> runningList, LogModel m) {
		if (m_operation == Op.DIVIDE && m_name.startsWith("softmax_")) {
			try {
				if (m_args.size() > 0) {
					runningList.add(m.A.getInt(m_name));
				}
			} catch (Exception e) {
				System.out.println(e.toString());
				e.printStackTrace();
			}
		} else {
			for (int i = 0; i < m_args.size(); i++) {
				m_args.get(i).getSoftmaxParameterIndicesAux(runningList, m);
			}
		}
	}

	public void printFormulaTree(int numtabs) {
		for (int i = 0; i < numtabs; i++) {
			System.out.print("\t");
		}
		System.out.println(m_name + "\t" + m_value + "\t\t" + m_gradient);
		for (int i = 0; i < m_args.size(); i++) {
			m_args.get(i).printFormulaTree(numtabs + 1);
		}
	}
	
	public String toString() {
		return "name=" + m_name + "\tval=" + m_value + "\tgrad=" + m_gradient; 
	}
	public String treeToString(LogModel m) {
		return treeToString(m, false, -1);
	}
	public String treeToString(LogModel m, boolean showFeatureNames, int remainingDepth) {
		StringBuffer sb = new StringBuffer();
		sb.append("(" + getStringForOp(m_operation) + " ");
		if (remainingDepth>0 || remainingDepth==-1) {
			for (int i = 0; i < m_args.size(); i++) {
				sb.append(m_args.get(i).treeToString(m, showFeatureNames, remainingDepth-1));
				if (i < (m_args.size()-1)) sb.append(" ");
			}
		}
		else
			sb.append("...");
		sb.append(")");
		return sb.toString();
	}
	private String getStringForOp(Op op) {
		switch (op) {
		case TIMES:
			return "*";
		case PLUS:
			return "+";			
		case DIVIDE:
			return "/";
		case NEG:
			return "-";			
		case EXP:
			return "e^";
		case LOG:
			return "log";
		case LOOKUP:
			return "lookup";
		case CONSTANT:
			return "" + m_value.exponentiate();
		case POWER:
			return "^";		
		}
		return "null";
	}
	
	/**
	 * @return The length of the longest path starting at this node
	 */
	private int _ht = -1;
	public int nodeHeight() {
		if (_ht>-1)
			return _ht;
		int h = 0;
		for (LogFormula c : m_args) {
			h = Math.max(h, c.nodeHeight()+1);
		}
		_ht = h;
		return h;
	}
	public void resetHeights(int newVal) {	// Reset _ht property for the subtree headed by this node. Should be a negative value.
		if (_ht==newVal)
			return;
		_ht = newVal;
		for (LogFormula c : this)
			c.resetHeights(newVal);
	}
	
	/**
	 * Removes unnecessary nodes in the formula tree (for example, multiplication by constant 1.0) to save memory.
	 */
	public void minify(LogModel m, boolean cycleCheck) {
		if (0==0)
			return;
		
		if (_minified)
			return;
		
		if (false && cycleCheck) {
			List<LogFormula> listWithThis = new LinkedList<LogFormula>();
			listWithThis.add(this);
			int c = hasCycles(listWithThis);
			if (c>0)	{	// Check that there are no cycles
				System.err.println("AARRRRGH CYCLES "+Integer.toString(c));
				System.exit(1);
			}
			else
				System.out.println("ok");
		}
		
		
		
		assert (this instanceof RootLogFormula) || 
			this.m_operation==Op.LOG || this.m_operation==Op.LOOKUP || this.m_operation==Op.CONSTANT || 
			this.getInDegree()>0;	// TODO: LOG is our top-level formula for a given training instance
		
		for (LogFormula c : m_args) {
			if (c.m_args.size()==1) {	// child + or * node has only one argument
				LogFormula d = c.m_args.get(0);
				if ((c.m_operation==Op.PLUS || c.m_operation==Op.TIMES) 
						&& c.getInDegree()<2 && d.getInDegree()<2) {
					// replace child + or * node with its argument
					this.m_args.set(this.m_args.indexOf(c), d);
					c.reduceInDegree();
				}
			}
		}
		
		
		if (m_operation==Op.TIMES) {
			stripChildren(IdentityElement.TIMES_IDENTITY);
		}
		else if (m_operation==Op.PLUS) {
			stripChildren(IdentityElement.PLUS_IDENTITY);
		}
		
		// Flatten where this and its child are the same operation
		List<LogFormula> toadd = new ArrayList<LogFormula>();
		if (m_operation==Op.PLUS || m_operation==Op.TIMES) {
			for (LogFormula c : m_args) {
				if (c.m_operation==m_operation && c.getInDegree()<2) {
					toadd.addAll(c.m_args);
					this.m_args.remove(c);
					c.reduceInDegree();
				}
			}
			this.m_args.addAll(toadd);
		}
		
		if (false && cycleCheck) {
			List<LogFormula> listWithThis = new LinkedList<LogFormula>();
			listWithThis.add(this);
			if (hasCycles(listWithThis)>0)	{	// Check that there are no cycles
				System.err.println("AARRRRGH CYCLES");
				System.exit(1);
			}
			else
				System.out.println("OK");
		}
		
		_minified = true;
	}
	private int hasCycles(List<LogFormula> queued) {
		// TODO: WARNING: This method may be buggy and/or slow!
		/*System.out.println(depth);
		if (this._cycleSeen)
			return true;
		this._cycleSeen = true;
		for (LogFormula c : m_args) {
			if (c.hasCycles(depth+1))
				return true;
		}
		this._cycleSeen = false;
		return false;*/
		
		// Topological sort
		// Add all descendants to the queue (breadth-first traversal)
		int i=0;
		
		Map<Integer,Set<Integer>> parentsMap = new HashMap<Integer,Set<Integer>>();
		while (i<queued.size()) {
			LogFormula n = queued.get(i);
			if (!parentsMap.containsKey(n))
				parentsMap.put(n._creation_index, new HashSet<Integer>());
			for (LogFormula c : n.m_args) {
				if (c==n)
					return 1;
				if (!parentsMap.containsKey(c))
					parentsMap.put(c._creation_index, new HashSet<Integer>());
				parentsMap.get(c._creation_index).add(n._creation_index);
				if (!queued.contains(c)) {
					queued.add(c);
				}
			}
			i++;
		}
		
		System.out.println("part1 done");
		
		// Reverse the list and work backward from the end
		Collections.reverse(queued);
		
		System.out.println("reversal done");
		
		Collection<Integer> sorted = new ArrayList<Integer>();	// new ArrayList<Integer>(); if we really cared about the sort order
		while (!queued.isEmpty()) {
			int j = queued.size()-1;
			while (j>=0) {
				LogFormula n = queued.get(j);
				boolean allParentsSorted = true;
				for (int p : parentsMap.get(n._creation_index)) {	// n's parents
					if (!sorted.contains(p)) {
						allParentsSorted = false;
						break;
					}
				}
				if (allParentsSorted) {
					int newestSorted = queued.remove(j)._creation_index;
					sorted.add(newestSorted);
					parentsMap.remove(newestSorted);
					break;
				}
				j--;
			}
			if (j==-1) {
				System.out.println(parentsMap);
				System.out.println(sorted.size());
				System.out.println(queued.size());
				for (LogFormula q : queued)
					System.out.print(String.format("%d ", q._creation_index));
				return 2;
			}
		}
		return 0;
	}
	
	protected void stripChildren(IdentityElement ie) {
		for (LogFormula c : m_args) {
			if (m_args.size()>1 && c.getInDegree()<2 
					&& c.m_operation==Op.CONSTANT && c.m_value.getValue()==new LDouble(ie).getValue())
				this.m_args.remove(c);
		}
	}
	
	/** Iterate over arguments to the operation represented by this formula (child nodes in the graph) */
	@Override
	public Iterator<LogFormula> iterator() {
		return this.m_args.iterator();
	}
	
}

