package edu.cmu.cs.lti.ark.fn.identification;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Iterables.concat;

/**
 * @author sthomson@cs.cmu.edu
 */
public class AncestorFeatureExtractor extends IdFeatureExtractor {
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
		return conjoinAll(frameNames, baseFeatures);
	}

	public Set<String> getConjoinedFeatureNames(Iterable<String> frameNames, String feature) {
		Set<String> results = Sets.newHashSet();
		for (String frame : frameNames) {
			results.add(SPACE.join("f:" + frame, feature));
			results.add(SPACE.join("af:" + frame, feature));
		}
		return results;
	}


	@Override
	public <V extends Number> Map<String, Map<String, V>>
			conjoinAll(Iterable<String> frameNames, Map<String, V> baseFeatures) {
		final Map<String, Map<String, V>> results = Maps.newHashMap();
		// conjoin base features with frame and ancestors
		for (String frame : frameNames) {
			final Iterable<String> frameAndAncestors = concat(ImmutableSet.of(frame), ancestors.getAncestors(frame));
			final Map<String, V> featuresForFrame = conjoin("f:" + frame, baseFeatures);
			for (String ancestor : frameAndAncestors) {
				featuresForFrame.putAll(conjoin("af:" + ancestor, baseFeatures));
			}
			results.put(frame, featuresForFrame);
		}
		return results;
	}
}
