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
package edu.cmu.cs.lti.ark.fn.identification;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Token;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static com.google.common.base.Strings.nullToEmpty;

/**
 * Extracts features for the frame identification model
 */
public class BasicFeatureExtractor extends IdFeatureExtractor {
	protected static final Joiner SPACE = Joiner.on(" ");
	private static final Joiner UNDERSCORE = Joiner.on("_");

	public Map<String, Map<String, Double>> extractFeaturesByName(Iterable<String> frameNames,
																  int[] targetTokenIdxs,
																  Sentence sentence) {
		final Map<String, Double> baseFeatures = getBaseFeatures(targetTokenIdxs, sentence);
		final Map<String, Map<String, Double>> results = Maps.newHashMap();
		// conjoin base features with frame
		for (String frame : frameNames) {
			results.put(frame, conjoin("f:" + frame, baseFeatures));
		}
		return results;
	}

	protected <V extends Number> Map<String, V> conjoin(String name, Map<String, V> oldFeatures) {
		final Map<String, V> conjoinedFeatures = Maps.newHashMap();
		for (String feature : oldFeatures.keySet()) {
			conjoinedFeatures.put(SPACE.join(name, feature), oldFeatures.get(feature));
		}
		return conjoinedFeatures;
	}

	public Map<String, Double> getBaseFeatures(int[] targetTokenIdxs, Sentence sentence) {
		Arrays.sort(targetTokenIdxs);
		// Get lemmas and postags for target
		final IntCounter<String> featureMap = getTargetWordFeatures(targetTokenIdxs, sentence);
		// add homogenous/bias feature
		featureMap.increment("bias");

		/*
		 * syntactic features
		 */
		final DependencyParse parse = DependencyParse.processFN(sentence.toAllLemmaTagsArray(), 0.0);
		final IntCounter<String> syntacticFeatures = getSyntacticFeatures(targetTokenIdxs, parse);
		return featureMap.addAll(syntacticFeatures).scaleBy(1.0);
	}

	protected IntCounter<String> getSentenceContextFeatures(int[] targetTokenIdxs, Sentence sentence) {
		// add a feature for each word in the sentence
		final IntCounter<String> featureMap = new IntCounter<String>();
		for (Token token : sentence.getTokens()) {
			final String form = token.getForm();
			final String postag = nullToEmpty(token.getPostag()).toUpperCase();
			final String cpostag = getCpostag(postag);
			final String lemma = token.getLemma();
			featureMap.increment("sTP:" + form + "_" + cpostag);
			featureMap.increment("sLP:" + lemma + "_" + cpostag);
		}
		return featureMap;
	}

	protected IntCounter<String> getTargetWordFeatures(int[] targetTokenIdxs, Sentence sentence) {
		final List<String> tokenAndCpostags = Lists.newArrayListWithExpectedSize(targetTokenIdxs.length);
		final List<String> cpostags = Lists.newArrayListWithExpectedSize(targetTokenIdxs.length);
		final List<String> lemmaAndCpostags = Lists.newArrayListWithExpectedSize(targetTokenIdxs.length);
		final List<Token> tokens = sentence.getTokens();
		for (int tokenIdx : targetTokenIdxs) {
			Token token = tokens.get(tokenIdx);
			final String cpostag = getCpostag(token.getPostag());
			tokenAndCpostags.add(token.getForm() + "_" + cpostag);
			cpostags.add(cpostag);
			lemmaAndCpostags.add(token.getLemma() + "_" + cpostag);
		}
		final IntCounter<String> featureMap = new IntCounter<String>();
		featureMap.increment("aP:" + UNDERSCORE.join(cpostags));
		featureMap.increment("aTP:" + UNDERSCORE.join(tokenAndCpostags));
		featureMap.increment("aLP:" + UNDERSCORE.join(lemmaAndCpostags));
		return featureMap;
	}

	protected IntCounter<String> getSyntacticFeatures(int[] targetTokenIdxs, DependencyParse parse) {
		final DependencyParse head = DependencyParse.getHeuristicHead(parse.getIndexSortedListOfNodes(), targetTokenIdxs);
		final String headCpostag = getCpostag(head.getPOS());

		final List<DependencyParse> children = head.getChildren();

		final SortedSet<String> depLabels = Sets.newTreeSet(); // unordered set of arc labels of children
		for (DependencyParse child : children) {
			depLabels.add(child.getLabelType().toUpperCase());
		}
		final IntCounter<String> featureMap = new IntCounter<String>();
		featureMap.increment("d:" + UNDERSCORE.join(depLabels));

		if (headCpostag.equals("V")) {
			final List<String> subcat = Lists.newArrayListWithExpectedSize(children.size()); // ordered arc labels of children
			for (DependencyParse child : children) {
				final String labelType = child.getLabelType().toUpperCase();
				if (!labelType.equals("SUB") && !labelType.equals("P") && !labelType.equals("CC")) {
					// TODO(smt): why exclude "sub"?
					subcat.add(labelType);
				}
			}
			featureMap.increment("sC:" + UNDERSCORE.join(subcat));
		}
		final IntCounter<String> parentFeatures = getParentFeatures(head.getParent());
		return featureMap.addAll(parentFeatures);
	}

	protected IntCounter<String> getParentFeatures(DependencyParse parent) {
		IntCounter<String> featureMap = new IntCounter<String>();
		if (parent == null) {
			featureMap.increment("pP:NULL");
			featureMap.increment("pPL:NULL");
			featureMap.increment("pLab:NULL");
		} else {
			final String parentPostag = parent.getPOS().toUpperCase();
			// if parent is a preposition, collapse the dependency, Stanford-style
			if (parentPostag.startsWith("I") && parent.getParent() != null) {
				final DependencyParse gp = parent.getParent();
				final String gpPostag = gp.getPOS().toUpperCase();
				featureMap.increment("pP:" + gpPostag);
				featureMap.increment("pLP:" + gp.getLemma() + "_" + gpPostag);
				featureMap.increment("pLab:" + gp.getLabelType().toUpperCase() + "_" + parent.getLemma());
			} else {
				featureMap.increment("pP:" + parentPostag);
				featureMap.increment("pLP:" + parent.getLemma() + "_" + parentPostag);
				featureMap.increment("pLab:" + parent.getLabelType().toUpperCase());
			}
		}
		return featureMap;
	}

	public static String getCpostag(String postag) {
		return postag.substring(0, 1).toUpperCase();
	}
}
