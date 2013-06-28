/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * Counter.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.util.ds.map;

import com.google.common.collect.Maps;
import gnu.trove.*;

import java.util.*;

/**
 * Simple double counter: stores integer values for keys; lookup on nonexistent keys returns 0.0. 
 * Stores the sum of all values and provides methods for normalizing them.
 * 
 * A {@code null} key is allowed, although the Iterator returned by {@link #getIterator()} 
 * will not include an entry whose key is {@code null}. 
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2009-03-25
 * @param <T> Type for keys
 * @see IntCounter
 */
public class Counter<T> extends AbstractCounter<T,Double> implements java.io.Serializable {
	private static final long serialVersionUID = 8749403819704088504L;

	public class CounterFactory implements FactoryDefaultMap.DefaultValueFactory<Counter<T>> {
		public Counter<T> newDefaultValue() { return new Counter<T>(); }
	}
	
	protected TObjectDoubleHashMap<T> m_map;
	protected double m_sum = 0.0;
	
	public final double DEFAULT_VALUE = 0.0;
	
	public Counter() {
		m_map = new TObjectDoubleHashMap<T>();
	}
	
	public Counter(TObjectHashingStrategy<T> hs) {
		m_map = new TObjectDoubleHashMap<T>(hs);
	}
	
	public Counter(TObjectDoubleHashMap<T> map) {
		m_map = map;
		
		double vals[] = map.getValues();
		for (double val : vals) {
			m_sum += val;
		}
	}
	
	/**
	 * Alias of {@link #getT(Object)}
	 * @param key
	 * @return The value stored for a particular key (if present), or 0 otherwise
	 */
	public double getCount(T key) {
		return this.getT(key);
	}
	
	/** Calls {@link #getT(T)}; required for compliance with {@link Map} */
	@SuppressWarnings("unchecked")
	public Double get(Object key) {
		return getT((T)key);
	}
	
	/**
	 * @param key
	 * @return The value stored for a particular key (if present), or 0 otherwise
	 */
	public double getT(T key) {
		if (m_map.containsKey(key))
			return m_map.get(key);
		return DEFAULT_VALUE;
	}
	
	/**
	 * @param key
	 * @param newValue
	 * @return Previous value for the key
	 */
	public double set(T key, double newValue) {
		double preval = getT(key);
		m_map.put(key, newValue);
		m_sum += newValue - preval;
		return preval;
	}
	
	/**
	 * Increments a value in the counter by 1.
	 * @param key
	 * @return The new value
	 */
	public double increment(T key) {
		return incrementBy(key, 1.0);
	}
	
	/**
	 * Changes a value in the counter by adding the specified delta to its current value.
	 * @param key
	 * @param delta
	 * @return The new value
	 */
	public double incrementBy(T key, double delta) {
		double curval = getT(key);
		double newValue = curval+delta;
		set(key, newValue);
		return newValue;
	}
	
	/**
	 * Iterates through the given list of keys, incrementing each by 1.
	 * @param keys
	 */
	public void incrementAll(Collection<? extends T> keys) {
		for (T key : keys) {
			increment(key);
		}
	}
	
	public void incrementAllBy(double delta) {
		for (TObjectDoubleIterator<T> iter = getIterator();
				iter.hasNext();) {
			iter.advance();
			this.incrementBy(iter.key(), delta);
		}
	}
	
	public void incrementAllBy(Collection<? extends T> keys, double delta) {
		for (T key : keys) {
			incrementBy(key, delta);
		}
	}
	
	public void incrementAllBy(Counter<? extends T> counts) {
		for (TObjectDoubleIterator<? extends T> iter = counts.getIterator();
				iter.hasNext();) {
			iter.advance();
			this.incrementBy(iter.key(), iter.value());
		}
	}
	
	/** 
	 * Returns a new counter containing only keys with nonzero values in 
	 * either this or 'that'. Each key's value is 2 if it occurs in both 
	 * this and 'that' or 1 otherwise.
	 */
	public Counter<T> orWith(Counter<? extends T> that) {
		List<Counter<? extends T>> list = new ArrayList<Counter<? extends T>>();
		list.add(this);
		list.add(that);
		return or(list);
	}
	
	/** 
	 * Returns a new counter containing only keys with nonzero values in 
	 * at least one of the provided counters. Each key's value is the 
	 * number of counters in which it occurs.
	 */
	public static <T> Counter<T> or(Collection<Counter<? extends T>> counters) {
		Counter<T> result = new Counter<T>();
		for (Counter<? extends T> counter : counters) {
			for (TObjectDoubleIterator<? extends T> iter = counter.getIterator();
					iter.hasNext();) {
				iter.advance();
				if (iter.value()!=0)
					result.increment(iter.key());
			}
		}
		return result;
	}
	
