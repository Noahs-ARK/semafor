package edu.cmu.cs.lti.ark.fn.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.io.Files;
import edu.cmu.cs.lti.ark.fn.parsing.RankedScoredRoleAssignment;
import edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Collections2.filter;
import static edu.cmu.cs.lti.ark.fn.evaluation.PrepareFullAnnotationJson.getSemaforParse;
import static edu.cmu.cs.lti.ark.fn.evaluation.PrepareFullAnnotationJson.parseRoleAssignments;
import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * @author sthomson@cs.cmu.edu
 */
public class PrepareFullAnnotationJsonTest {
	private File frameElementsFile;
	private File tokenizedFile;
	private File jsonFile;

	private static String morningSentence =
			"I walked along Shattuck between Delaware and Cedar at a few minutes before eight this morning .";
	private static String morningArgResult =
			"0\t27.78959712412228\t3\tQuantity\ta.d\t9_10\ta few\t0\tQuantity\t10\tIndividuals\t11\n" +
			"0\t94.61497462141946\t3\tSelf_motion\twalked.v\t1\twalked\t0\tTime\t2:3\tSelf_mover\t0\n" +
			"0\t25.37947913514716\t2\tCalendric_unit\tminutes.n\t11\tminutes\t0\tUnit\t11\n" +
			"0\t25.163207951563482\t2\tCardinal_numbers\teight.c\t13\teight\t0\tNumber\t13\n" +
			"0\t30.11051956886748\t1\tCalendric_unit\tmorning.n\t15\tmorning\t0\n";

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

	@Test
	public void testFramesWithSameNameAreNotCombined() throws JsonProcessingException {
		final Collection<RankedScoredRoleAssignment> roleAssignments =
				parseRoleAssignments(Arrays.asList(morningArgResult.split("\n"))).get(0);
		final List<String> tokens = Arrays.asList(morningSentence.split(" "));
		final SemaforParseResult semaforParseResult = getSemaforParse(roleAssignments, tokens);
		final Collection<SemaforParseResult.Frame> calendricUnitFrames =
				filter(semaforParseResult.frames, new Predicate<SemaforParseResult.Frame>() {
					@Override public boolean apply(SemaforParseResult.Frame input) {
						return input.target.name.equals("Calendric_unit");
					}
				});
		// the two Calendric_unit frames should not be combined into one
		Assert.assertEquals(2, calendricUnitFrames.size());
	}
}
