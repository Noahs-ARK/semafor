package edu.cmu.cs.lti.ark.fn.identification;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Iterables.concat;

/**
 * @author sthomson@cs.cmu.edu
 */
public abstract class FrameFeatureExtractor {
	protected static final Joiner SPACE = Joiner.on(" ");

	public static class BasicFrameFeatureExtractor extends FrameFeatureExtractor {
		@Override
		public Map<String, Integer> extractFeatures(String frame) {
			final IntCounter<String> results = new IntCounter<String>();
			results.increment("f:" + frame);
			return results;
		}
	}

	public static class AncestorFrameFeatureExtractor extends FrameFeatureExtractor {
		private final FrameAncestors ancestors;

		public AncestorFrameFeatureExtractor(FrameAncestors ancestors) {
			this.ancestors = ancestors;
		}

		public static AncestorFrameFeatureExtractor load() throws IOException {
			return new AncestorFrameFeatureExtractor(FrameAncestors.load());
		}

		@Override
		public Map<String, Integer> extractFeatures(String frame) {
			final IntCounter<String> results = new IntCounter<String>();
			results.increment("f:" + frame);
			for (String ancestor : concat(ImmutableSet.of(frame), ancestors.getAncestors(frame))) {
				results.increment("af:" + ancestor);
			}
			return results;
		}
	}

	public abstract Map<String, Integer> extractFeatures(String frameName);

	public Set<String> getAllConjoinedFeatureNames(Iterable<String> frameNames, String feature) {
		final Set<String> allFrameFeatureNames = Sets.newHashSet();
		for (String frame : frameNames) {
			allFrameFeatureNames.addAll(extractFeatures(frame).keySet());
		}
		Set<String> results = Sets.newHashSet();
		for (String featureName : allFrameFeatureNames) {
			results.add(SPACE.join(featureName, feature));
		}
		return results;
	}

	public <V extends Number> Map<String, Map<String, V>> conjoinAll(Iterable<String> frameNames,
																	 Map<String, V> baseFeatures) {
		final Map<String, Map<String, V>> results = Maps.newHashMap();
		// conjoin base features with frame and ancestors
		for (String frame : frameNames) {
			Map<String, V> featuresForFrame = Maps.newHashMap();
			final Map<String, Integer> frameFeatures = extractFeatures(frame);
			for (String frameFeatureName : frameFeatures.keySet()) {
				featuresForFrame.putAll(conjoin(frameFeatureName, baseFeatures));
			}
			results.put(frame, featuresForFrame);
		}
		return results;
	}

	public static <V extends Number> Map<String, V> conjoin(String name, Map<String, V> oldFeatures) {
		final Map<String, V> conjoinedFeatures = Maps.newHashMap();
		for (String feature : oldFeatures.keySet()) {
			conjoinedFeatures.put(SPACE.join(name, feature), oldFeatures.get(feature));
		}
		return conjoinedFeatures;
	}
}