	/** 
	 * Returns a new counter containing only keys with nonzero values in 
	 * both this and 'that'. Each key's value is the sum of its corresponding 
	 * values in this and 'that'.
	 */
	public Counter<T> andWith(Counter<T> that) {
		Counter<T> result = new Counter<T>();
		for (TObjectDoubleIterator<T> iter = this.getIterator();
				iter.hasNext();) {
			iter.advance();
			if (iter.value()!=0 && that.getCount(iter.key())!=0)
				result.set(iter.key(), iter.value() + that.getCount(iter.key()));
		}
		return result;
	}
	
	/**
	 * @return Sum of all values currently in the Counter
	 */
	public double getSum() {
		return m_sum;
	}
	
	public Counter<T> add(final double val) {
		final Counter<T> result = new Counter<T>();
		m_map.forEachEntry(new TObjectDoubleProcedure<T>() {
            private boolean first = true;
            public boolean execute(T key, double value) {
            	if ( first ) first = false;
            	double newValue = value + val;
	            result.set(key, newValue);
                return true;
            }
        });
		return result;
	}
	
	public Counter<T> addAll(Counter<? extends T> counts) {
		Counter<T> result = this.clone();
		result.incrementAllBy(counts);
		return result;
	}
	
	/**
	 * @return A new Counter instance with the values in this Counter divided by their sum
	 */
	public Counter<T> normalize() {
		return divideBy(m_sum);
	}
	
	/**
	 * Equivalent to {@link #scaleBy(double)} called with 1.0/{@code denominator}
	 * @param denominator
	 * @return
	 */
	public Counter<T> divideBy(double denominator) {
		return scaleBy(1.0/denominator);
	}
	
	/**
	 * For each entry in this counter, looks up the corresponding entry in {@code that} 
	 * and stores their ratio in the result.
	 * @param that
	 */
	public Counter<T> divideAllBy(final Counter<? super T> that) {
		final Counter<T> result = new Counter<T>();
		m_map.forEachEntry(new TObjectDoubleProcedure<T>() {
            private boolean first = true;
            public boolean execute(T key, double value) {
            	if ( first ) first = false;
            	double newValue = value / that.getT(key);
	            result.set(key, newValue);
                return true;
            }
        });
		return result;
	}
	
	/**
	 * @param factor Scaling factor to be multiplied by each value in this counter
	 * @return A new Counter instance equivalent to this one, but with scaled values
	 */
	public Counter<T> scaleBy(double factor) {
		Counter<T> result = new Counter<T>();
		for (TObjectDoubleIterator<T> iter = getIterator();
				iter.hasNext();) {
			iter.advance();
			result.set(iter.key(), iter.value() * factor);
		}
		if (containsKeyT(null))
			result.set(null, getT(null)*factor);
		return result;
	}
	
	/**
	 * For each entry in this counter, looks up the corresponding entry in {@code that} 
	 * and stores their product in the result.
	 * @param that
	 */
	public Counter<T> scaleAllBy(final Counter<? super T> that) {
		final Counter<T> result = new Counter<T>();
		m_map.forEachEntry(new TObjectDoubleProcedure<T>() {
            private boolean first = true;
            public boolean execute(T key, double value) {
            	if ( first ) first = false;
            	double newValue = value * that.getT(key);
	            result.set(key, newValue);
                return true;
            }
        });
		return result;
	}
	
	/**
	 * Divides the values in this Counter by their sum
	 */
	public void normalizeInPlace() {
		divideInPlace(getSum());
	}
	
	/**
	 * @param denominator
	 * Divides the values in this Counter by the given denominator
	 */
	public void divideInPlace(double denominator) {
		scaleInPlace(1.0/denominator);
	}
	
	/**
	 * @param factor Scaling factor to be multiplied by each value in this counter
	 */
	public void scaleInPlace(double factor) {
		for (TObjectDoubleIterator<T> iter = getIterator();
				iter.hasNext();) {
			iter.advance();
			double preval = iter.value();
			double newVal = preval * factor;
			iter.setValue(newVal);
			m_sum += newVal - preval;
		}
		if (containsKeyT(null)) {
			double preval = getT(null);
			double newVal = preval * factor;
			set(null, newVal);
			m_sum += newVal - preval;
		}
	}
	
	/**
	 * @return Iterator for the counter. Ignores the {@code null} key (if present).
	 */
	public TObjectDoubleIterator<T> getIterator() {
		return m_map.iterator();
	}
	
	public T[] keys(T[] array) {
		return m_map.keys(array);
	}
	
	@SuppressWarnings("unchecked")
	public Set<T> keySet() {
		Object[] okeys = m_map.keys();
		HashSet<T> keyset = new HashSet<T>();
		for(Object o:okeys) {
			keyset.add((T)o);
		}
		return keyset;
	}
	
