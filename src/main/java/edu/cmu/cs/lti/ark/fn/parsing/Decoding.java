/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * Decoding.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.parsing;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import edu.cmu.cs.lti.ark.fn.optimization.LDouble;
import edu.cmu.cs.lti.ark.util.FileUtil;
import edu.cmu.cs.lti.ark.util.ds.Scored;

import java.util.*;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.transform;
import static edu.cmu.cs.lti.ark.fn.optimization.LogMath.logtimes;
import static edu.cmu.cs.lti.ark.util.ds.Scored.scored;
import static java.lang.Integer.parseInt;
import static java.lang.Math.min;

/**
 * Predict spans for roles using beam search.
 */
public class Decoding {
	private static final int DEFAULT_BEAM_WIDTH = 100;
	private static final LDouble LOG_ZERO = LDouble.convertToLogDomain(0);
	private static final LDouble LOG_ONE = new LDouble(0.0);
	private static final Joiner TAB_JOINER = Joiner.on("\t");

	protected double[] modelWeights;

	/** 0-indexed. Both ends inclusive. Null span is represented as [-1,-1]. */
	public static class Span implements Comparable<Span> {
		public final int start;
		public final int end;

		public Span(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public boolean isEmpty() { return start == -1; }

		/** Determines whether this and the other span overlap */
		public boolean overlaps(Span other) {
			// empty spans can't overlap with anything
			if(isEmpty() || other.isEmpty()) return false;
			if(start < other.start) {
				return end >= other.start;
			} else {
				return other.end >= start;
			}
		}

		@Override
		public int compareTo(Span other) {
			int cmp = Ints.compare(start, other.start);
			return cmp != 0 ? cmp : Ints.compare(end, other.end);
		}

		@Override
		public String toString() {
			return (start == end) ? (""+start) : (start + ":" + end);
		}
	}

	/** An assignment of spans to roles of a particular frame */
	public static class RoleAssignments extends HashMap<String,Span> {
		private final Function<Map.Entry<String,Span>,String> JOIN_ENTRY = new Function<Map.Entry<String, Span>, String>() {
			@Override public String apply(Map.Entry<String, Span> input) {
				return input.getKey() + "\t" + input.getValue().toString();
			}
		};
		private final Predicate<Map.Entry<String,Span>> IS_NOT_NULL = new Predicate<Map.Entry<String, Span>>() {
			@Override public boolean apply(Map.Entry<String, Span> input) {
				return !input.getValue().isEmpty();
			}
		};

		public Map<String, Span> filterNulls() {
			// TODO: could keep null spans separate from non-null for speedup
			return Maps.filterEntries(this, IS_NOT_NULL);
		}

		/** Determines whether the given span overlaps with any of our spans */
		private boolean overlaps(Span otherSpan) {
			if(otherSpan.isEmpty()) return false;
			for (Span span : filterNulls().values()) {
				if (span.overlaps(otherSpan)) return true;
			}
			return false;
		}

		public int count() {
			// includes the target
			return filterNulls().size() + 1;
		}

		@Override
		public String toString()  {
			return TAB_JOINER.join(transform(filterNulls().entrySet(), JOIN_ENTRY));
		}
	}

	/** A sorted list of spans and their log score for a particular role */
	public static class CandidatesForRole extends TreeSet<Scored<Span>> { }

	public Decoding(double[] modelWeights) {
		this.modelWeights = modelWeights;
	}

	public static Decoding fromFile(String modelFile, String alphabetFile) {
		return new Decoding(readModel(modelFile, alphabetFile));
	}

	protected static double[] readModel(String modelFile, String alphabetFile) {
		final Scanner localsc = FileUtil.openInFile(alphabetFile);
		final int numLocalFeatures;
		try {
			numLocalFeatures = localsc.nextInt() + 1;
		} finally {
			localsc.close();
		}
		final Scanner scanner = FileUtil.openInFile(modelFile);
		final double [] modelWeights = new double[numLocalFeatures];
		try {
			for (int i = 0; i < numLocalFeatures; i++) {
				modelWeights[i] = Double.parseDouble(scanner.nextLine());
			}
		} finally {
			scanner.close();
		}
		return modelWeights;
	}

	public List<String> decodeAll(List<FrameFeatures> frameFeaturesList, List<String> frameLines, int offset, int kBestOutput) {
		final ArrayList<String> results = new ArrayList<String>();
		for(int i = 0; i < frameFeaturesList.size(); i++) {
			final FrameFeatures frameFeatures = frameFeaturesList.get(i);
			final String initialDecisionLine = getInitialDecisionLine(frameLines.get(i), offset);
			final List<Scored<RoleAssignments>> predictions = getPredictions(frameFeatures, kBestOutput);
			final List<String> predictionLines = Lists.newArrayList();
			for(int j = 0; j < predictions.size(); j++) {
				final Scored<RoleAssignments> prediction = predictions.get(j);
				predictionLines.add(formatPrediction(j, initialDecisionLine, prediction.value, prediction.score));
			}
			results.add(Joiner.on("\n").join(predictionLines));
		}
		return results;
	}

