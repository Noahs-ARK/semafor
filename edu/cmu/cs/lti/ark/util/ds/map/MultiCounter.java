/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * MultiCounter.java is part of SEMAFOR 2.0.
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

import gnu.trove.THashSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Houses a fixed set of component {@link Counter}s, or "fields" (which may have string names); 
 * keys are expected to be largely shared across these fields. If the fields have mostly disjoint 
 * key sets, {@link CounterMap} should be used instead.
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2009-11-18
 *
 * @param <K> Type of keys for entries in the component {@link Counter}s
 */

public class MultiCounter<K> implements
		ICounterMap<String, K, Double, Counter<K>> {

	//protected Set<K> sharedKeySet;	// TODO: computed on the fly for now, but there will be a lot of overlap among counters' keys. See keys() below.
	protected final Map<String,Integer> counterNameMap;
	protected final String[] counterNames;
	
	protected final Counter<K>[] counters;
	
	@SuppressWarnings("unchecked")
	public MultiCounter(int numCounters) {
		this.counterNames = new String[numCounters];
		counterNameMap = new HashMap<String,Integer>();
		counters = new Counter[numCounters];
	}
	@SuppressWarnings("unchecked")
	public MultiCounter(String[] counterNames) {
		this.counterNames = counterNames;
		counters = new Counter[counterNames.length];
		counterNameMap = new HashMap<String,Integer>();
		int i = 0;
		for (String name : counterNames) {
			counters[i] = new Counter<K>();
			this.counterNameMap.put(name, i);
			i++;
		}
	}
	
	@Override
	public Double getCount(String field, K key) {
		return getCount(counterNameMap.get(field),key);
	}
	public Double getCount(int fieldIndex, K key) {
		return getCounter(fieldIndex).getCount(key);
	}

	@Override
	public Counter<K> getCounter(String field) {
		return getCounter(counterNameMap.get(field));
	}
	public Counter<K> getCounter(int fieldIndex) {
		return counters[fieldIndex];
	}

	@Override
	public Double increment(String field, K key) {
		return increment(counterNameMap.get(field),key);
	}
	public Double increment(int fieldIndex, K key) {
		return getCounter(fieldIndex).increment(key);
	}

	@Override
	public Double incrementBy(String field, K key, Double delta) {
		return incrementBy(counterNameMap.get(field),key,delta);
	}
	public Double incrementBy(int fieldIndex, K key, Double delta) {
		return getCounter(fieldIndex).increment(key);
	}

	/** @deprecated Unsupported; included for interface compliance */
	@Override
	public void clear() {
		throw new UnsupportedOperationException("Cannot clear a MultiCounter");
	}

	/**
	 * @return whether some counter (field) contains the specified key. Compare {@link #containsKey(Object)}.
	 */
	public boolean containsKeyT(K key) {
		for (Counter<K> counter : counters) {
			if (counter.containsKey(key))
				return true;
		}
		return false;
	}
	
	public boolean containsField(String fieldName) {
		return keySet().contains(fieldName);
	}
	/** Equivalent to {@link #containsField(String)} */
	@Override
	public boolean containsKey(Object field) {
		return keySet().contains((String)field);
	}

	/**
	 * Returns whether the specified value is present for some field
	 */
	@Override
	public boolean containsValue(Object value) {
		for (Counter<K> counter : counters) {
			if (counter.containsValue(value))
				return true;
		}
		return false;
	}

	@Override
	public Set<java.util.Map.Entry<String, Counter<K>>> entrySet() {
		THashSet<java.util.Map.Entry<String,Counter<K>>> entries = new THashSet<java.util.Map.Entry<String,Counter<K>>>();
		for (int i=0; i<counters.length; i++) {
			entries.add(new java.util.AbstractMap.SimpleEntry<String,Counter<K>>(counterNames[i], counters[i]));
		}
		return entries;
	}

	@Override
	public Counter<K> get(Object field) {
		if (field instanceof Integer)
			return getCounter((Integer)field);
		return getCounter((String)field);
	}

	/**
	 * @return whether this has any counters (fields).
	 */
	@Override
	public boolean isEmpty() {
		return counters.length==0;
	}

	/**
	 * @return the union of keys for all counters (fields). Compare {@link #keySet()}.
	 */
	public Set<K> keys() {
		// TODO: This implementation is extremely naive, as keys will often be shared across counters.
		// Possible fixes:
		// - Assume the Counters will not be accessed directly by the user, and simply log keys here from the getCount() and increment() methods
		// - Pass each Counter a reference to a shared Set, so the Counters can log keys there as needed
		Set<K> keys = new THashSet<K>();
		for (Counter<K> counter : counters)
			keys.addAll(counter.keySet());
		return keys;
	}
	
	/**
	 * @return the names of counters (fields). Compare {@link #keys()}.
	 */
	@Override
	public Set<String> keySet() {
		return counterNameMap.keySet();
	}

	/** @deprecated Unsupported; included for interface compliance */
	@Override
	public Counter<K> put(String key, Counter<K> value) {
		throw new UnsupportedOperationException("MultiCounter.put() is unsupported");
	}

	/** @deprecated Unsupported; included for interface compliance */
	@Override
	public void putAll(Map<? extends String, ? extends Counter<K>> m) {
		throw new UnsupportedOperationException("MultiCounter.putall() is unsupported");
	}

	/** @deprecated Unsupported; included for interface compliance */
	@Override
	public Counter<K> remove(Object key) {
		throw new UnsupportedOperationException("MultiCounter.remove() is unsupported");
	}

	/**
	 * @return the total number of unique keys across all counters (fields). Compare {@link #size()}.
	 */
	public int numKeys() {
		return keys().size();
	}
	
	/**
	 * @return the number of counters (fields) in this MultiCounter. Compare {@link #numKeys()}.
	 */
	@Override
	public int size() {
		return counters.length;
	}

	public Counter<K>[] getCounters() {
		return counters;
	}
	/** Equivalent to {@link #getCounters()}, but returns a collection */
	@Override
	public Collection<Counter<K>> values() {
		return Arrays.asList(counters);
	}

	public String toHTMLTable(String valueFormat) { return toHTMLTable(valueFormat,Double.NEGATIVE_INFINITY); }
	public String toHTMLTable(String valueFormat, double valueThreshold) {
		StringBuffer sb = new StringBuffer("");
		sb.append("<table>\n");
		if (counterNames!=null) {
			sb.append("\t<tr><th></th>");
			for (String field : counterNames)
				sb.append("<th>" + field + "</th>");
			sb.append("</tr>\n");
		}
		Set<K> keys = keys();
		
		for (K key : keys) {
			String row = "\t<tr><th>" + key.toString() + "</th>";
			boolean hasNonemptyColumn = false;
			for (Counter<K> counter : counters) {
				double v = counter.getCount(key);
				if (v >= valueThreshold) {
					row += String.format("<td>" + valueFormat + "</td>", v);
					hasNonemptyColumn = true;
				}
				else
					row += "<td></td>";
			}
			row += "</tr>\n";
			if (hasNonemptyColumn)
				sb.append(row);
		}
		sb.append("</table>\n");
		return sb.toString();
	}
	public String toLaTeXTable(String valueFormat) { return toLaTeXTable(valueFormat,Double.NEGATIVE_INFINITY); } 
	public String toLaTeXTable(String valueFormat, double valueThreshold) {
		return null;	// TODO
	}
	/**
	 * @return A tab-delimited text table 
	 */
	public String toTable(String valueFormat) { return toTable(valueFormat,Double.NEGATIVE_INFINITY); } 
	public String toTable(String valueFormat, double valueThreshold) {
		StringBuffer sb = new StringBuffer("");
		if (counterNames!=null) {
			for (String field : counterNames)
				sb.append("\t" + field);
			sb.append("\n");
		}
		Set<K> keys = keys();
		
		for (K key : keys) {
			String row = key.toString();
			boolean hasNonemptyColumn = false;
			for (Counter<K> counter : counters) {
				double v = counter.getCount(key);
				if (v >= valueThreshold) {
					row += String.format("\t" + valueFormat, v);
					hasNonemptyColumn = true;
				}
				else
					row += "\t";
			}
			row += "\n";
			if (hasNonemptyColumn)
				sb.append(row);
		}
		return sb.toString();
	}
}
