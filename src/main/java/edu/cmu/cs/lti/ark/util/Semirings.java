/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * Semirings.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.path.Path;
import edu.cmu.cs.lti.ark.util.optimization.LDouble;
import edu.cmu.cs.lti.ark.util.optimization.LogFormula;
import edu.cmu.cs.lti.ark.util.optimization.LogMath;
import edu.cmu.cs.lti.ark.util.optimization.LogModel;
import edu.cmu.cs.lti.ark.util.optimization.LDouble.IdentityElement;
import edu.cmu.cs.lti.ark.util.optimization.LogFormula.Op;

/**
 * Represents operations and semirings.
 * @author Nathan Schneider (nschneid)
 * @since 2010-03-02
 */
public class Semirings {
	public static abstract class Operation<V> {
		public abstract V $(V a, V b);
		public abstract V $(Collection<V> operands);
		public V IDENTITY;
		public V ZERO;
	}
	public static class Add extends Operation<Double> {
		public final Double $(Double a, Double b) {
			return a+b;
		}
		public final Double $(Collection<Double> operands) {
			double v = IDENTITY;
			for (Double operand : operands)
				v += operand;
			return v;
		}
		{ IDENTITY = 0.0; ZERO = Double.POSITIVE_INFINITY; }	// Double.NEGATIVE_INFINITY is another zero
	}
	
	public static class LogAdd extends Operation<Double> {
		public final Double $(Double a, Double b) {
			return logadd(a,b);
		}
		public final Double $(Collection<Double> operands) {
			double v = IDENTITY;
			for (Double operand : operands)
				v = $(v, operand);
			return v;
		}
		public final static Double logadd(Double a, Double b) {
			return LogMath.logplus(new LDouble(a),new LDouble(b)).getValue();
		}
		{ IDENTITY = Double.NEGATIVE_INFINITY; ZERO = Double.NaN; }	// TODO: is there a zero??
	}
	
	public static abstract class PathOperation<T,V> extends WrapOp<V,Set<Pair<V,Path<T>>>> {
		public PathOperation(Operation<V> inner) {
			super(inner);
		}
	}
	
	public static class PathsUnion<T,Q> extends PathOperation<T,Q> {
		public PathsUnion(Operation<Q> innerOp) {
			super(innerOp);
		}
		
		public final Set<Pair<Q,Path<T>>> $(Set<Pair<Q,Path<T>>> a, Set<Pair<Q,Path<T>>> b) {
			List<Set<Pair<Q,Path<T>>>> ab = new ArrayList<Set<Pair<Q,Path<T>>>>();
			ab.add(a);
			ab.add(b);
			return $(ab);
		}
		public final Set<Pair<Q,Path<T>>> $(Collection<Set<Pair<Q,Path<T>>>> operands) {
			Set<Pair<Q,Path<T>>> result = new HashSet<Pair<Q,Path<T>>>();
			for (Set<Pair<Q,Path<T>>> operand : operands)
				result.addAll(operand);	// set union
			return result;
		}
		{ IDENTITY = new HashSet<Pair<Q,Path<T>>>(); ZERO = null; }	// TODO: is there a zero??
	}
	public static class PathsCartesianProduct<T,Q> extends PathOperation<T,Q> {	// Cartesian product of paths in the respective sets
		public PathsCartesianProduct(Operation<Q> inner) {
			this(inner, new Path<T>());
		}
		public PathsCartesianProduct(Operation<Q> inner, Path<T> emptyPath) {
			super(inner);
			IDENTITY.add(new Pair<Q,Path<T>>(_innerOp.IDENTITY, (Path<T>)emptyPath.clone()));	// this way the proper dynamic subtype of Path<T> will be maintained
			ZERO.add(new Pair<Q,Path<T>>(_innerOp.ZERO, (Path<T>)emptyPath.clone()));
		}
		