	private String formatPrediction(int rank, String initialDecisionLine, RoleAssignments assignments, LDouble score) {
		final String result = rank + "\t" +
				score.getValue() + "\t" +
				assignments.count() + "\t" +
				initialDecisionLine + "\t" +
				assignments.toString();
		return result.trim();
	}

	/**
	 * Calculates the sum of the weights of firing features.
	 *
	 * @param feats indexes of firing features
	 * @param weights an array of weights into which feats indexes
	 * @return the sum of the weights of firing features
	 */
	public static double getWeightSum(int[] feats, double[] weights) {
		// the 0th coordinate is the bias; it always fires
		double weightSum = weights[0];
		for (int feat : feats) {
			if (feat != 0) weightSum += weights[feat];
		}
		return weightSum;
	}

	/** Adds 'offset' to the sentence field and discards the 1st two fields. */
	protected String getInitialDecisionLine(String frameLine, int offset) {
		String[] frameTokens = frameLine.split("\t");
		frameTokens[7] = "" + (parseInt(frameTokens[7]) + offset);
		return TAB_JOINER.join(copyOf(frameTokens).subList(3, frameTokens.length)).trim();
	}

	private static <T> List<T> safeTruncate(List<T> list, int beamWidth) {
		return list.subList(0, min(list.size(), beamWidth));
	}

	/**
	 * Decode, respecting the constraint that arguments do not overlap.
	 * Find the k (approximately) best configurations of non-overlapping role-filling spans using beam search.
	 *
	 * @param frameFeatures features for the given frame
	 * @param kBestOutput the number of top configurations we should return
	 * @return a list of Strings encoding the best k configurations of spans for all roles of the given frame
	 */
	public List<Scored<RoleAssignments>> getPredictions(FrameFeatures frameFeatures, int kBestOutput) {
		// group by role
		final Map<String, CandidatesForRole> candidatesAndScoresByRole =
				scoreCandidatesForRoles(frameFeatures.fElements, frameFeatures.fElementSpansAndFeatures);

		// run beam search to find the (approximately) k-best non-overlapping configurations
		final List<String> roleNames = copyOf(candidatesAndScoresByRole.keySet());
		// our beam
		List<Scored<RoleAssignments>> currentBeam = Lists.newArrayList(scored(new RoleAssignments(), LOG_ONE));
		// run beam search
		for (String roleName : roleNames) {
			final TreeSet<Scored<RoleAssignments>> newBeam = Sets.newTreeSet();
			for (Scored<Span> candidate : candidatesAndScoresByRole.get(roleName)) {
				for (Scored<RoleAssignments> partialAssignment : currentBeam) {
					// TODO: stop as soon as we know we can't improve the beam
					final LDouble newScore = partialAssignment.value.overlaps(candidate.value)
							? LOG_ZERO
							: logtimes(partialAssignment.score, candidate.score);
					// TODO: a PersistentHashMap would be real nice here
					final RoleAssignments newAssignment = new RoleAssignments();
					newAssignment.putAll(partialAssignment.value);
					newAssignment.put(roleName, candidate.value);
					newBeam.add(scored(newAssignment, newScore));
				}
			}
			// TODO: configurable beam width
			currentBeam = safeTruncate(copyOf(newBeam), DEFAULT_BEAM_WIDTH);
			//System.out.println("Considering " + roleName);
			//System.out.println("Beam grew to " + newBeam.size());
			//System.out.println("Current best: " + newBeam.first().value + " " + newBeam.first().score);
		}
		return safeTruncate(currentBeam, kBestOutput);
	}

	private Map<String, CandidatesForRole>
			scoreCandidatesForRoles(List<String> roleNames, List<SpanAndCorrespondingFeatures[]> featuresList) {
		final Map<String,CandidatesForRole> results = Maps.newHashMap();
		for(int i = 0; i < featuresList.size(); i++) {
			final String roleName = roleNames.get(i);
			final CandidatesForRole candidatesForRole = new CandidatesForRole();
			for (SpanAndCorrespondingFeatures spanAndFeatures : featuresList.get(i)) {
				final Span span = new Span(spanAndFeatures.span[0], spanAndFeatures.span[1]);
				final LDouble logScore = new LDouble(getWeightSum(spanAndFeatures.features, modelWeights));
				candidatesForRole.add(scored(span, logScore));
			}
			results.put(roleName, candidatesForRole);
		}
		return results;
	}

	public void wrapUp() { /* no op unless overridden */ }
}
