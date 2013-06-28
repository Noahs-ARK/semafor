package edu.cmu.cs.lti.ark.fn.identification;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author sthomson@cs.cmu.edu
 */
public class SennaTest {
	private Senna SENNA;
	double[] EXPECTED_TEXT_EMBEDDING = {
			-1.76935000e+00,  -7.35982000e-01,   1.34140000e+00,
			4.98062000e-01,   1.41250000e+00,  -1.15633000e-01,
			-3.76426000e-01,  -2.41127000e+00,  -4.81990000e-01,
			9.61057000e-02,   4.24913000e-01,  -9.26365000e-01,
			1.38837000e+00,  -1.10676000e+00,   3.71972000e-01,
			1.54616000e+00,  -5.38165000e-01,  -1.51114000e+00,
			5.29418000e-01,   1.48786000e+00,  -6.43525000e-01,
			6.60863000e-01,   3.22748000e-01,  -1.43554000e-03,
			3.64180000e-01,  -2.69707000e+00,   9.97264000e-01,
			3.23228000e-01,  -3.18516000e-01,   7.69400000e-01,
			2.29630000e-02,   2.37799000e-01,  -1.74260000e+00,
			9.82318000e-02,   2.14434000e-01,   1.04227000e+00,
			-1.35076000e+00,  -1.09729000e+00,   5.50396000e-01,
			-2.54967000e-01,  -1.10034000e+00,  -1.51361000e+00,
			1.69342000e-01,   4.55995000e-01,  -5.60956000e-01,
			4.76936000e-01,  -1.72507000e-01,  -1.28186000e+00,
			-6.04337000e-02,   4.39739000e-01};

	@Before
	public void setUp() throws IOException {
		SENNA = Senna.load();
	}

	@Test
	public void testGetEmbedding() {
		final Optional<double[]> textEmbedding = SENNA.getEmbedding("text");
		assertArrayEquals(EXPECTED_TEXT_EMBEDDING, textEmbedding.get(), .00001);
	}

	@Test
	public void testGetEmbeddingForFakeWordReturnsAbsent() {
		final Optional<double[]> textEmbedding = SENNA.getEmbedding("fake word");
		assertFalse(textEmbedding.isPresent());
	}
}
