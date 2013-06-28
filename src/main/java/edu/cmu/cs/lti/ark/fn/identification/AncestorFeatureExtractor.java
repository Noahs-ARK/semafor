package edu.cmu.cs.lti.ark.fn.identification;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;

import java.io.IOException;
import java.util.Map;

import static com.google.common.collect.Iterables.concat;

/**
 * @author sthomson@cs.cmu.edu
 */
public class AncestorFeatureExtractor extends BasicFeatureExtractor {

	private final FrameAncestors ancestors;

	public AncestorFeatureExtractor(FrameAncestors ancestors) {
		this.ancestors = ancestors;
	}

	public static AncestorFeatureExtractor load() throws IOException {
		return new AncestorFeatureExtractor(FrameAncestors.load());
	}

	public Map<String, Map<String, Double>> extractFeaturesByName(Iterable<String> frameNames,
																  int[] targetTokenIdxs,
																  Sentence sentence) {
		final Map<String, Double> baseFeatures = getBaseFeatures(targetTokenIdxs, sentence);
		return conjoinWithFrameAndAncestors(frameNames, baseFeatures);
	}

	protected Map<String, Map<String, Double>> conjoinWithFrameAndAncestors(Iterable<String> frameNames,
																			Map<String, Double> baseFeatures) {
		final Map<String, Map<String, Double>> results = Maps.newHashMap();
		// conjoin base features with frame and ancestors
		for (String frame : frameNames) {
			final Iterable<String> frameAndAncestors = concat(ImmutableSet.of(frame), ancestors.getAncestors(frame));
			final Map<String, Double> featuresForFrame = conjoin("f:" + frame, baseFeatures);
			for (String ancestor : frameAndAncestors) {
				featuresForFrame.putAll(conjoin("af:" + ancestor, baseFeatures));
			}
			results.put(frame, featuresForFrame);
		}
		return results;
	}
}
