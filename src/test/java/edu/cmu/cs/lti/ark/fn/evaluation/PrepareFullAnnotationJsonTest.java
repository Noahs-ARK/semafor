package edu.cmu.cs.lti.ark.fn.evaluation;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;
import java.net.URISyntaxException;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * @author sthomson@cs.cmu.edu
 */
public class PrepareFullAnnotationJsonTest {
	private File frameElementsFile;
	private File tokenizedFile;
	private File jsonFile;

	@Before
	@SuppressWarnings("ConstantConditions")
	public void setUp() throws URISyntaxException {
		final ClassLoader classLoader = getClass().getClassLoader();
		frameElementsFile = new File(classLoader.getResource("fixtures/example.frame.elements").toURI());
		tokenizedFile = new File(classLoader.getResource("fixtures/example.tokenized").toURI());
		jsonFile = new File(classLoader.getResource("fixtures/example.json").toURI());
	}

	@Test
	public void testWriteJsonForPredictions() throws Exception {
		final String expected = Files.toString(jsonFile, Charsets.UTF_8);
		final StringWriter output = new StringWriter();
		final FileReader tokenizedInput = new FileReader(tokenizedFile);
		final FileReader feInput = new FileReader(frameElementsFile);
		try {
			PrepareFullAnnotationJson.writeJsonForPredictions(tokenizedInput, feInput, output);
			Assert.assertEquals(expected, output.toString());
		} finally {
			closeQuietly(feInput);
			closeQuietly(tokenizedInput);
		}
	}
}
