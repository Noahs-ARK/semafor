package edu.cmu.cs.lti.ark.fn.segmentation;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;
import static edu.cmu.cs.lti.ark.fn.evaluation.ParseUtils.GOLD_TARGET_SUFFIX;
import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;

public abstract class Segmenter {
	public abstract List<List<Integer>> getSegmentation(Sentence sentence);

	/**
	 * @param parseLines a list of AllLemmaTags-formatted sentences
	 * @return a list of predicted targets, one line per sentence
	 */
	public List<String> getSegmentations(List<String> parseLines) {
		final ImmutableList.Builder<String> result = ImmutableList.builder();
		for(int sentenceIdx: xrange(parseLines.size())) {
			final String parse = parseLines.get(sentenceIdx);
			final Sentence sentence = Sentence.fromAllLemmaTagsArray(AllLemmaTags.readLine(parse));
			final List<List<Integer>> ngramIndices = getSegmentation(sentence);
			result.add(getTestLine(ngramIndices) + "\t" + sentenceIdx);
		}
		return result.build();
	}

	/**
	 * Removes prepositions from the given sentence
	 *
	 * @param candidateTokens a row of token indices
	 * @param pData an array of parse data. each column is a word. relevant rows are:  0: token, 1: pos, 5: lemma
	 */
	public static List<List<Integer>> trimPrepositions(List<List<Integer>> candidateTokens, final String[][] pData) {
		final DependencyParse mParse = DependencyParse.processFN(pData, 0.0);
		final DependencyParse[] mNodeList = mParse.getIndexSortedListOfNodes();
		final Iterable<List<Integer>> goodTokens = Iterables.filter(candidateTokens, new Predicate<List<Integer>>() {
			@Override
			public boolean apply(@Nullable List<Integer> input) {
				return RoteSegmenter.shouldIncludeToken(input, pData, mNodeList);
			}
		});
		return copyOf(goodTokens);
	}

	public static String getTestLine(List<List<Integer>> tokenIdxs) {
		final ImmutableList.Builder<String> result = ImmutableList.builder();
		for (List<Integer> idxs : tokenIdxs) {
			result.add(Joiner.on("_").join(idxs) + GOLD_TARGET_SUFFIX);
		}
		return Joiner.on("\t").join(result.build());
	}
}
