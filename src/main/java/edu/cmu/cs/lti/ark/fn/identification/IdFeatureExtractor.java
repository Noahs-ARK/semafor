package edu.cmu.cs.lti.ark.fn.identification;

import com.beust.jcommander.IStringConverter;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import gnu.trove.TIntDoubleHashMap;

import java.util.Map;

/**
 * @author sthomson@cs.cmu.edu
 */
public abstract class IdFeatureExtractor {
	// Command line option converters
	private static Map<String, Supplier<IdFeatureExtractor>> featureExtractorMap = ImmutableMap.of(
			"basic", new Supplier<IdFeatureExtractor>() {
				@Override public IdFeatureExtractor get() { return new BasicFeatureExtractor(); }
			} ,
			"ancestor", new Supplier<IdFeatureExtractor>() {
				@Override public IdFeatureExtractor get() { return AncestorFeatureExtractor.load(); }
			});
	public static class FeatureExtractorConverter implements IStringConverter<IdFeatureExtractor> {
		@Override public IdFeatureExtractor convert(String value) {
			return featureExtractorMap.get(value.trim().toLowerCase()).get();
		}
	}

	public abstract Map<String, Map<String, Double>> extractFeaturesByName(Iterable<String> frameNames,
																  int[] targetTokenIdxs,
																  Sentence sentence);

	public Map<String, TIntDoubleHashMap> extractFeaturesByIndex(Iterable<String> frames,
																 int[] targetTokenIdxs,
																 Sentence sentence,
																 Map<String, Integer> alphabet) {
		return convertToIndexes(extractFeaturesByName(frames, targetTokenIdxs, sentence), alphabet);
	}

	/** Replaces feature names with feature indexes */
	protected static Map<String, TIntDoubleHashMap> convertToIndexes(Map<String, Map<String, Double>> featuresByFrame,
																	 Map<String, Integer> alphabet) {
		final Map<String, TIntDoubleHashMap> results = Maps.newHashMap();
		for (String frame : featuresByFrame.keySet()) {
			final Map<String, Double> features = featuresByFrame.get(frame);
			final TIntDoubleHashMap featsForFrame = new TIntDoubleHashMap(features.size());
			for (String feat : features.keySet()) {
				if (alphabet.containsKey(feat)) {
					featsForFrame.put(alphabet.get(feat), features.get(feat));
				}
			}
			results.put(frame, featsForFrame);
		}
		return results;
	}

}