		public final Set<Pair<Q,Path<T>>> $(Set<Pair<Q,Path<T>>> a, Set<Pair<Q,Path<T>>> b) {
			List<Set<Pair<Q,Path<T>>>> ab = new ArrayList<Set<Pair<Q,Path<T>>>>();
			ab.add(a);
			ab.add(b);
			return $(ab);
		}
		public final Set<Pair<Q,Path<T>>> $(Collection<Set<Pair<Q,Path<T>>>> operands) {
			Set<Pair<Q,Path<T>>> result = new HashSet<Pair<Q,Path<T>>>();
			
			Iterator<Set<Pair<Q, Path<T>>>> iter = operands.iterator();
			Set<Pair<Q,Path<T>>> operand0 = iter.next();
			while (iter.hasNext()) {
				Set<Pair<Q,Path<T>>> operand1 = iter.next();
				
				for (Pair<Q,Path<T>> item0 : operand0) {
					for (Pair<Q,Path<T>> item1 : operand1) {
						Q v = _innerOp.$(item0.getFirst(), item1.getFirst());
						if (!(item0.getSecond() instanceof Path<?>)) {	// TODO: temporary
							item0 = new Pair<Q,Path<T>>(item0.getFirst(), new Path<T>(item0.getSecond()));
						}
						if (!(item1.getSecond() instanceof Path<?>)) {	// TODO: temporary
							item1 = new Pair<Q,Path<T>>(item1.getFirst(), new Path<T>((ArrayList<T>)item1.getSecond()));
						}
						Path<T> concat = item0.getSecond().extend(item1.getSecond().getEnd());	// concatenation of consecutive parts of the path
						result.add(new Pair<Q,Path<T>>(v, concat));
					}
				}
				
				operand0 = operand1;
			}
			return result;
		}
		{ 	IDENTITY = new HashSet<Pair<Q,Path<T>>>(); 
			ZERO = new HashSet<Pair<Q,Path<T>>>();
		}	// TODO: is there a zero??
	}
	
	public static class Mult extends Operation<Double> {
		public final Double $(Double a, Double b) {
			return a*b;
		}
		public final Double $(Collection<Double> operands) {
			double v = IDENTITY;
			for (Double operand : operands)
				v *= operand;
			return v;
		}
		{ IDENTITY = 1.0; ZERO = 0.0; }
	}
	
	public static class Max extends Operation<Double> {
		public final Double $(Double a, Double b) {
			return Math.max(a, b);
		}
		public final Double $(Collection<Double> operands) {
			double v = IDENTITY;
			for (Double operand : operands)
				v = Math.max(v, operand);
			return v;
		}
		{ IDENTITY = Double.NEGATIVE_INFINITY; ZERO = Double.POSITIVE_INFINITY; }
	}
	public static class MaxPath<T> extends PathOperation<T,Double> {
		public MaxPath(Operation<Double> inner) {
			this(inner, new Path<T>());
		}
		public MaxPath(Operation<Double> inner, Path<T> emptyPath) {
			super(inner);
			IDENTITY.add(new Pair<Double,Path<T>>(Double.NEGATIVE_INFINITY,(Path<T>)emptyPath.clone()));	// this way the proper dynamic subtype of Path<T> will be maintained
			ZERO.add(new Pair<Double,Path<T>>(Double.NEGATIVE_INFINITY,null));
		}
		public final Pair<Double,Path<T>> $(Pair<Double,Path<T>> a, Pair<Double,Path<T>> b) {
			return (b.getFirst()>a.getFirst()) ? b : a;
		}
		public final Pair<Double,Path<T>> $(Collection<Pair<Double,Path<T>>> operands) {
			Pair<Double,Path<T>> v = IDENTITY.iterator().next();
			v = new Pair<Double,Path<T>>(v.getFirst(), (Path<T>)v.getSecond().clone());	// this way, the subtype of Path<T> will be maintained from IDENTITY
			for (Pair<Double,Path<T>> operand : operands)
				v = $(v, operand);
			return v;
		}
		public final Set<Pair<Double,Path<T>>> $(Set<Pair<Double,Path<T>>> a, Set<Pair<Double,Path<T>>> b) {
			// TODO: is this right? returns max { max a, max b }
			Set<Pair<Double, Path<T>>> result = new HashSet<Pair<Double, Path<T>>>();
			result.add($($(a), $(b)));
			return result;
		}
		public final Set<Pair<Double,Path<T>>> $(Collection<Set<Pair<Double,Path<T>>>> operands) {
			Set<Pair<Double,Path<T>>> v = IDENTITY;
			for (Set<Pair<Double,Path<T>>> operand : operands)
				v = $(v, operand);
			return v;
		}
		{
			IDENTITY = new HashSet<Pair<Double,Path<T>>>();
			ZERO = new HashSet<Pair<Double,Path<T>>>();
		}
	}
	public static class Min extends Operation<Double> {
		public final Double $(Double a, Double b) {
			return Math.min(a, b);
		}
		public final Double $(Collection<Double> operands) {
			double v = IDENTITY;
			for (Double operand : operands)
				v = Math.min(v, operand);
			return v;
		}
		{ ZERO = Double.NEGATIVE_INFINITY; IDENTITY = Double.POSITIVE_INFINITY; }
	}
	
