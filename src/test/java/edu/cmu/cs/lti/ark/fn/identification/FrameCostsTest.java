package edu.cmu.cs.lti.ark.fn.identification;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author sthomson@cs.cmu.edu
 */
public class FrameCostsTest {
	@Test
	public void testLoad() throws Exception {
		final FrameCosts costs = FrameCosts.load();
		assertEquals(0.36f, costs.getCost("Text", "Entity"), .01f);
		assertEquals(FrameCosts.DEFAULT_COST, costs.getCost("Text", "Not_a_real_frame"), .01f);
	}
}
