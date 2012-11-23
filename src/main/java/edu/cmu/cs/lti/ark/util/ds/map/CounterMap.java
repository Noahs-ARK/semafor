/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * CounterMap.java is part of SEMAFOR 2.0.
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

import gnu.trove.TObjectObjectProcedure;

/**
 * Acts as a Counter, but associates keys with other Counters (rather than numeric values).
 * @author Nathan Schneider (nschneid)
 * @since 2009-09-30
 *
 * @param <K>
 * @param <L>
 */

public class CounterMap<K,L> extends FactoryDefaultMap<K,Counter<L>> implements ICounterMap<K,L,Double,Counter<L>> {
	public CounterMap() {
		super(new Counter<L>().new CounterFactory());
	}
	
	public Counter<L> getCounter(K key) {
		return this.get(key);
	}
	
	public Double getCount(K key1, L key2) {
		return this.getCounter(key1).getT(key2);
	}
	
	public Double increment(K key1, L key2) { return incrementBy(key1,key2,1.0); }
	public Double incrementBy(K key1, L key2, Double delta) {
		return this.getCounter(key1).incrementBy(key2, delta);
	}
	
	/**
	 * @param sep Array with at least 2 Strings: an entry separator ("," by default, if this is {@code null}), and a key-value separator ("=" by default). 
	 * If 4 Strings are specified, the first pair will be used for the outer display, and the second pair for the inner display (of the contained {@link IntCounter}s).  
	 */
	public String toString(String[] sep) {
		return toString(Double.MIN_VALUE, sep);
	}
	
	/**
	 * @param valueThreshold
	 * @param sep Array with at least 2 Strings: an entry separator ("," by default, if this is {@code null}), and a key-value separator ("=" by default). 
	 * If 4 Strings are specified, the first pair will be used for the outer display, and the second pair for the inner display (of the contained {@link IntCounter}s).  
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
        this.forEachEntry(new TObjectObjectProcedure<K,Counter<L>>() {
            private boolean first = true;
            public boolean execute(K key, Counter<L> value) {
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
