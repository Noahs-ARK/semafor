/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 *
 * FastFrameIdentifier.java is part of SEMAFOR 2.0.
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
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Token;
import edu.cmu.cs.lti.ark.fn.identification.training.LRIdentificationModelSingleNode;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectDoubleHashMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.nullToEmpty;
import static edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags.readLine;

/**
 * This class is for finding the best frame for a span of tokens, given
 * the annotation for a sentence. It is just a single node implementation.
 * Moreover, it looks only at a small set of frames for seen words
 *
 * @author dipanjan
 */
public class FastFrameIdentifier {
	private final LRIdentificationModelSingleNode model;
	// map from lemmas to frames
	private THashMap<String, THashSet<String>> mHvCorrespondenceMap;
	private final IdFeatureExtractor featureExtractor;

	public FastFrameIdentifier(IdFeatureExtractor featureExtractor,
							   TObjectDoubleHashMap<String> paramList,
							   String reg,
							   double l,
							   THashMap<String, THashSet<String>> frameMap,
							   THashMap<String, THashSet<String>> hvCorrespondenceMap) {
		model = new LRIdentificationModelSingleNode(paramList, reg, l, null, frameMap);
		mHvCorrespondenceMap = hvCorrespondenceMap;
		this.featureExtractor = featureExtractor;
	}

	/**
	 * Applies the log-linear model to each frame in frames and selects the highest scoring frame
	 *
	 * @param frames       the frames to consider
	 * @param sentence     the dependency parse of the sentence, needed to extract features
	 * @param tokenIndices the token indexes that the frame spans
	 * @return the highest scoring frame
	 */
	private String pickBestFrame(Set<String> frames, Sentence sentence, int[] tokenIndices) {
		final Map<String, Map<String, Double>> featuresByFrame =
				featureExtractor.extractFeaturesByName(frames,
						tokenIndices,
						sentence
				);

		String result = null;
		double maxVal = -Double.MIN_VALUE;
		for (String frame : frames) {
			double val = getValueForFrame(featuresByFrame.get(frame));
			if (val > maxVal) {
				maxVal = val;
				result = frame;
			}
		}
		return result;
	}

	/**
	 * Applies the log-linear model to frame
	 * @return the score of the frame
	 */
	private double getValueForFrame(Map<String, Double> features) {
		model.m_current = 0;
		model.m_llcurrent = 0;
		double result = 0.0;
		double featSum = 0.0;
		for (String feat : features.keySet()) {
			double val = features.get(feat);
			int ind = model.localA.get(feat);
			double paramVal = model.V[ind].getValue();
			double prod = val * paramVal;
			featSum += prod;
		}
		result += Math.exp(featSum);
		return result;
	}

	private Set<String> checkPresenceOfTokensInMap(int[] intTokNums, Sentence sentence) {
		final List<Token> tokens = sentence.getTokens();
		final List<String> lemmatizedTokens = Lists.newArrayList();
		for (int tokNum : intTokNums) {
			lemmatizedTokens.add(tokens.get(tokNum).getLemma());
		}
		final String lemmas = Joiner.on(" ").join(lemmatizedTokens);
		Set<String> frames = mHvCorrespondenceMap.get(lemmas);
		if (frames == null) System.err.println("Not found in hvCorrespondenceMap:\t" + lemmas);
		return frames;
	}

	private Set<String> getCandidateFrames(int[] tokenIndices, Sentence sentence, SmoothedGraph graph) {
		final List<Token> sentenceTokens = sentence.getTokens();
		final Set<String> frames = checkPresenceOfTokensInMap(tokenIndices, sentence);
		if (frames != null) return frames;

		final List<Token> frameTokens = Lists.newArrayList();
		final List<String> lowerCaseForms = Lists.newArrayList();
		for (int tokNum : tokenIndices) {
			final Token token = sentenceTokens.get(tokNum);
			frameTokens.add(token);
			lowerCaseForms.add(token.getForm().toLowerCase());
		}
		final Map<String, Set<String>> coarseMap = graph.getCoarseMap();
		if (frameTokens.size() > 1) {
			final String coarseToken = getCanonicalForm(Joiner.on(" ").join(lowerCaseForms));
			if (coarseMap.containsKey(coarseToken)) return coarseMap.get(coarseToken);
		} else {
			final Token token = frameTokens.get(0);
			final String lemma = token.getLemma();
			final String pos = convertPostag(token.getPostag());
			if (pos != null) {
				final String fineToken = getCanonicalForm(lemma + "." + pos);
				if (graph.getFineMap().containsKey(fineToken)) return graph.getFineMap().get(fineToken);
			}
			final String coarseToken = getCanonicalForm(lemma);
			if (coarseMap.containsKey(coarseToken)) return coarseMap.get(coarseToken);
		}
		return model.mFrameMap.keySet();
	}

	private String convertPostag(String pos) {
		pos = nullToEmpty(pos).toUpperCase();
		if (pos.startsWith("N")) {
			pos = "n";
		} else if (pos.startsWith("V")) {
			pos = "v";
		} else if (pos.startsWith("J")) {
			pos = "a";
		} else if (pos.startsWith("RB")) {
			pos = "adv";
		} else if (pos.startsWith("I") || pos.startsWith("TO")) {
			pos = "prep";
		} else {
			pos = null;
		}
		return pos;
	}

	/* SmoothedGraph versions */
	public String getBestFrame(String frameLine, String parseLine, SmoothedGraph graph) {
		return getBestFrame(parseFrameLine(frameLine), Sentence.fromAllLemmaTagsArray(readLine(parseLine)), graph);
	}

	public String getBestFrame(int[] tokenIndices, Sentence sentence, SmoothedGraph graph) {
		final Set<String> candidateFrames = getCandidateFrames(tokenIndices, sentence, graph);
		return pickBestFrame(candidateFrames, sentence, tokenIndices);
	}

	/* non-graph versions */
	public String getBestFrame(String frameLine, String parseLine) {
		return getBestFrame(parseFrameLine(frameLine), Sentence.fromAllLemmaTagsArray(readLine(parseLine)));
	}

	private String getBestFrame(int[] tokenIndices, Sentence sentence) {
		Set<String> frames = checkPresenceOfTokensInMap(tokenIndices, sentence);
		if (frames == null) frames = model.mFrameMap.keySet(); // lemmas aren't in the map. fall back to all frames
		return pickBestFrame(frames, sentence, tokenIndices);
	}

	private int[] parseFrameLine(String frameLine) {
		String[] toks = frameLine.split("\t");
		String[] tokNums = toks[1].split("_");
		int[] intTokNums = new int[tokNums.length];
		for (int j = 0; j < tokNums.length; j++) {
			intTokNums[j] = Integer.parseInt(tokNums[j]);
		}
		Arrays.sort(intTokNums);
		return intTokNums;
	}

	private static String getCanonicalForm(String word) {
		int len = word.length();
		String ans = "";
		for (int i = 0; i < len; i ++) {
			char c = word.charAt(i);
			if (Character.isDigit(c)) {
				ans += "@";
			} else {
				ans += c;
			}
		}
		return ans.toLowerCase();
	}
}
