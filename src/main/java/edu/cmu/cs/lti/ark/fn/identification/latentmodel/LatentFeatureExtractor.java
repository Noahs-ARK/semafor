/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 *
 * FeatureExtractor.java is part of SEMAFOR 2.0.
 *
 * SEMAFOR 2.0 is free software: you can redistribute it and/or modify  it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 *
 * SEMAFOR 2.0 is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. 
 *
 * You should have received a copy of the GNU General Public License along
 * with SEMAFOR 2.0.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package edu.cmu.cs.lti.ark.fn.identification.latentmodel;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.cmu.cs.lti.ark.fn.wordnet.Relations;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.nlp.Lemmatizer;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags.*;
import static edu.cmu.cs.lti.ark.fn.identification.IdFeatureExtractor.getCpostag;
import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;

/**
 * Extracts features for the frame identification model
 */
public class LatentFeatureExtractor {
	private static final Joiner SPACE = Joiner.on(" ");
	private static final Joiner UNDERSCORE = Joiner.on("_");
	private final Relations wnRelations;
	private final Lemmatizer lemmatizer;

	public LatentFeatureExtractor(Relations wnRelations, Lemmatizer lemmatizer) {
		this.wnRelations = wnRelations;
		this.lemmatizer = lemmatizer;
	}