	public abstract static class Semiring<V> {
		public Operation<V> oplus;
		public Operation<V> otimes;
		public V plus(V a, V b) {
			return oplus.$(a,b);
		}
		public V plus(Collection<V> operands) {
			return oplus.$(operands);
		}
		public V ozero() { return oplus.IDENTITY; }
		public V times(V a, V b) {
			return otimes.$(a,b);
		}
		public V times(Collection<V> operands) {
			return otimes.$(operands);
		}
		public V oone() { return otimes.IDENTITY; }
	}
	
	public static class PlusTimes extends Semiring<Double> {
		{ oplus = new Add(); otimes = new Mult(); }
	}
	
	public static class MaxTimes extends Semiring<Double> {
		{ oplus = new Max(); otimes = new Mult(); }
	}
	
	public static class MaxPlus extends Semiring<Double> {
		{ oplus = new Max(); otimes = new Add(); }
	}
	
	public static class LogPlusTimes extends Semiring<Double> {
		{ oplus = new LogAdd(); otimes = new Add(); }
	}
	
	public static class LogMaxTimes extends Semiring<Double> {
		{ oplus = new Max(); otimes = new Add(); }
	}
	
	public static class LogMaxPlus extends Semiring<Double> {
		{ oplus = new Max(); otimes = new LogAdd(); }
	}
	
	
	/** Semiring on values V whose operations involve a nested call to operations defined in semiring B 
	 * (on subparts of V values)
	 */
	public static class WrapperSemiring<B extends Semiring<?>,V> extends Semiring<V> {
		protected B _baseSemiring;
		public WrapperSemiring(B base) {
			_baseSemiring = base;
			
		}
	}
	
	public abstract static class WrapOp<Q,V> extends Operation<V> {
		protected Operation<Q> _innerOp;
		public WrapOp(Operation<Q> inner) {
			_innerOp = inner;
		}
	}
	
	public abstract static class PathSemiring<T,Q> extends WrapperSemiring<Semiring<Q>,Set<Pair<Q,Path<T>>>> {
		public PathSemiring(Semiring<Q> base) {
			super(base);
		}
	}
	
	public static class PlusTimesPath<T> extends PathSemiring<T,Double> {
		public PlusTimesPath(Semiring<Double> base) {
			super(base);
			oplus = new PathsUnion<T,Double>(_baseSemiring.oplus);
			otimes = new PathsCartesianProduct<T,Double>(_baseSemiring.otimes);
		}
		public PlusTimesPath(Semiring<Double> base, Path<T> emptyPath) {
			super(base);
			oplus = new PathsUnion<T,Double>(_baseSemiring.oplus);
			otimes = new PathsCartesianProduct<T,Double>(_baseSemiring.otimes, (Path<T>)emptyPath.clone());
		}
	}
	
	public static class MaxTimesPath<T> extends PathSemiring<T,Double> {	// TODO: should this be any different from MaxPlusPath?
		public MaxTimesPath(Semiring<Double> base) {
			super(base);
			oplus = new MaxPath<T>(_baseSemiring.oplus);
			otimes = new PathsCartesianProduct<T,Double>(_baseSemiring.otimes);
		}
		public MaxTimesPath(Semiring<Double> base, Path<T> emptyPath) {
			super(base);
			oplus = new MaxPath<T>(_baseSemiring.oplus, (Path<T>)emptyPath.clone());
			otimes = new PathsCartesianProduct<T,Double>(_baseSemiring.otimes, (Path<T>)emptyPath.clone());
		}
	}
	
