/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * FactoryDefaultMap.java is part of SEMAFOR 2.0.
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

import java.util.Set;

import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectHashingStrategy;
import gnu.trove.TObjectIdentityHashingStrategy;

/**
 * Class mapping a key to a value, both objects. As in a {@link Counter}, 
 * if lookup is performed and no value is found for a specified key, 
 * a default value instance will be returned. This value instance 
 * is created with a {@link DefaultValueFactory} instance, 
 * which must be passed into the constructor of this class. 
 * @author Nathan Schneider (nschneid)
 * @since 2009-09-30
 *
 * @param <K>
 * @param <V>
 */
public class FactoryDefaultMap<K,V> extends THashMap<K,V> {
	public interface DefaultValueFactory<U> {
		public U newDefaultValue();
	}

	public static class THashSetFactory<T> implements DefaultValueFactory<THashSet<T>> {
		public THashSet<T> newDefaultValue() { return new THashSet<T>(); }
	}
	public static class IdentityTHashSetFactory<T> implements DefaultValueFactory<THashSet<T>> {
		public THashSet<T> newDefaultValue() { return new THashSet<T>(new TObjectIdentityHashingStrategy<T>()); }
	}
	
	public DefaultValueFactory<V> factory;
	
	public FactoryDefaultMap(DefaultValueFactory<V> factory) {
		this.factory = factory;
	}
	public FactoryDefaultMap(DefaultValueFactory<V> factory, TObjectHashingStrategy<K> hashingStrategy) {
		super(hashingStrategy);
		this.factory = factory;
	}
	
	public V get(Object key) {
		V v = super.get(key);
		if (v==null) {
			v = factory.newDefaultValue();
			this.put((K)key, v);
		}
		return v;
	}
}