	/**
	 * Extract features for a (frame, target, hidden l.u.) tuple
	 *
	 * @param frameName the name of the candidate frame
	 * @param targetTokenIdxs the token indexes (0-indexed) of the target
	 * @param hiddenLexUnit the latent l.u.
	 * @param allLemmaTags the sentence in AllLemmaTags format
	 * @param parse the dependency parse for the sentence
	 * @param parseHasLemmas whether or not allLemma already includes lemmas for each token
	 * @return a map from feature name -> count
	 */
	public IntCounter<String> extractFeatures(String frameName,
											  int[] targetTokenIdxs,
											  String hiddenLexUnit,
											  String[][] allLemmaTags,
											  DependencyParse parse,
											  boolean parseHasLemmas) {
		// Get lemmas and postags for prototype
		// hiddenLexUnit is in format: "form1_pos1 form2_pos2 ... formn_posn"
		final String[] hiddenTokenAndPos = hiddenLexUnit.split(" ");
		final List<String> hiddenTokenAndCpostags = Lists.newArrayListWithExpectedSize(hiddenTokenAndPos.length);
		final List<String> hiddenTokens = Lists.newArrayListWithExpectedSize(hiddenTokenAndPos.length);
		final List<String> hiddenCpostags = Lists.newArrayListWithExpectedSize(hiddenTokenAndPos.length);
		final List<String> hiddenLemmaAndCpostags = Lists.newArrayListWithExpectedSize(hiddenTokenAndPos.length);
		for (String hiddenTok : hiddenTokenAndPos) {
			final String[] arr = hiddenTok.split("_");
			final String form = arr[0];
			final String postag = arr[1].toUpperCase();
			final String cpostag = getCpostag(postag);
			final String lemma = lemmatizer.getLemma(form, postag);
			hiddenCpostags.add(cpostag);
			hiddenTokens.add(form);
			hiddenTokenAndCpostags.add(form + "_" + cpostag);
			hiddenLemmaAndCpostags.add(lemma + "_" + cpostag);
		}
		final String hiddenTokenAndCpostagsStr = UNDERSCORE.join(hiddenTokenAndCpostags);
		final String hiddenCpostagsStr = UNDERSCORE.join(hiddenCpostags);
		final String hiddenLemmaAndCpostagsStr = UNDERSCORE.join(hiddenLemmaAndCpostags);

		// Get lemmas and postags for target
		final List<String> actualTokenAndCpostags = Lists.newArrayListWithExpectedSize(targetTokenIdxs.length);
		final List<String> actualTokens = Lists.newArrayListWithExpectedSize(targetTokenIdxs.length);
		final List<String> actualCpostags = Lists.newArrayListWithExpectedSize(targetTokenIdxs.length);
		final List<String> actualLemmaAndCpostags = Lists.newArrayListWithExpectedSize(targetTokenIdxs.length);
		Arrays.sort(targetTokenIdxs);
		for (int tokenIdx : targetTokenIdxs) {
			final String form = allLemmaTags[PARSE_TOKEN_ROW][tokenIdx];
			final String postag = allLemmaTags[PARSE_POS_ROW][tokenIdx].toUpperCase();
			final String cpostag = getCpostag(postag);
			final String lemma = parseHasLemmas ? allLemmaTags[PARSE_LEMMA_ROW][tokenIdx]
											: lemmatizer.getLemma(form, postag);
			actualTokens.add(form);
			actualTokenAndCpostags.add(form + "_" + cpostag);
			actualCpostags.add(cpostag);
			actualLemmaAndCpostags.add(lemma + "_" + cpostag);
		}
		final String actualTokenAndCpostagsStr = UNDERSCORE.join(actualTokenAndCpostags);
		final String actualCpostagsStr = UNDERSCORE.join(actualCpostags);
		final String actualLemmaAndCpostagsStr = UNDERSCORE.join(actualLemmaAndCpostags);

		final Set<String> relations = wnRelations.getRelations(SPACE.join(actualTokens), SPACE.join(hiddenTokens));

		final IntCounter<String> featureMap = new IntCounter<String>();
		/*
		 * base features
		 * will be conjoined in various ways
		 * (always conjoined with the frame name)
		 */
		final String frameFtr = "f:" + frameName;
		final String actualCpostagsFtr = "aP:" + actualCpostagsStr;
		final String actualLemmaAndCpostagsFtr = "aLP:" + actualLemmaAndCpostagsStr;
		final String hiddenTokenAndCpostagsFtr = "hT:" + hiddenTokenAndCpostagsStr;
		final String hiddenCpostagsFtr = "hP:" + hiddenCpostagsStr;
		final String hiddenLemmaAndCpostagsFtr = "hLP:" + hiddenLemmaAndCpostagsStr;

		// add a feature for each word in the sentence
		for (int tokenIdx : xrange(allLemmaTags[0].length)) {
			final String form = allLemmaTags[PARSE_TOKEN_ROW][tokenIdx];
			final String postag = allLemmaTags[PARSE_POS_ROW][tokenIdx].toUpperCase();
			final String cpostag = getCpostag(postag);
			final String lemma = parseHasLemmas ? allLemmaTags[PARSE_LEMMA_ROW][tokenIdx]
					: lemmatizer.getLemma(form, postag);
			featureMap.increment(UNDERSCORE.join(
					"sTP:" + form + "_" + cpostag,
					frameFtr));
			featureMap.increment(UNDERSCORE.join(
					"sLP:" + lemma + "_" + cpostag,
					frameFtr));
		}

		featureMap.increment(UNDERSCORE.join(
				hiddenTokenAndCpostagsFtr,
				frameFtr));
		featureMap.increment(UNDERSCORE.join(
				hiddenLemmaAndCpostagsFtr,
				frameFtr));

		// extract features for each WordNet relation by which the target and prototype are connected
		for (String relation : relations) {
			if (relation.equals(WordNetRelations.NO_RELATION)) continue;
			final String relationFeature = "tRLn:" + relation;
			featureMap.increment(UNDERSCORE.join(
					relationFeature,
					frameFtr));
			featureMap.increment(UNDERSCORE.join(
					relationFeature,
					hiddenTokenAndCpostagsStr,
					frameFtr));
			featureMap.increment(UNDERSCORE.join(
					relationFeature,
					hiddenTokenAndCpostagsStr,
					hiddenCpostagsFtr,
					actualCpostagsFtr,
					frameFtr));
		}

		if (hiddenTokenAndCpostagsStr.equals(actualTokenAndCpostagsStr)) {
			final String tokenMatchFtr = "sTs";
			featureMap.increment(UNDERSCORE.join(
					tokenMatchFtr,
					frameFtr));
			featureMap.increment(UNDERSCORE.join(
					tokenMatchFtr,
					hiddenTokenAndCpostagsFtr,
					frameFtr));
			featureMap.increment(UNDERSCORE.join(
					tokenMatchFtr,
					actualCpostagsFtr,
					hiddenCpostagsFtr,
					frameFtr));
			featureMap.increment(UNDERSCORE.join(
					tokenMatchFtr,
					actualLemmaAndCpostagsFtr,
					hiddenLemmaAndCpostagsFtr,
					frameFtr));
		}
		if (hiddenLemmaAndCpostagsStr.equals(actualLemmaAndCpostagsStr)) {
			final String lemmaMatchFtr = "sLs";
			featureMap.increment(UNDERSCORE.join(
					lemmaMatchFtr,
					frameFtr));
			featureMap.increment(UNDERSCORE.join(
					lemmaMatchFtr,
					hiddenTokenAndCpostagsFtr,
					frameFtr));
			featureMap.increment(UNDERSCORE.join(
					lemmaMatchFtr,
					actualCpostagsFtr,
					hiddenCpostagsFtr,
					frameFtr));
			featureMap.increment(UNDERSCORE.join(
					lemmaMatchFtr,
					actualLemmaAndCpostagsFtr,
					hiddenLemmaAndCpostagsFtr,
					frameFtr));
		}

		/*
		 * syntactic features
		 */
		final DependencyParse[] sortedNodes = parse.getIndexSortedListOfNodes();
		final DependencyParse head = DependencyParse.getHeuristicHead(sortedNodes, targetTokenIdxs);
		final String headCpostag = getCpostag(head.getPOS());

		final List<DependencyParse> children = head.getChildren();

		final SortedSet<String> depLabels = Sets.newTreeSet(); // unordered set of arc labels of children
		for (DependencyParse child : children) {
			depLabels.add(child.getLabelType().toUpperCase());
		}
		final String dependencyFtr = "d:" + UNDERSCORE.join(depLabels);
		featureMap.increment(UNDERSCORE.join(
				dependencyFtr,
				frameFtr));

		if (headCpostag.equals("V")) {
			final List<String> subcat = Lists.newArrayListWithExpectedSize(children.size()); // ordered arc labels of children
			for (DependencyParse child : children) {
				final String labelType = child.getLabelType().toUpperCase();
				if (!labelType.equals("SUB") && !labelType.equals("P") && !labelType.equals("CC")) {
					// TODO(smt): why exclude "sub"?
					subcat.add(labelType);
				}
			}
			final String subcatFtr = "sC:" + UNDERSCORE.join(subcat);
			featureMap.increment(UNDERSCORE.join(
					subcatFtr,
					frameFtr));
		}

		final DependencyParse parent = head.getParent();
		final String parentPosFtr = "pP:" + ((parent == null) ? "NULL" : parent.getPOS().toUpperCase());
		featureMap.increment(UNDERSCORE.join(
				parentPosFtr,
				frameFtr));
		final String parentLabelFtr = "pL:" + ((parent == null) ? "NULL" : parent.getLabelType().toUpperCase());
		featureMap.increment(UNDERSCORE.join(
				parentLabelFtr,
				frameFtr));

		return featureMap;
	}
}
