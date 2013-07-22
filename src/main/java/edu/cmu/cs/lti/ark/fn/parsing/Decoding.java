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
import com.google.common.collect.*;
import edu.cmu.cs.lti.ark.util.FileUtil;
import edu.cmu.cs.lti.ark.util.ds.Scored;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import java.util.*;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.transform;
import static edu.cmu.cs.lti.ark.util.ds.Scored.scored;
import static java.lang.Integer.parseInt;
import static java.lang.Math.min;

/**
 * Predict spans for roles using beam search.
 */
public class Decoding {
	private static final int DEFAULT_BEAM_WIDTH = 100;
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
			return ComparisonChain.start()
					.compare(start, other.start)
					.compare(end, other.end).result();
		}

		@Override
		public String toString() {
			return (start == end) ? (""+start) : (start + ":" + end);
		}
	}

	/** An assignment of spans to roles of a particular frame */
	public static class RoleAssignments implements Comparable<RoleAssignments> {
		private final static Function<Map.Entry<String,Span>,String> JOIN_ENTRY = new Function<Map.Entry<String, Span>, String>() {
			@Override public String apply(Map.Entry<String, Span> input) {
				return TAB_JOINER.join(input.getKey(), input.getValue().toString());
			}
		};
		private final PMap<String, Span> nonNullAssignments;
		private final PMap<String, Span> nullAssignments;

		public RoleAssignments(PMap<String, Span> nonNullAssignments, PMap<String, Span> nullAssignments) {
			this.nonNullAssignments = nonNullAssignments;
			this.nullAssignments = nullAssignments;
		}

		public RoleAssignments() {
			this(HashTreePMap.<String, Span>empty(), HashTreePMap.<String, Span>empty());
		}

		public RoleAssignments plus(String key, Span value) {
			if (value.isEmpty()) {
				return new RoleAssignments(nonNullAssignments, nullAssignments.plus(key, value));
			} else {
				return new RoleAssignments(nonNullAssignments.plus(key, value), nullAssignments);
			}
		}

		private Map<String, Span> getNonNullAssignments() {
			return nonNullAssignments;
		}

		/** Determines whether the given span overlaps with any of our spans */
		private boolean overlaps(Span otherSpan) {
			if(otherSpan.isEmpty()) return false;
			for (Span span : getNonNullAssignments().values()) {
				if (span.overlaps(otherSpan)) return true;
			}
			return false;
		}

		@Override
		public String toString()  {
			return TAB_JOINER.join(transform(getNonNullAssignments().entrySet(), JOIN_ENTRY));
		}

		@Override
		public int compareTo(RoleAssignments other) {
			return Ordering.arbitrary().compare(this, other);
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

	private String formatPrediction(int rank, String initialDecisionLine, RoleAssignments assignments, double score) {
		return TAB_JOINER.join(
				rank,
				score,
				assignments.getNonNullAssignments().size() + 1, // includes the target
				initialDecisionLine,
				assignments.toString());
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
		// our beam
		List<Scored<RoleAssignments>> currentBeam = Lists.newArrayList(scored(new RoleAssignments(), 0.0));
		// run beam search
		for (String roleName : candidatesAndScoresByRole.keySet()) {
			final PriorityQueue<Scored<RoleAssignments>> newBeam = Queues.newPriorityQueue();
			for (Scored<Span> candidate : candidatesAndScoresByRole.get(roleName)) {
				for (Scored<RoleAssignments> partialAssignment : currentBeam) {
					final double newScore = partialAssignment.score + candidate.score; // multiply in log-space
					if (newBeam.size() >= DEFAULT_BEAM_WIDTH && newScore <= newBeam.peek().score) break;
					if (!partialAssignment.value.overlaps(candidate.value)) {
						final RoleAssignments newAssignment = partialAssignment.value.plus(roleName, candidate.value);
						newBeam.add(scored(newAssignment, newScore));
					}
					if (newBeam.size() > DEFAULT_BEAM_WIDTH) newBeam.poll();
				}
			}
			currentBeam = copyOf(newBeam);
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
				final double logScore = getWeightSum(spanAndFeatures.features, modelWeights);
				candidatesForRole.add(scored(span, logScore));
			}
			results.put(roleName, candidatesForRole);
		}
		return results;
	}

	public void wrapUp() { /* no op unless overridden */ }
}
