/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * Interner.java is part of SEMAFOR 2.0.
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

import java.util.*;

public class Interner<T> {

  protected static Interner interner = new Interner();

  /**
   * For getting the instance that global methods use.
   */
  public static Interner getGlobal() {
    return interner;
  }

  /**
   * For supplying a new instance for the global methods to
   * use. Returns the previous global interner.
   */
  public static Interner setGlobal(Interner interner) {
    Interner oldInterner = Interner.interner;
    Interner.interner = interner;
    return oldInterner;
  }

  /**
   * Returns a unique object o' that .equals the argument o.  If o
   * itself is returned, this is the first request for an object
   * .equals to o.
   */
  public static Object globalIntern(Object o) {
    return getGlobal().intern(o);
  }


  protected Map<T,T> map = new WeakHashMap<T,T>();

  public void clear() { map = new WeakHashMap<T,T>(); }
  
  /**
   * Returns a unique object o' that .equals the argument o.  If o
   * itself is returned, this is the first request for an object
   * .equals to o.
   */
  public T intern(T o) {
    T i = map.get(o);
    if (i == null) {
      i = o;
      map.put(i, i);
    }
//    else {
//      System.err.println("Found dup for " + o);
//    }
    return i;
  }

  /**
   * Returns a unique object o' that .equals the argument o.  If o
   * itself is returned, this is the first request for an object
   * .equals to o.
   */
  public Set<T> internAll(Set<T> s) {
    Set<T> result = new HashSet<T>();
    for (T o : s) {
      result.add(intern(o));
    }
    return result;
  }

  public int size() {
    return map.size();
  }

  /**
   * Test method: interns its arguments and says whether they == themselves.
   */
  public static void main(String[] args) {
    for (int i = 0; i < args.length; i++) {
      String str = args[i];
      System.out.println(Interner.globalIntern(str) == str);
    }
  }

}
