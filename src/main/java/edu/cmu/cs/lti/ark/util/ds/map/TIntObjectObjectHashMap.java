/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * TIntObjectObjectHashMap.java is part of SEMAFOR 2.0.
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
import gnu.trove.TIntObjectHashMap;



public class TIntObjectObjectHashMap<V,K>
{
	TIntObjectHashMap<THashMap<V,K>> map;
	
	public TIntObjectObjectHashMap()
	{
		map = new TIntObjectHashMap<THashMap<V,K>>();
	}
	
	public K get(int one, V two)
	{
		THashMap<V,K> val = map.get(one);
		if(val==null)
			return null;
		return val.get(two);
	}
	
	public K put(int one, V two, K val)
	{
		THashMap<V,K> firstVal = map.get(one);
		if(firstVal==null)
		{
			firstVal = new THashMap<V,K>();
			map.put(one, firstVal);
		}
		return firstVal.put(two, val);
	}
	
	public int[] keys()
	{
		return map.keys();
	}	
	
	public THashMap<V,K> getValueMap(int i)
	{
		return map.get(i);
	}
}