	public static class MaxPlusPath<T> extends PathSemiring<T,Double> {	// TODO: should this be any different from MaxTimesPath?
		public MaxPlusPath(Semiring<Double> base) {
			super(base);
			oplus = new MaxPath<T>(_baseSemiring.oplus);
			otimes = new PathsCartesianProduct<T,Double>(_baseSemiring.otimes);
		}
		public MaxPlusPath(Semiring<Double> base, Path<T> emptyPath) {
			super(base);
			oplus = new MaxPath<T>(_baseSemiring.oplus, (Path<T>)emptyPath.clone());
			otimes = new PathsCartesianProduct<T,Double>(_baseSemiring.otimes, (Path<T>)emptyPath.clone());
		}
	}
	
	public static abstract class LogFormulaBuilder extends Semiring<LogFormula> {
		public abstract LogFormula recruitFormula(LogFormula.Op op);
		public abstract LogFormula recruitFormula(IdentityElement ie);
		public abstract class LogFormulaOp extends Operation<LogFormula> {
			protected LogFormula.Op _op;
			protected boolean _associative = false;
			protected boolean _ignoreIdentity = true;
			protected boolean _modifiesSingleArgument = true;
			public final LogFormula $(LogFormula a, LogFormula b) {
				if (false) return null; 
				/*if (_associative && a.getOperation()==_op && b.getOperation()!=_op && a!=b && a.getInDegree()==0) {
					// attach b under a and return a
					assert a.getInDegree()==0;	// TODO: necessary?
					if (b.getOperation()==_op) {
						assert b.getInDegree()==0;
						for (LogFormula barg : b) {
							barg.reduceInDegree();
							a.add_arg(barg);
						}
					}
					else
						a.add_arg(b);
					return a;
				}*/
				else if (_associative && b.getOperation()==_op && a.getOperation()!=_op && a!=b && b.getInDegree()==0) {
					// attach a under b and return b
					assert b.getInDegree()==0;
					b.add_arg(a);
					return b;
				}
				else if (!_modifiesSingleArgument && _ignoreIdentity && a==IDENTITY) {
					assert a.getInDegree()==0;
					return b;
				}
				else if (!_modifiesSingleArgument && _ignoreIdentity && b==IDENTITY) {
					assert b.getInDegree()==0;
					return a;
				}
				else {
					// create a new node with a and b as arguments
					LogFormula f = recruitFormula(_op);
					if (!_ignoreIdentity || a!=IDENTITY)	// include a unless it's the identity
						f.add_arg(a);
					if (!_ignoreIdentity || b!=IDENTITY)	// include b unless it's the identity
						f.add_arg(b);
					if (_ignoreIdentity && a==IDENTITY && b==IDENTITY)
						f.add_arg(IDENTITY);	// ensure there's at least one argument
					return f;
				}
			}
			public final LogFormula $(Collection<LogFormula> operands) {
				LogFormula f = recruitFormula(_op);
				for (LogFormula operand : operands)
					f.add_arg(operand);
				return f;
			}
		}
		public class LogFormulaPlus extends LogFormulaOp {
			{ _op = LogFormula.Op.PLUS; _associative = true; _modifiesSingleArgument = false; IDENTITY = recruitFormula(IdentityElement.PLUS_IDENTITY); ZERO = null; }	// TODO: is a ZERO necessary in practice?
		}
		public class LogFormulaTimes extends LogFormulaOp {
			{ _op = LogFormula.Op.TIMES; _associative = true; _modifiesSingleArgument = false; IDENTITY = recruitFormula(IdentityElement.TIMES_IDENTITY); ZERO = null; }	// TODO: is a ZERO necessary in practice?
		}
		
		{ oplus = new LogFormulaPlus(); otimes = new LogFormulaTimes(); }
	}
	
}
