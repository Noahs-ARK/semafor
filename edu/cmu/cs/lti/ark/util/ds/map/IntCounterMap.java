/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * IntCounterMap.java is part of SEMAFOR 2.0.
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

import java.util.Map;

import edu.cmu.cs.lti.ark.util.ds.map.IntCounter.IntCounterFactory;

import gnu.trove.TObjectIntIterator;
import gnu.trove.TObjectObjectProcedure;

/**
 * Acts as an IntCounter, but associates keys with other IntCounters (rather than numeric values).
 * @author Nathan Schneider (nschneid)
 * @since 2009-09-30
 *
 * @param <K>
 * @param <L>
 */

public class IntCounterMap<K,L> extends FactoryDefaultMap<K,IntCounter<L>> implements ICounterMap<K,L,Integer,IntCounter<L>> {
	public IntCounterMap() {
		super(new IntCounter<L>().new IntCounterFactory());
	}
	
	public IntCounter<L> getCounter(K key) {
		return this.get(key);
	}
	
	public Integer getCount(K key1, L key2) {
		return this.getCounter(key1).getT(key2);
	}
	
	public Integer increment(K key1, L key2) { return incrementBy(key1,key2,1); }
	public Integer incrementBy(K key1, L key2, Integer delta) {
		return this.getCounter(key1).incrementBy(key2, delta);
	}
	
	public int set(K key1, L key2, int newValue) {
		return this.getCounter(key1).set(key2, newValue);
	}
	
	/**
	 * If {@code this} is an {@link IntCounterMap} of the form <code> { a => { b => c } } </code>,
	 * returns a new {@link IntCounterMap} of the form <code> { b => { a => c } } </code>.
	 */
	public IntCounterMap<L,K> invert() {
		IntCounterMap<L,K> invertedMap = new IntCounterMap<L,K>();
		for (Map.Entry<K,IntCounter<L>> item : this.entrySet()) {
			K key1 = item.getKey();
			IntCounter<L> counter = item.getValue();
			for (TObjectIntIterator<L> iter = counter.getIterator();
					iter.hasNext();) {
				iter.advance();
				L key2 = iter.key();
				int value = iter.value();
				invertedMap.set(key2, key1, value);
			}
		}
		return invertedMap;
	}
	
	/**
	 * If {@code this} is a {@link IntCounterMap} of the form <code> { a => { b => c } } </code>,
	 * returns a new {@link IntCounterMap} of the form <code> { b => {c => # } } </code>
	 * where for every {@code b}, {@code #} is the number of {@code a}'s for which {@code this[a][b]==c }
	 */
	public IntCounterMap<L,Integer> getAggregateHistogram() {
		IntCounterMap<L,Integer> aggregateHistogram = new IntCounterMap<L,Integer>();
		for (IntCounter<L> counter : this.values()) {
			for (TObjectIntIterator<L> iter = counter.getIterator();
					iter.hasNext();) {
				iter.advance();
				L key = iter.key();
				int value = iter.value();
				aggregateHistogram.increment(key, value);
			}
		}
		return aggregateHistogram;
	}

	/**
	 * @param sep Array with at least 2 Strings: an entry separator ("," by default, if this is {@code null}), and a key-value separator ("=" by default). 
	 * If 4 Strings are specified, the first pair will be used for the outer display, and the second pair for the inner display (of the contained {@link IntCounter}s).  
	 */
	public String toString(String[] sep) {
		return toString(Integer.MIN_VALUE, sep);
	}
	
	/**
	 * @param valueThreshold
	 * @param sep Array with at least 2 Strings: an entry separator ("," by default, if this is {@code null}), and a key-value separator ("=" by default). 
	 * If 4 Strings are specified, the first pair will be used for the outer display, and the second pair for the inner display (of the contained {@link IntCounter}s).  
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
		final String[] INNER_SEPS = new String[2];
		if (sep.length==4) {
			INNER_SEPS[0] = sep[2];
			INNER_SEPS[1] = sep[3];
		}
		else {
			INNER_SEPS[0] = ENTRYSEP;
			INNER_SEPS[1] = KVSEP;
		}
        final StringBuilder buf = new StringBuilder("{");
        this.forEachEntry(new TObjectObjectProcedure<K,IntCounter<L>>() {
            private boolean first = true;
            public boolean execute(K key, IntCounter<L> value) {
            	if ( first ) first = false;
	            else buf.append(ENTRYSEP);
            	
	            buf.append(key);
	            buf.append(KVSEP);
	            buf.append(value.toString(valueThreshold, INNER_SEPS));
            	return true;
            }
        });
        buf.append("}");
        return buf.toString();
    }

}
