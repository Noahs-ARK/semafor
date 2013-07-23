/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * IntCounter.java is part of SEMAFOR 2.0.
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

import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;

/**
 * Simple integer counter: stores integer values for keys; lookup on nonexistent keys returns 0.
 * Stores the sum of all values and provides methods for normalizing them.
 * 
 * A {@code null} key is allowed, although the Iterator returned by {@link #getIterator()} 
 * will not include an entry whose key is {@code null}. 
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2009-03-19
 * @param <T> Type for keys
 */
@NotThreadSafe
public class IntCounter<T> extends AbstractCounter<T, Integer> implements java.io.Serializable {
	private static final long serialVersionUID = -5622820446958578575L;

	protected TObjectIntHashMap<T> m_map;
	protected int m_sum = 0;
	
	public final int DEFAULT_VALUE = 0;
	
	public IntCounter() {
		m_map = new TObjectIntHashMap<T>();
	}

	public IntCounter(TObjectIntHashMap<T> map) {
		m_map = map;
		
		int vals[] = map.getValues();
		for (int val : vals) {
			m_sum += val;
		}
	}
	
	/**
	 * Alias of {@link #getT(Object)}
	 * @param key
	 * @return The value stored for a particular key (if present), or 0 otherwise
	 */
	public int getCount(T key) {
		return this.getT(key);
	}
	
	/**
	 * @param key
	 * @return The value stored for a particular key (if present), or 0 otherwise
	 */
	public int getT(T key) {
		if (m_map.containsKey(key))
			return m_map.get(key);
		return DEFAULT_VALUE;
	}
	
	/** Calls {@link #getT(T)}; required for compliance with {@link Map} */
	@SuppressWarnings("unchecked")
	public Integer get(Object key) {
		return getT((T)key);
	}
	
	/**
	 * @param key
	 * @param newValue
	 * @return Previous value for the key
	 */
	public int set(T key, int newValue) {
		int preval = getT(key);
		m_map.put(key, newValue);
		m_sum += newValue - preval;
		return preval;
	}
	
	/**
	 * Increments a value in the counter by 1.
	 * @param key
	 * @return The new value
	 */
	public int increment(T key) {
		return incrementBy(key, 1);
	}
	
	/**
	 * Changes a value in the counter by adding the specified delta to its current value.
	 * @param key
	 * @param delta
	 * @return The new value
	 */
	public int incrementBy(T key, int delta) {
		int curval = getT(key);
		int newValue = curval+delta;
		set(key, newValue);
		return newValue;
	}

