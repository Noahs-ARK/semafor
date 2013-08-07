package edu.cmu.cs.lti.ark.fn.parsing;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.io.Resources;
import com.google.common.primitives.Chars;
import org.junit.Test;

import java.io.IOException;
import java.util.regex.Pattern;

import static com.google.common.io.Resources.getResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author sthomson@cs.cmu.edu
 */
public class SemaforParseResultTest {
	private final static String PARSE_FILENAME = "fixtures/semaforParse.json";

	@Test
	public void testReadJson() throws IOException {
		final String jsonString = Resources.toString(getResource(PARSE_FILENAME), Charsets.UTF_8);
		final SemaforParseResult parse = SemaforParseResult.fromJson(jsonString);
		assertEquals(14, parse.tokens.size());
		assertEquals(5, parse.frames.size());
		assertEquals(1, parse.frames.get(0).annotationSets.size());
	}

	@Test
	public void testJsonOutputIsOnlyOneLine() throws IOException {
		final String jsonFixture = Resources.toString(getResource(PARSE_FILENAME), Charsets.UTF_8);
		final SemaforParseResult parse = SemaforParseResult.fromJson(jsonFixture);
		assertFalse(parse.toJson().contains("\n"));
	}

	@Test
	public void testJsonContainsSameChars() throws IOException {
		final String jsonFixture = Resources.toString(getResource(PARSE_FILENAME), Charsets.UTF_8);
		final SemaforParseResult parse = SemaforParseResult.fromJson(jsonFixture);
		// check that content of output is the same (disregarding order and whitespace)
		final Pattern whiteSpace = Pattern.compile("\\s");
		final String expectedJsonString = whiteSpace.matcher(jsonFixture).replaceAll("");
		final String actualJsonString = whiteSpace.matcher(parse.toJson()).replaceAll("");
		final Multiset<Character> expectedCharCounts = HashMultiset.create(Chars.asList(expectedJsonString.toCharArray()));
		final Multiset<Character> actualCharCounts = HashMultiset.create(Chars.asList(actualJsonString.toCharArray()));
		assertEquals(expectedCharCounts, actualCharCounts);
	}
}
