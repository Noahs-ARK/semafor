/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * Zip.java is part of SEMAFOR 2.0.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import edu.cmu.cs.lti.ark.util.ds.Pair;

/**
 * Allows for parallel iteration over two {@link Collection}s or two {@link Iterator}s of equal size, 
 * without the need for an index variable.
 * Attempting to iterate over two {@link Collection}s of unequal size causes an exception, as does 
 * attempting to iterate past the end of the shorter of two {@link Iterator}s. (For Python, similar 
 * functionality is built in with the 'zip()' function.)
 * 
 * This can be used as follows:
 * <pre>
 * {@code
 * List<String> names = ...
 * List<Integer> numbers = ...
 * for (Pair&lt;String, Integer&gt; pair : Zip.zip2(names, numbers)) {
 *   ...
 * }
 * </pre>
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2009-11-25
 *
 * @param <S>
 * @param <T>
 */
public class Zip {

	public static class Zip2<S, T> implements Iterable<Pair<S,T>>, Iterator<Pair<S,T>> {
		protected Iterator<? extends S> iter1;
		protected Iterator<? extends T> iter2;
		protected int remaining;
		
		public Zip2(Collection<? extends S> items1, Collection<? extends T> items2) {
			this(items1.iterator(), items2.iterator(), 0);
		}
		
		public Zip2(Iterator<? extends S> i1, Iterator<? extends T> i2) {
			this(i1,i2,0);
		}
		
		public Zip2(Collection<? extends S> items1, Collection<? extends T> items2, int n) {
			if (items1.size()!=items2.size())
				throw new IllegalArgumentException("Collections passed to Zip constructor must have the same size");
			int nonnegN = (n>=0) ? n : items1.size()+n;	// Subtract from the size of the collection
			init(items1.iterator(), items2.iterator(), nonnegN);
		}
		
		public Zip2(Iterator<? extends S> i1, Iterator<? extends T> i2, int n) {
			if (n<0)
				throw new IllegalArgumentException("'n' cannot be negative for zipping Iterators");
			init(i1,i2,n);
		}
		
		protected void init(Iterator<? extends S> i1, Iterator<? extends T> i2, int n) {
			iter1 = i1;
			iter2 = i2;
			remaining = (n>0) ? n : -1;
		}
		
		@Override
		public Iterator<Pair<S,T>> iterator() {
			return this;
		}

		@Override
		public boolean hasNext() {
			if (remaining==0)
				return false;
			if (iter1.hasNext()!=iter2.hasNext())
				throw new IllegalStateException("Parallel iterators in Zip must have the same number of elements");
			return (iter1.hasNext() && iter2.hasNext());
		}

		@Override
		public Pair<S, T> next() {
			if (remaining==0)
				return null;
			S item1 = iter1.next();
			T item2 = iter2.next();
			if (remaining>0)
				remaining--;
			return new Pair<S,T>(item1, item2);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Zip2.remove() is unsupported");
		}

	}
	
	
	
	public static <S,T> Zip2<S,T> zip2(Collection<? extends S> items1, Collection<? extends T> items2) {
		return new Zip2<S,T>(items1, items2);
	}
	public static <S,T> Zip2<S,T> zip2(S[] items1, Collection<? extends T> items2) {
		return new Zip2<S,T>(Arrays.asList(items1), items2);
	}
	public static <S,T> Zip2<S,T> zip2(Collection<? extends S> items1, T[] items2) {
		return new Zip2<S,T>(items1, Arrays.asList(items2));
	}
	public static <S,T> Zip2<S,T> zip2(S[] items1, T[] items2) {
		return new Zip2<S,T>(Arrays.asList(items1), Arrays.asList(items2));
	}
	public static <S,T> Zip2<S,T> zip2(Iterator<? extends S> iter1, Iterator<? extends T> iter2) {
		return new Zip2<S,T>(iter1, iter2);
	}
	/**
	 * Zip two Collections of items so they can be iterated in parallel. The Collections must be the same size; 
	 * otherwise the constructor will raise an exception.
	 * @param <S>
	 * @param <T>
	 * @param items1
	 * @param items2
	 * @param n Limit on the number of items to iterate through, starting with the first element of each Collection. 
	 * If negative, it will be added to the size of the Collections. {@literal 0} is equivalent to not specifying a limit.
	 * @return
	 */
	public static <S,T> Zip2<S,T> zip2(Collection<? extends S> items1, Collection<? extends T> items2, int n) {
		return new Zip2<S,T>(items1, items2, n);
	}
	/**
	 * Zip two Iterators so they can be iterated in parallel. Attempting to iterate past the limit of either of
	 * the iterators will raise an exception.
	 * @param <S>
	 * @param <T>
	 * @param iter1
	 * @param iter2
	 * @param n Limit on the number of iterations. Must be nonnegative; {@literal 0} is equivalent to not specifying a limit.
	 * @return
	 */
	public static <S,T> Zip2<S,T> zip2(Iterator<? extends S> iter1, Iterator<? extends T> iter2, int n) {
		return new Zip2<S,T>(iter1, iter2, n);
	}
	