	/**
	 * @param valueThreshold
	 * @return Set of keys whose corresponding value equals or exceeds the given threshold
	 */
	public Set<T> filteredKeys(int valueThreshold) {
		Set<T> result = new THashSet<T>();
		for (TObjectDoubleIterator<T> iter = getIterator();
				iter.hasNext();) {
			iter.advance();
			T key = iter.key();
			double value = getT(key);
			if (value >= valueThreshold) {
				result.add(key);
			}
		}
		if (containsKeyT(null) && getT(null) >= valueThreshold)
			result.add(null);
		return result;
	}
	
	/**
	 * @param valueThreshold
	 * @return New IntCounter containing only entries whose value equals or exceeds the given threshold
	 */
	public Counter<T> filter(double valueThreshold) {
		Counter<T> result = new Counter<T>();
		for (TObjectDoubleIterator<T> iter = getIterator();
				iter.hasNext();) {
			iter.advance();
			T key = iter.key();
			double value = getT(key);
			if (value >= valueThreshold) {
				result.set(key, value);
			}
		}
		double nullValue = getT(null);
		if (containsKeyT(null) && nullValue >= valueThreshold)
			result.set(null, nullValue);
		return result;
	}
	
	/** Calls {@link #containsKeyT(T)}; required for compliance with {@link Map} */
	@SuppressWarnings("unchecked")
	public boolean containsKey(Object key) {
		return containsKeyT((T)key);
	}
	
	public boolean containsKeyT(T key) {
		return m_map.containsKey(key);
	}
	
	public int size() {
		return m_map.size();
	}
	
	public String toString() {
		return toString(Double.NEGATIVE_INFINITY, null);
	}
	
	public String toString(double valueThreshold) {
		return toString(valueThreshold, null);
	}
	
	/**
	 * @param sep Array with two Strings: an entry separator ("," by default, if this is {@code null}), and a key-value separator ("=" by default)
	 */
	public String toString(String[] sep) {
		return toString(Double.NEGATIVE_INFINITY, sep);
	}
	
	/**
	 * @param valueThreshold
	 * @param sep Array with two Strings: an entry separator ("," by default, if this is {@code null}), and a key-value separator ("=" by default)
	 * @return A string representation of all (key, value) pairs such that the value equals or exceeds the given threshold
	 */
	public String toString(final double valueThreshold, String[] sep) {
    	String entrySep = ",";	// default
		String kvSep = "=";	// default
		if (sep!=null && sep.length>0) {
			if (sep[0]!=null) 
				entrySep = sep[0];
			if (sep.length>1 && sep[1]!=null)
				kvSep = sep[1];
		}
		final String ENTRYSEP = entrySep;
		final String KVSEP = kvSep;
        final StringBuilder buf = new StringBuilder("{");
        m_map.forEachEntry(new TObjectDoubleProcedure<T>() {
            private boolean first = true;
            public boolean execute(T key, double value) {
            	if (value >= valueThreshold) {
	                if ( first ) first = false;
	                else buf.append(ENTRYSEP);
	
	                buf.append(key);
	                buf.append(KVSEP);
	                buf.append(value);
            	}
                return true;
            }
        });
        buf.append("}");
        return buf.toString();
	}
	
	public Counter<T> clone() {
		return new Counter<T>(m_map.clone());
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException("Counter.clear() unsupported");
	}

	@Override
	public boolean containsValue(Object value) {
		return m_map.containsValue((Double)value);
	}

	@Override
	public Set<java.util.Map.Entry<T, Double>> entrySet() {
		return asMap().entrySet();
	}

	public Map<T, Double> asMap() {
		final Map<T, Double> copy = Maps.newHashMapWithExpectedSize(size());
		final TObjectDoubleIterator<T> iter = getIterator();
		while (iter.hasNext()) {
			iter.advance();
			copy.put(iter.key(), iter.value());
		}
		return copy;
	}


	@Override
	public boolean isEmpty() {
		return m_map.isEmpty();
	}

	@Override
	public Double put(T key, Double value) {
		return set(key,value);
	}

	@Override
	public void putAll(Map<? extends T, ? extends Double> m) {
		throw new UnsupportedOperationException("Counter.putAll() unsupported");
	}

	@Override
	public Double remove(Object key) {
		throw new UnsupportedOperationException("Counter.remove() unsupported");
	}

	@Override
	public Collection<Double> values() {
		throw new UnsupportedOperationException("Counter.values() unsupported");
	}
	
	/**
	 * Computes the sum of products of key-value pairs in {@code that}
	 * @param that
	 * @return
	 */
	public static double sumOfProducts(Counter<Integer> that) {
		final double[] result = new double[1];	// use a single-element array to make this variable 'final'
		result[0] = 0;
		that.m_map.forEachEntry(new TObjectDoubleProcedure<Integer>() {
            private boolean first = true;
            public boolean execute(Integer key, double value) {
            	if ( first ) first = false;
            	result[0] += key * value;
                return true;
            }
        });
		return result[0];
	}
}
