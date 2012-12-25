package edu.cmu.cs.lti.ark.util;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableList.of;
import static edu.cmu.cs.lti.ark.util.IntRanges.range;
import static org.junit.Assert.assertEquals;

/**
 * @author sthomson@cs.cmu.edu
 */
public class IntRangesTest {
	private final ImmutableList<Integer> EMPTY = of();

	@Test
	public void testTwoSidedRange() {
		assertEquals(of(1, 2), copyOf(range(1, 3)));
	}

	@Test
	public void testOneSidedRange()  {
		assertEquals(of(0, 1, 2), copyOf(range(3)));
	}

	@Test
	public void testRangeWithEqualBoundsIsEmpty() {
		assertEquals(EMPTY, copyOf(range(3, 3)));
	}

	@Test
	public void testRangeWithOutOfOrderBoundsIsEmpty() {
		assertEquals(EMPTY, copyOf(range(3, -3)));
	}

	@Test
	public void testRangeWithNegativeBoundIsEmpty() {
		assertEquals(EMPTY, copyOf(range(-3)));
	}

	@Test
	public void testRangeWithNegativeBoundsWorks() {
		assertEquals(of(-3, -2), copyOf(range(-3, -1)));
	}
}
