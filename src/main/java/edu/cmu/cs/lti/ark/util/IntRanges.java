package edu.cmu.cs.lti.ark.util;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableSortedSet;

import javax.annotation.concurrent.Immutable;
import java.util.Iterator;

import static com.google.common.collect.ImmutableSortedSet.copyOf;

/**
 * We all miss these from Python
 *
 * @author sthomson@cs.cmu.edu
 */
public class IntRanges {
	@Immutable
	public static class XRange  implements Iterable<Integer> {
		private final int start;
		private final int end;

		public XRange(int start, int end) {
			this.start = start;
			this.end = end;
		}

		@Override
		public Iterator<Integer> iterator() {
			return new AbstractIterator<Integer>() {
				private int counter = start - 1;

				@Override
				protected Integer computeNext() {
					counter++;
					if(counter < end) return counter;
					return endOfData();
				}
			};
		}
	}

	public static XRange xrange(int start, int end) { return new XRange(start, end); }

	public static XRange xrange(int end) { return new XRange(0, end); }

	public static ImmutableSortedSet<Integer> range(int start, int end) { return copyOf(xrange(start, end)); }

	public static ImmutableSortedSet<Integer> range(int end) { return copyOf(xrange(end)); }
}