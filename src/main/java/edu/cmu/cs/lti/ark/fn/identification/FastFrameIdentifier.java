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
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Token;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectDoubleHashMap;

import java.util.*;

import static edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags.readLine;

/**
 * This class is for finding the best frame for a span of tokens, given
 * the annotation for a sentence. It is just a single node implementation.
 * Moreover, it looks only at a small set of frames for seen words
 *
 * @author dipanjan
 */
public class FastFrameIdentifier {
	public final TObjectDoubleHashMap<String> params;
	protected final Set<String> allFrames;
	// map from lemmas to frames
	private THashMap<String, THashSet<String>> framesByLemma;
	public final IdFeatureExtractor featureExtractor;

	public FastFrameIdentifier(IdFeatureExtractor featureExtractor,
							   TObjectDoubleHashMap<String> params,
							   Set<String> allFrames,
							   THashMap<String, THashSet<String>> framesByLemma) {
		this.params = params;
		this.allFrames = allFrames;
		this.framesByLemma = framesByLemma;
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
	protected String pickBestFrame(Set<String> frames, Sentence sentence, int[] tokenIndices) {
		final Map<String, Map<String, Double>> featuresByFrame =
				featureExtractor.extractFeaturesByName(frames, tokenIndices, sentence);
		String result = null;
		double maxVal = Double.NEGATIVE_INFINITY;
		for (String frame : frames) {
			double val = getValueForFrame(featuresByFrame.get(frame));
			if (val >= maxVal) {
				maxVal = val;
				result = frame;
			}
		}
		return result;
	}

	/**
	 * Applies the log-linear model to frame
	 * @return the log score of the frame
	 */
	protected double getValueForFrame(Map<String, Double> features) {
		Preconditions.checkArgument(features.size() > 0);
		double featSum = 0.0;
		for (String feat : features.keySet()) {
			featSum += features.get(feat) * params.get(feat);
		}
		return featSum;
	}

	protected Optional<THashSet<String>> checkPresenceOfTokensInMap(int[] intTokNums, Sentence sentence) {
		final List<Token> tokens = sentence.getTokens();
		final List<String> lemmatizedTokens = Lists.newArrayList();
		for (int tokNum : intTokNums) {
			lemmatizedTokens.add(tokens.get(tokNum).getLemma());
		}
		final String lemmas = Joiner.on(" ").join(lemmatizedTokens);
		return Optional.fromNullable(framesByLemma.get(lemmas));
	}

	public String getBestFrame(String frameLine, String parseLine) {
		return getBestFrame(getTargetTokenIdxs(frameLine), Sentence.fromAllLemmaTagsArray(readLine(parseLine)));
	}

	public String getBestFrame(Collection<Integer> indices, Sentence sentence) {
		return getBestFrame(Ints.toArray(indices), sentence);
	}

	public String getBestFrame(int[] tokenIndices, Sentence sentence) {
		final Optional<THashSet<String>> oFrames = checkPresenceOfTokensInMap(tokenIndices, sentence);
		// fall back to all frames if lemmas aren't in the map.
		final Set<String> frames = oFrames.isPresent() ? oFrames.get() : allFrames;
		return pickBestFrame(frames, sentence, tokenIndices);
	}

	private int[] getTargetTokenIdxs(String frameLine) {
		String[] toks = frameLine.split("\t");
		String[] tokNums = toks[1].split("_");
		int[] intTokNums = new int[tokNums.length];
		for (int j = 0; j < tokNums.length; j++) {
			intTokNums[j] = Integer.parseInt(tokNums[j]);
		}
		Arrays.sort(intTokNums);
		return intTokNums;
	}
}
