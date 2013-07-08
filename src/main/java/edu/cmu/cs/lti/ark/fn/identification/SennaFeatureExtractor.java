package edu.cmu.cs.lti.ark.fn.identification;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Token;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;
import static edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse.getHeuristicHead;

/**
 * @author sthomson@cs.cmu.edu
 */
public class SennaFeatureExtractor {
	public static final String[] FIVE_WORD_WINDOW_NAMES = {"[-2]", "[-1]", "[0]", "[1]", "[2]"};

	private final Senna senna;

	public SennaFeatureExtractor(Senna senna) {
		this.senna = senna;
	}

	public static SennaFeatureExtractor load() throws IOException {
		return new SennaFeatureExtractor(Senna.load());
	}

	public Map<String, Double> getSennaFeatures(int[] targetTokenIdxs, Sentence sentence) {
		final Map<String, Double> features = Maps.newHashMap();

		final DependencyParse parse = DependencyParse.processFN(sentence.toAllLemmaTagsArray(), 0.0);
		final int headIdx = getHeuristicHead(parse.getIndexSortedListOfNodes(), targetTokenIdxs).getIndex() - 1;
		final List<Token> tokens = sentence.getTokens();
		// add senna features for five-word window around target head
		for (int i : xrange(FIVE_WORD_WINDOW_NAMES.length)) {
			final int idx = headIdx - 2 + i;
			if (idx >= 0 && idx < sentence.size()) {
				final Map<String, Double> sennaFeaturesForWord = getSennaFeaturesForWord(tokens.get(idx).getForm());
				features.putAll(FrameFeatureExtractor.conjoin(FIVE_WORD_WINDOW_NAMES[i], sennaFeaturesForWord));
			}
		}
		return features;
	}

	public Map<String, Double> getSennaFeaturesForWord(String word) {
		final Map<String, Double> features = Maps.newHashMap();
		final Optional<double[]> oEmbedding = senna.getEmbedding(word);
		if (oEmbedding.isPresent()) {
			final double[] embedding = oEmbedding.get();
			for (int i : xrange(embedding.length))  {
				features.put(String.format("senna%02d", i), embedding[i]);
			}
		}
		return features;
	}
}
