package edu.cmu.cs.lti.ark.fn.parsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author sthomson@cs.cmu.edu
 */
public class SemaforParseTest {
	final ObjectMapper mapper = new ObjectMapper();

	@Test
	@SuppressWarnings("ConstantConditions")
	public void testReadJson() throws URISyntaxException, IOException {
		final File jsonFile = new File(getClass().getClassLoader().getResource("fixtures/semaforParse.json").toURI());
		final SemaforParse parse = mapper.readValue(jsonFile, SemaforParse.class);
		assertEquals(14, parse.tokens.size());
		assertEquals(5, parse.frames.size());
		assertEquals(1, parse.frames.get(0).frameElements.size());
	}
	@Test
	@SuppressWarnings("ConstantConditions")
	public void testWriteJson() throws URISyntaxException, IOException {
		final File jsonFile = new File(getClass().getClassLoader().getResource("fixtures/semaforParse.json").toURI());

		final SemaforParse parse = mapper.readValue(jsonFile, SemaforParse.class);
		final String outputLine = mapper.writeValueAsString(parse);
		assertFalse(outputLine.contains("\n"));
	}
}