	/** Test this class with a size-2 zip */
	public static void main(String[] args) {
		Collection<String> names = Arrays.asList(new String[]{"Joe","Shirley","Doug","Rita"});
		Collection<Integer> nums = Arrays.asList(new Integer[]{10,15,42,-8});
		
		for (Pair<String,Integer> pair : Zip.zip2(names,nums)) {
			String s = pair.getFirst();
			Integer i = pair.getSecond();
			System.out.println(s + ":" + i);
		}
		
		Zip.Zip2<String,Integer> z;
		names = Arrays.asList(new String[]{"Joe","Shirley","Doug"});
		nums = Arrays.asList(new Integer[]{10,15,42,-8});
		try {
			z = Zip.zip2(names,nums);
		}
		catch (IllegalArgumentException ex) { System.out.println("Correctly threw exception with unevenly-sized Collections (" + ex.getMessage() + ")"); }
		
		
		z = Zip.zip2(names.iterator(), nums.iterator());
		try {
			for (Pair<String,Integer> pair : z) {
				String s = pair.getFirst();
				Integer i = pair.getSecond();
				System.out.println(s + ":" + i);
			}
		} catch (IllegalStateException ex) { System.out.println("Correctly threw exception after reaching the end of the shorter of two Iterators (" + ex.getMessage() + ")"); }
		
		
		// With limits
		
		names = Arrays.asList(new String[]{"Joe","Shirley","Doug","Rita"});
		nums = Arrays.asList(new Integer[]{10,15,42,-8});
		
		for (Pair<String,Integer> pair : Zip.zip2(names,nums,3)) {
			String s = pair.getFirst();
			Integer i = pair.getSecond();
			if (i==-8) throw new RuntimeException("Error: iteration beyond limit");
			System.out.println(s + ":" + i);
		}
		
		names = Arrays.asList(new String[]{"Joe","Shirley","Doug"});
		nums = Arrays.asList(new Integer[]{10,15,42,-8});
		try {
			z = Zip.zip2(names,nums,3);
		}
		catch (IllegalArgumentException ex) { System.out.println("Correctly threw exception with unevenly-sized Collections (" + ex.getMessage() + ")"); }
		
		
		z = Zip.zip2(names.iterator(), nums.iterator(), 3);
		try {
			for (Pair<String,Integer> pair : z) {
				String s = pair.getFirst();
				Integer i = pair.getSecond();
				System.out.println(s + ":" + i);
			}
		} catch (IllegalStateException ex) { System.err.println("Zip of two Iterators should not have thrown an exception since iteration was limited"); }
		
		z = Zip.zip2(names.iterator(), nums.iterator(), 4);
		try {
			for (Pair<String,Integer> pair : z) {
				String s = pair.getFirst();
				Integer i = pair.getSecond();
				System.out.println(s + ":" + i);
			}
		} catch (IllegalStateException ex) { System.out.println("Correctly threw exception after reaching the end of the shorter of two Iterators--limit was too large (" + ex.getMessage() + ")"); }
		
	}
	
	
}
