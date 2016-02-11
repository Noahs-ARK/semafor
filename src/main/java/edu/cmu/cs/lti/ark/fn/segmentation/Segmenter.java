package edu.cmu.cs.lti.ark.fn.segmentation;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;

public abstract class Segmenter {

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

}