	public Map<T, Integer> asMap() {
		final Map<T, Integer> copy = Maps.newHashMapWithExpectedSize(size());
		final TObjectIntIterator<T> iter = getIterator();
		while (iter.hasNext()) {
			iter.advance();
			copy.put(iter.key(), iter.value());
		}
		return copy;
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
	
	public void incrementAllBy(int delta) {
		for (TObjectIntIterator<T> iter = getIterator();
				iter.hasNext();) {
			iter.advance();
			this.incrementBy(iter.key(), delta);
		}
	}
	
	public void incrementAllBy(Collection<? extends T> keys, int delta) {
		for (T key : keys) {
			incrementBy(key, delta);
		}
	}
	
	public void incrementAllBy(IntCounter<? extends T> counts) {
		for (TObjectIntIterator<? extends T> iter = counts.getIterator();
				iter.hasNext();) {
			iter.advance();
			this.incrementBy(iter.key(), iter.value());
		}
		if (counts.containsKey(null))
			this.incrementBy(null, counts.getCount(null));
	}
	
	/** 
	 * Returns a new counter containing only keys with nonzero values in 
	 * either this or 'that'. Each key's value is 2 if it occurs in both 
	 * this and 'that' or 1 otherwise.
	 */
	public IntCounter<T> orWith(IntCounter<? extends T> that) {
		List<IntCounter<? extends T>> list = new ArrayList<IntCounter<? extends T>>();
		list.add(this);
		list.add(that);
		return or(list);
	}
	
	/** 
	 * Returns a new counter containing only keys with nonzero values in 
	 * at least one of the provided counters. Each key's value is the 
	 * number of counters in which it occurs.
	 */
	public static <T> IntCounter<T> or(Collection<IntCounter<? extends T>> counters) {
		IntCounter<T> result = new IntCounter<T>();
		for (IntCounter<? extends T> counter : counters) {
			for (TObjectIntIterator<? extends T> iter = counter.getIterator();
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
	public IntCounter<T> andWith(IntCounter<T> that) {
		IntCounter<T> result = new IntCounter<T>();
		for (TObjectIntIterator<T> iter = this.getIterator();
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
	public int getSum() {
		return m_sum;
	}
	
	public IntCounter<T> add(final int val) {
		final IntCounter<T> result = new IntCounter<T>();
		m_map.forEachEntry(new TObjectIntProcedure<T>() {
            private boolean first = true;
            public boolean execute(T key, int value) {
            	if ( first ) first = false;
            	int newValue = value + val;
	            result.set(key, newValue);
                return true;
            }
        });
		return result;
	}
	
	public IntCounter<T> addAll(IntCounter<? extends T> counts) {
		IntCounter<T> result = this.clone();
		result.incrementAllBy(counts);
		return result;
	}
	
	/**
	 * @return A new Counter instance with the values in this IntCounter divided by their sum
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
		m_map.forEachEntry(new TObjectIntProcedure<T>() {
            private boolean first = true;
            public boolean execute(T key, int value) {
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
	public IntCounter<T> scaleBy(int factor) {
		IntCounter<T> result = new IntCounter<T>();
		for (TObjectIntIterator<T> iter = getIterator();
				iter.hasNext();) {
			iter.advance();
			result.set(iter.key(), iter.value() * factor);
		}
		if (containsKey(null))
			result.set(null, getT(null)*factor);
		return result;
	}
	
	/**
	 * @param factor Scaling factor to be multiplied by each value in this counter
	 * @return A new Counter instance equivalent to this one, but with scaled values
	 */
	public Counter<T> scaleBy(double factor) {
		Counter<T> result = new Counter<T>();
		for (TObjectIntIterator<T> iter = getIterator();
				iter.hasNext();) {
			iter.advance();
			result.set(iter.key(), iter.value() * factor);
		}
		if (containsKey(null))
			result.set(null, getT(null)*factor);
		return result;
	}
	
	/**
	 * For each entry in this counter, looks up the corresponding entry in {@code that} 
	 * and stores their product in the result.
	 * @param that
	 */
	public Counter<T> scaleAllBy(final Counter<T> that) {
		final Counter<T> result = new Counter<T>();
		m_map.forEachEntry(new TObjectIntProcedure<T>() {
            private boolean first = true;
            public boolean execute(T key, int value) {
            	if ( first ) first = false;
            	double newValue = value * that.getT(key);
	            result.set(key, newValue);
                return true;
            }
        });
		return result;
	}
	
	/**
	 * @return Iterator for the counter. Ignores the {@code null} key (if present).
	 */
	public TObjectIntIterator<T> getIterator() {
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
		for (TObjectIntIterator<T> iter = getIterator();
				iter.hasNext();) {
			iter.advance();
			T key = iter.key();
			int value = getT(key);
			if (value >= valueThreshold) {
				result.add(key);
			}
		}
		if (containsKey(null) && getT(null) >= valueThreshold)
			result.add(null);
		return result;
	}
	
	/**
	 * @param valueThreshold
	 * @return New IntCounter containing only entries whose value equals or exceeds the given threshold
	 */
	public IntCounter<T> filter(int valueThreshold) {
		IntCounter<T> result = new IntCounter<T>();
		for (TObjectIntIterator<T> iter = getIterator();
				iter.hasNext();) {
			iter.advance();
			T key = iter.key();
			int value = getT(key);
			if (value >= valueThreshold) {
				result.set(key, value);
			}
		}
		int nullValue = getT(null);
		if (containsKey(null) && nullValue >= valueThreshold)
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
	
	/**
	 * @return A map from unique values in this IntCounter to the number of distinct keys with which they are associated
	 */
	public IntCounter<Integer> getHistogram() {
		IntCounter<Integer> histogram = new IntCounter<Integer>();
		for (TObjectIntIterator<T> iter = getIterator();
				iter.hasNext();) {
			iter.advance();
			int value = iter.value();
			histogram.increment(value);
		}
		if (containsKey(null))
			histogram.increment(getT(null));
		return histogram;
	}
	
	public int size() {
		return m_map.size();
	}
	
	public String toString() {
		return toString(Integer.MIN_VALUE, null);
	}
	
	public String toString(int valueThreshold) {
		return toString(valueThreshold, null);
	}
	
	/**
	 * @param sep Array with two Strings: an entry separator ("," by default, if this is {@code null}), and a key-value separator ("=" by default)
	 */
	public String toString(String[] sep) {
		return toString(Integer.MIN_VALUE, sep);
	}
	
	
	/**
	 * @param valueThreshold
	 * @param sep Array with two Strings: an entry separator ("," by default, if this is {@code null}), and a key-value separator ("=" by default)
	 * @return A string representation of all (key, value) pairs such that the value equals or exceeds the given threshold
	 */
    public String toString(final int valueThreshold, String[] sep) {
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
        m_map.forEachEntry(new TObjectIntProcedure<T>() {
            private boolean first = true;
            public boolean execute(T key, int value) {
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
    
    /** @see {@link #toStringWithPercents(int, double, String[], int)} */
    public String toStringWithPercents(String[] sep, int precision) {
    	return toStringWithPercents(Integer.MIN_VALUE, Double.MIN_VALUE, sep, precision);
    }
    
    /**
	 * @param valueThreshold
	 * @param percentThreshold
	 * @param sep Array with three Strings: a value-percent separator ({@literal " "} by default, if this is {@literal null}}; 
	 * an entry separator ({@literal "%,"} by default, if this is {@literal null}); and a key-value separator ({@literal "="} by default)
	 * @param precision Number of decimal places to display for the percent
	 * @return A string representation of all (key, value) pairs such that the value equals or exceeds the given threshold
	 */
    public String toStringWithPercents(final int valueThreshold, final double percentThreshold, String[] sep, int precision) {
    	String percSep = " "; // default
    	String entrySep = "%,";	// default
		String kvSep = "=";	// default
		if (sep!=null && sep.length>0) {
			if (sep[0]!=null) 
				percSep = sep[0];
			if (sep.length>1 && sep[1]!=null) {
				entrySep = sep[1];
				if (sep.length>2 && sep[2]!=null)
					kvSep = sep[2];
			}
		}
		final String PERCSEP = percSep;
		final String ENTRYSEP = entrySep;
		final String KVSEP = kvSep;
		final int PRECISION = precision;
		final int SUM = getSum();
        final StringBuilder buf = new StringBuilder("{");
        m_map.forEachEntry(new TObjectIntProcedure<T>() {
            private boolean first = true;
            public boolean execute(T key, int value) {
            	double percent = 100.0*value / SUM;
            	if (value >= valueThreshold && percent >= percentThreshold) {
	                if ( first ) first = false;
	                else buf.append(ENTRYSEP);
	
	                buf.append(key);
	                buf.append(KVSEP);
	                buf.append(value);
	                buf.append(PERCSEP);
	                buf.append(String.format("%." + PRECISION + "f", percent));
            	}
            	return true;
            }
        });
        buf.append("}");
        return buf.toString();
    }
	
	public Counter<T> toCounter() {
		TObjectDoubleHashMap<T> map = new TObjectDoubleHashMap<T>();
		for (TObjectIntIterator<T> iter = getIterator();
				iter.hasNext();) {
			iter.advance();
			map.put(iter.key(), (double)iter.value());
		}
		if (containsKey(null))
			map.put(null, (double)getT(null));
		return new Counter<T>(map);
	}
	
	public IntCounter<T> clone() {
		return new IntCounter<T>(m_map.clone());
	}

	// Other methods implemented by the Map interface
	
	@Override
	public void clear() {
		throw new UnsupportedOperationException("IntCounter.clear() unsupported");
	}

	@Override
	public boolean containsValue(Object value) {
		return m_map.containsValue((Integer)value);
	}

	@Override
	public Set<java.util.Map.Entry<T, Integer>> entrySet() {
		return asMap().entrySet();
	}

	@Override
	public boolean isEmpty() {
		return m_map.isEmpty();
	}

	@Override
	public Integer put(T key, Integer value) {
		return set(key,value);
	}

	@Override
	public void putAll(Map<? extends T, ? extends Integer> m) {
		throw new UnsupportedOperationException("IntCounter.putAll() unsupported");
	}

	@Override
	public Integer remove(Object key) {
		throw new UnsupportedOperationException("IntCounter.remove() unsupported");
	}

	@Override
	public Collection<Integer> values() {
		throw new UnsupportedOperationException("IntCounter.values() unsupported");
	}
	
	/**
	 * Computes the sum of products of key-value pairs in {@code that}
	 * @param that
	 * @return
	 */
	public static int sumOfProducts(IntCounter<Integer> that) {
		final int[] result = new int[1];	// use a single-element array to make this variable 'final'
		result[0] = 0;
		that.m_map.forEachEntry(new TObjectIntProcedure<Integer>() {
            private boolean first = true;
            public boolean execute(Integer key, int value) {
            	if ( first ) first = false;
            	result[0] += key * value;
                return true;
            }
        });
		return result[0];
	}
}
