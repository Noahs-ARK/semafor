package edu.cmu.cs.lti.ark.fn.identification;

import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.identification.latentmodel.LatentFeatureExtractor;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectDoubleHashMap;

import java.util.Set;

/**
 * @author sthomson@cs.cmu.edu
 */
public class LatentGraphBasedFrameIdentifier extends GraphBasedFrameIdentifier {
	private final LatentFeatureExtractor featureExtractor;
	private final THashMap<String, THashSet<String>> hiddenUnitsByFrame;

	public LatentGraphBasedFrameIdentifier(LatentFeatureExtractor featureExtractor,
										   THashMap<String, THashSet<String>> hiddenUnitsByFrame,
										   THashMap<String, THashSet<String>> framesByLemma,
										   TObjectDoubleHashMap<String> params,
										   SmoothedGraph graph) {
		super(null, hiddenUnitsByFrame.keySet(), framesByLemma, params, graph);
		this.featureExtractor = featureExtractor;
		this.hiddenUnitsByFrame = hiddenUnitsByFrame;
	}

	@Override
	public String getBestFrame(int[] tokenIndices, Sentence sentence) {
		final Set<String> candidateFrames = getCandidateFrames(tokenIndices, sentence);
		return pickBestFrame(candidateFrames, sentence, tokenIndices);
	}

	/**
	 * Applies the log-linear model to each frame in frames and selects the highest scoring frame
	 *
	 * @param frames       the frames to consider
	 * @param sentence     the dependency parse of the sentence, needed to extract features
	 * @param targetTokenIdxs the token indexes that the frame spans
	 * @return the highest scoring frame
	 */
	@Override
	protected String pickBestFrame(Set<String> frames, Sentence sentence, int[] targetTokenIdxs) {
		String result = null;
		double maxVal = Double.NEGATIVE_INFINITY;
		for (String frame : frames) {
			double val = getValueForFrame(frame, targetTokenIdxs, sentence);
			if (val >= maxVal) {
				maxVal = val;
				result = frame;
			}
		}
		return result;
	}

	/**
	 * Applies the log-linear model to frame
	 * @param frame the frames to score
	 * @param sentence the dependency parse of the sentence, needed to extract features
	 * @param targetTokenIdxs the token indexes that the frame spans
	 * @return the score of the frame
	 */
	protected double getValueForFrame(String frame, int[] targetTokenIdxs, Sentence sentence) {
		if (!allFrames.contains(frame)) return Double.NEGATIVE_INFINITY;
		final THashSet<String> hiddenUnits = hiddenUnitsByFrame.get(frame);
		final String[][] allLemmaTags = sentence.toAllLemmaTagsArray();
		final DependencyParse parse = DependencyParse.processFN(allLemmaTags, 0.0);
		double result = 0.0;
		for (String hiddenLexUnit : hiddenUnits) {
			final IntCounter<String> features =
					featureExtractor.extractFeatures(
							frame,
							targetTokenIdxs,
							hiddenLexUnit,
							allLemmaTags,
							parse,
							true);
			double featSum = 0.0;
			for (String feat : features.keySet()) {
				featSum += features.get(feat) * params.get(feat);
			}
			result += Math.exp(featSum);
		}
		return result;
	}
}
