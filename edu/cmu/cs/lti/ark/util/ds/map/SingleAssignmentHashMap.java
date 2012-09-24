/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * SingleAssignmentHashMap.java is part of SEMAFOR 2.0.
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

import gnu.trove.THashMap;
import gnu.trove.TObjectHashingStrategy;

/**
 * A hash map whose {@link #put(Object, Object)} method prohibits (for safety's sake) setting a new value for a key 
 * whose current value is not {@code null}.
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2009-04-03
 *
 * @param <K> key type
 * @param <V> value type
 */
public class SingleAssignmentHashMap<K, V> extends THashMap<K, V> implements java.io.Serializable {
	/** What to do if {@link SingleAssignmentHashMap#put(Object, Object)} is invoked for a key that already has a non-{@code null} value. 
	 * <ul>
	 * <li>{@link #ERROR} indicates that an error should be printed</li>
	 * <li>{@link #IGNORE} indicates that the new value should be ignored without error</li>
	 * <li>{@link #REPLACE} indicates that the new value should replace the old one without error</li>
	 * <li>{@link #DIFFERENT} indicates that an error should be printed if the new value is 
	 * different from the old one; if they are the same (comparison via {@code equals()}) the new 
	 * value will be ignored silently</li>
	 * </ul>
	 * 
	 * {@link #ERROR} is the default for a {@link SingleAssignmentHashMap}. Such a policy can be 
	 * specified at the instance level via a constructor, or as an argument to 
	 * {@link SingleAssignmentHashMap#put(Object, Object, ReassignmentPolicy)} (this will override 
	 * any instance-level policy).
	 */
	public enum ReassignmentPolicy { ERROR, DIFFERENT, IGNORE, REPLACE };
	
	/** Instance-level reassignment policy */
	protected ReassignmentPolicy _reassign;
	
	public SingleAssignmentHashMap() {
		this(ReassignmentPolicy.ERROR);
	}
	public SingleAssignmentHashMap(ReassignmentPolicy reassign) {
		
	}
	public SingleAssignmentHashMap(TObjectHashingStrategy<K> hashingStrategy) {
		this(hashingStrategy, ReassignmentPolicy.ERROR);
	}
	public SingleAssignmentHashMap(TObjectHashingStrategy<K> hashingStrategy, ReassignmentPolicy reassign) {
		super(hashingStrategy);
	}
	
	/** Invokes {@link #put(Object, Object, ReassignmentPolicy)} with this instance's reassignment 
	 * policy ({@link ReassignmentPolicy#ERROR} by default).
	 */
	@Override
	public V put(K key, V newValue) {
		return put(key, newValue, _reassign);
	}
	
	/** 
	 * Puts the value 'newValue' in the hash map with key 'key', subject to the reassignment policy 
	 * (which matters if the current value stored for 'key' is non-{@code null}). {@link #reset(Object)} can 
	 * be called to restore the value to {@code null}. Equivalent to {@link #putIfAbsent(Object, Object)}.
	 * @param key
	 * @param newValue
	 * @param reassign
	 * @return The value that was already present in the map, or null if the new value was added
	 * @see #reset(Object)
	 */
	public V put(K key, V newValue, ReassignmentPolicy reassign) {
		V oldValue = this.get(key);
		
		if (oldValue!=null && reassign!=ReassignmentPolicy.REPLACE) {
			if (reassign==ReassignmentPolicy.ERROR || (reassign==ReassignmentPolicy.DIFFERENT && !oldValue.equals(newValue)))
				System.err.println("Cannot reassign to a key a different non-null value in a SingleAssignmentHashMap without first calling reset()");
		}
		else
			super.put(key, newValue);
		return oldValue;
	}
	
	/**
	 * Resets the value at the specified key to {@code null}
	 * @param key
	 * @return The old value at the specified key
	 */
	public V reset(K key) {
		V oldValue = this.get(key);
		super.put(key, null);
		return oldValue;
	}
}
