package edu.cmu.cs.lti.ark.fn.parsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

/**
 * @author sthomson@cs.cmu.edu
 */
public class SemaforParseTest {

	@Test
	@SuppressWarnings("ConstantConditions")
	public void testReadJson() throws URISyntaxException, IOException {
		final File jsonFile = new File(getClass().getClassLoader().getResource("fixtures/semaforParse.json").toURI());
		final ObjectMapper mapper = new ObjectMapper();
		final SemaforParse parse = mapper.readValue(jsonFile, SemaforParse.class);
		assertEquals(14, parse.text.size());
		assertEquals(5, parse.frames.size());
		assertEquals(1, parse.frames.get(0).frame_elements.size());
	}
}
