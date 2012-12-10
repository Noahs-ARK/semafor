package edu.cmu.cs.lti.ark.util;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static edu.cmu.cs.lti.ark.util.IntRanges.range;
import static junit.framework.Assert.assertEquals;

/**
 * @author sthomson@cs.cmu.edu
 */
public class IntRangesTest {
	@Test
	public void testTwoSidedRange() {
		assertEquals(ImmutableList.of(1, 2), range(1, 3));
	}

	@Test
	public void testOneSidedRange()  {
		assertEquals(ImmutableList.of(0, 1, 2), range(3));
	}

	@Test
	public void testRangeWithEqualBoundsIsEmpty() {
		assertEquals(ImmutableList.<Integer>of(), range(3, 3));
	}

	@Test
	public void testRangeWithOutOfOrderBoundsIsEmpty() {
		assertEquals(ImmutableList.<Integer>of(), range(3, -3));
	}

	@Test
	public void testRangeWithNegativeBoundIsEmpty() {
		assertEquals(ImmutableList.<Integer>of(), range(-3));
	}

	@Test
	public void testRangeWithNegativeBoundsWorks() {
		assertEquals(ImmutableList.of(-3, -2), range(-3, -1));
	}
}
