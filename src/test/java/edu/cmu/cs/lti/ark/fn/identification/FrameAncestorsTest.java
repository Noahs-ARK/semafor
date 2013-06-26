package edu.cmu.cs.lti.ark.fn.identification;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author sthomson@cs.cmu.edu
 */
public class FrameAncestorsTest {
	@Test
	public void testLoad() throws IOException {
		final FrameAncestors ancestors = FrameAncestors.load();
		assertEquals(ancestors.getAncestors("Text"), ImmutableSet.of("Entity", "Artifact"));
		assertEquals(ancestors.getAncestors("Not_a_real_frame"), ImmutableSet.<String>of());
	}
}
