/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * Alphabet.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.optimization;

import java.io.Serializable;
import java.util.*;

import edu.cmu.cs.lti.ark.util.Interner;
import gnu.trove.*;

public class Alphabet implements Serializable
{
	/**
	 * Maps each unique string to a unique index.
	 * 
	 * @author dipanjan
	 */
	private static final long serialVersionUID = -3475498139713667452L;
	private ArrayList<String> m_decode;
	private TObjectIntHashMap<String> m_encode;
	private Interner<String> m_interner = new Interner<String>();
	private static int ALPHABET_INITIAL_CAPACITY = 2000000;

	public Alphabet() {
		m_decode = new ArrayList<String>(ALPHABET_INITIAL_CAPACITY);
		m_encode = new TObjectIntHashMap<String>(ALPHABET_INITIAL_CAPACITY, new TObjectIdentityHashingStrategy<String>());
	}

	public int getNumEntries() {
		return m_decode.size();
	}
	
	/**
	 * Returns String corresponding to given integer i, or "<UNK>" if i is out of range.
	 * @param i
	 * @return
	 */
	public String getString(int i) {
		if (i <= m_decode.size() && i >= 1) 
			return m_decode.get(i-1);
		else
			return "<UNK>";
	}

	/**
	 * Adds given String to this Alphabet if it has not already been added. It is assigned 
	 * a new unique ID equal to the new size of the Alphabet after performing the addition.  
	 * @param s String to add
	 */
	public void addString(String s) {
		String ss = m_interner.intern(s);
		if (!m_encode.containsKey(ss)) {
			m_encode.put(ss, m_decode.size());
			m_decode.add(ss);
			//m_decode.add(ss);
			//m_encode.put(ss, m_decode.size());
		}
	}

	public boolean checkString(String s) {
		String ss = m_interner.intern(s);
		if (m_encode.containsKey(ss)) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Returns int corresponding to given String s. If s has not been seen before,
	 * adds s to m_decode and m_encode and returns the hashed index of s in m_encode. 
	 * @param s
	 * @return
	 */
	public int getInt(String s) {
		boolean check = checkString(s);
		String ss = m_interner.intern(s);
		if(!check)
		{
			m_decode.add(ss);
			int newind = m_decode.size();
			m_encode.put(ss, newind);
			return newind;
		}
		int ind = m_encode.get(ss);
		return ind;		
	}
	
	public ArrayList<String> getEntries()
	{
		return m_decode;
	}
} 

