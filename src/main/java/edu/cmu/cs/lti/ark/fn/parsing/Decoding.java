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
import com.google.common.collect.Lists;
import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import edu.cmu.cs.lti.ark.fn.optimization.LDouble;
import edu.cmu.cs.lti.ark.fn.optimization.LogMath;
import edu.cmu.cs.lti.ark.util.FileUtil;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.util.*;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.transform;
import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;
import static java.lang.Math.min;

/**
 * Predict spans for roles using beam search.
 */
public class Decoding {
	private static final int BEAM_WIDTH = 100;
	private static final LDouble LOG_ZERO = LDouble.convertToLogDomain(0);
	private static final Joiner TAB_JOINER = Joiner.on("\t");

	protected int numLocalFeatures;
	protected double[] localW;
	private String mLocalAlphabetFile;
	private String mLocalModelFile;
	public static long count = 0;
	protected List<FrameFeatures> mFrameList;
	protected String mPredictionFile;	
	protected List<String> mFrameLines;

	private static final Comparator<Pair<String,LDouble>> logDoubleComparator = new Comparator<Pair<String,LDouble>>() {
		public int compare(Pair<String, LDouble> o1, Pair<String, LDouble> o2) {
			final LDouble one = o2.getSecond();
			final LDouble two = o1.getSecond();
			final Double oneValue = one.getValue();
			final Double twoValue = two.getValue();
			if(one.isPositive()) {
				return two.isPositive() ? oneValue.compareTo(twoValue) : 1;
			} else {
				return two.isPositive() ? -1 : twoValue.compareTo(oneValue);
			}
		}
	};

	/** An assignment of spans to roles of a particular frame */
	public static class RoleAssignments extends THashMap<String,String> {
		private final Function<Map.Entry<String,String>,String> JOIN_ENTRY = new Function<Map.Entry<String, String>, String>() {
			@Override public String apply(Map.Entry<String, String> input) {
				return input.getKey() + "\t" + input.getValue();
			}
		};

		public String format()  {
			return TAB_JOINER.join(transform(entrySet(), JOIN_ENTRY));
		}
	}

	/** A map from spans to their log score for a particular role */
	public static class CandidatesForRole extends THashMap<String, LDouble> { }

	/**
	 * 0-indexed. Both ends inclusive. Null span is represented as [-1,-1].
	 */
	public static class Span extends Pair<Integer, Integer> {
		public Span(Integer first, Integer second) {
			super(first, second);
		}

		/**
		 * Determines whether the given spans overlap
		 *
		 * @param other the other span
		 * @return whether this and the other span overlap
		 */
		public boolean overlaps(Span other) {
			int start = getFirst();
			int end = getSecond();
			int otherStart = other.getFirst();
			int otherEnd = other.getSecond();
			// null spans can't overlap with anything
			if(start == -1 || otherStart == -1)
				return false;
			if(start < otherStart)
				return end >= otherStart;
			else
				return otherEnd >= start;
		}
	}

	public static class FrameArgumentsPrediction {
		final public int count;
		final public String initialDecisionLine;
		final public RoleAssignments assignments;

		public FrameArgumentsPrediction(int count, String initialDecisionLine, RoleAssignments assignments) {
			this.count = count;
			this.initialDecisionLine = initialDecisionLine;
			this.assignments = assignments;
		}

		public String format() {
			return 0 + "\t" + count + "\t" + initialDecisionLine + "\t" + assignments.format();
		}
	}

	public void init(String modelFile, 
					 String alphabetFile,
					 String predictionFile,
					 List<FrameFeatures> list,
					 List<String> frameLines) {
		mLocalModelFile = modelFile;
		mLocalAlphabetFile = alphabetFile;
		readModel();
		mFrameList = list;
		mPredictionFile = predictionFile;
		mFrameLines = frameLines;
	}

	public void init(String modelFile, String alphabetFile) {
		mLocalModelFile = modelFile;
		mLocalAlphabetFile = alphabetFile;
		readModel();
	}
	
	public void setData(String predictionFile, List<FrameFeatures> list, List<String> frameLines) {
		mFrameList = list;
		mPredictionFile = predictionFile;
		mFrameLines = frameLines;
	}
	
	public void readModel() {	
		Scanner localsc = FileUtil.openInFile(mLocalAlphabetFile);
		Scanner paramsc = FileUtil.openInFile(mLocalModelFile);
		numLocalFeatures = localsc.nextInt() + 1;
		localsc.close();
		localW = new double[numLocalFeatures];
		for (int i = 0; i < numLocalFeatures; i++) {
			double val = Double.parseDouble(paramsc.nextLine());
			localW[i] = val;
		}
	}
	
	public ArrayList<String> decodeAll(boolean doOverlapCheck, int offset) {
		int size = mFrameList.size();
		ArrayList<String> result = new ArrayList<String>();
		for(int i = 0; i < size; i++) {
			System.out.println("Decoding index:" + i);
			String decisionLine = decode(i, doOverlapCheck, offset);
			result.add(decisionLine);
		}
		if (mPredictionFile != null) {
			ParsePreparation.writeSentencesToFile(mPredictionFile, result);
		}
		return result;
	}
	
	public String decode(int index, boolean doOverlapCheck, int offset) {
		final FrameFeatures frameFeatures = mFrameList.get(index);
		final String frameLine = mFrameLines.get(index);
		if(doOverlapCheck)
			return getNonOverlappingDecision(frameFeatures, frameLine, offset);
		else
			return getUnconstrainedDecision(frameFeatures, frameLine, offset);
	}

	/**
	 * Calculates the sum of the weights of firing features.
	 *
	 * @param feats indexes of firing features
	 * @param w an array of weights into which feats indexes
	 * @return the sum of the weights of firing features
	 */
	public double getWeightSum(int[] feats, double[] w) {
		double weightSum = w[0];
		for (int feat : feats) {
			if (feat != 0) {
				weightSum += w[feat];
			}
		}
		return weightSum;
	}

	/**
	 * Choose spans for each role independently.
	 *
	 * @param frameFeatures features for each span
	 * @param frameLine
	 * @param offset
	 * @return
	 */
	public String getUnconstrainedDecision(FrameFeatures frameFeatures, String frameLine, int offset) {
		String frameName = frameFeatures.frameName;
		System.out.println("Frame:" + frameName);
		String decisionLine = getInitialDecisionLine(frameLine, offset);
		if(frameFeatures.fElements.size() == 0)
			return "0\t1\t" + decisionLine.trim();
		int count = 1;
		final ArrayList<SpanAndCorrespondingFeatures[]> featsList = frameFeatures.fElementSpansAndFeatures;
		final ArrayList<String> frameElements = frameFeatures.fElements;
		int listSize = featsList.size();
		for(int i = 0; i < listSize; i++) {
			SpanAndCorrespondingFeatures[] featureArray = featsList.get(i);
			String fe = frameElements.get(i);
			int featArrLen = featureArray.length;
			int maxIndex = -1;
			double maxSum = -Double.MAX_VALUE;
			for(int j = 0; j < featArrLen; j ++) {
				int[] feats = featureArray[j].features;
				double weightSum=getWeightSum(feats, localW);
				double expVal = Math.exp(weightSum);
				if(expVal>maxSum) {
					maxSum = expVal;
					maxIndex = j;
				}
			}
			String maxSpan = featureArray[maxIndex].span[0] + "_" + featureArray[maxIndex].span[1];
			System.out.println("Frame element:" + fe + " Found span:" + maxSpan);
			// leave null spans unspecified
			if(maxSpan.equals("-1_-1"))
				continue;
			count++;
			String modTokens = formatSpan(maxSpan);

			decisionLine += fe + "\t" + modTokens + "\t";
		}	
		decisionLine = "0\t" + count + "\t" + decisionLine.trim();
		return decisionLine;
	}

	/**
	 * Adds 'offset' to the 6th field and discards the 1st field.
	 *
	 * @param frameLine
	 * @param offset
	 * @return frameline with offset added to its 5th field and the 1st field discarded
	 */
	public String getInitialDecisionLine(String frameLine, int offset) {
		String[] frameTokens = frameLine.split("\t");
		String decisionLine = "";
		for(int i = 1; i <= 5; i++) {
			String tok = frameTokens[i];
			if (i == 5) {
				int num = Integer.parseInt(tok);
				num = num + offset;
				tok = "" + num;
			}
			decisionLine += tok + "\t";
		}	
		return decisionLine;
	}

	/**
	 * Determines whether the given spans overlap
	 *
	 * @param one the first span
	 * @param two the second span
	 * @return whether the given spans overlap
	 */
	private static boolean pairwiseOverlap(String one, String two) {
		return parseRange(one).overlaps(parseRange(two));
	}

	private static Span parseRange(String rangeStr) {
		final String[] tokens = rangeStr.split("_");
		int start = Integer.parseInt(tokens[0]);
		int end = Integer.parseInt(tokens[tokens.length-1]);
		return new Span(start, end);
	}

	/**
	 * Filters out null spans from the given map.
	 *
	 * @param map the map to filter
	 * @return a new map with only the entries which have non-null spans
	 */
	public static RoleAssignments getOnlyNonNullFrameElements(RoleAssignments map) {
		final RoleAssignments tempMap = new RoleAssignments();
		for(String fe: map.keySet()) {
			final String span = map.get(fe);
			if(!span.equals("-1_-1")) {
				tempMap.put(fe, span);
			}
		}	
		return tempMap;
	}	
	
	private static boolean isThereOverlap(String spansStr, THashSet<String> seenSpans) {
		return isThereOverlap(spansStr.split(":"), seenSpans);
	}

	private static boolean isThereOverlap(String[] spans, THashSet<String> seenSpans) {
		for(int i : xrange(spans.length)) {
			for(int j : xrange(i + 1, spans.length)) {
				if (pairwiseOverlap(spans[i], spans[j])
						|| seenSpans.contains(spans[i])
						|| seenSpans.contains(spans[j])) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Find the (approximately) best configuration of non-overlapping role-filling spans using beam search.
	 *
	 * @param overlappingSpanMap
	 * @param candidatesAndScoresByRole map from role name to (map from span to score)
	 * @param beamWidth the beam width
	 * @param seenSpans a set of spans we've already seen
	 * @return the best configuration of role-filling spans. a map from role name to span.
	 */
	public static RoleAssignments
			getCubePruningDecoding(RoleAssignments overlappingSpanMap,
								   THashMap<String, CandidatesForRole> candidatesAndScoresByRole,
								   int beamWidth,
								   THashSet<String> seenSpans) {
		final ArrayList<RoleAssignments> kBest =
				getCubePruningDecodingKBest(overlappingSpanMap, candidatesAndScoresByRole, 1, beamWidth, seenSpans);
		return kBest.get(0);
	}

	/**
	 * Find the k (approximately) best configuration of non-overlapping role-filling spans using beam search.
	 *
	 * @param overlappingSpanMap
	 * @param candidatesAndScoresByRole map from role name to (map from span to score)
	 * @param beamWidth the beam width
	 * @param seenSpans a set of spans we've already seen
	 * @return the k best configuration of role-filling spans. a map from role name to span
	 */
	public static ArrayList<RoleAssignments>
			getCubePruningDecodingKBest(RoleAssignments overlappingSpanMap,
										THashMap<String, CandidatesForRole> candidatesAndScoresByRole,
										int k,
										int beamWidth,
										THashSet<String> seenSpans) {
		final int numOverlappingSpans = overlappingSpanMap.size();
		if(numOverlappingSpans == 0) {
			return Lists.newArrayList(new RoleAssignments());
		}

		// build up an array of overlapping spans
		final ArrayList<String> roles = Lists.newArrayList();
		for(String role : candidatesAndScoresByRole.keySet()) {
			if (overlappingSpanMap.contains(role)) {
				roles.add(role);
			}
		}

		// our beam
		List<Pair<String,LDouble>> currentSpans = new ArrayList<Pair<String,LDouble>>();
		// do beam search
		for(int i = 0; i < roles.size(); i++) {
			final Map<String,LDouble> candidatesAndScores = candidatesAndScoresByRole.get(roles.get(i));
			final PriorityQueue<Pair<String,LDouble>> scoredSpans =
					new PriorityQueue<Pair<String, LDouble>>(candidatesAndScores.size(), logDoubleComparator);
			for(String candidate : candidatesAndScores.keySet()) {
				scoredSpans.add(new Pair<String,LDouble>(candidate, candidatesAndScores.get(candidate)));
			}
			if(i == 0) {
				currentSpans = safeTruncate(copyOf(scoredSpans), beamWidth);
			}
			else {
				final PriorityQueue<Pair<String,LDouble>> newSpans =
						new PriorityQueue<Pair<String, LDouble>>(currentSpans.size() * scoredSpans.size(), logDoubleComparator);
				for (Pair<String, LDouble> span : currentSpans) {
					for (Pair<String, LDouble> candidate : scoredSpans) {
						final String newSpan = span.getFirst() + ":" + candidate.getFirst();
						final LDouble val = isThereOverlap(newSpan, seenSpans)
												? LOG_ZERO
												: LogMath.logtimes(span.getSecond(), candidate.getSecond());
						newSpans.add(new Pair<String, LDouble>(newSpan, val));
					}
				}
				currentSpans = safeTruncate(copyOf(newSpans), beamWidth);
			}
		}

		final ArrayList<RoleAssignments> results = Lists.newArrayList();
		for(int i = 0; i < k && i < currentSpans.size(); i++) {
			results.add(getRoleAssignments(roles, currentSpans.get(i)));
		}
		return results;
	}

	private static <T> List<T> safeTruncate(List<T> list, int beamWidth) {
		return list.subList(0, min(list.size(), beamWidth));
	}

	private static RoleAssignments getRoleAssignments(List<String> fes, Pair<String, LDouble> assignments) {
		final String[] tokens = assignments.getFirst().split(":");
		final RoleAssignments result = new RoleAssignments();
		for(int i = 0; i < fes.size(); i++) {
			result.put(fes.get(i), tokens[i]);
		}
		return result;
	}

	/**
	 * Decode, respecting the constraint that arguments do not overlap.
	 *
	 * @param frameFeatures features for the given frame
	 * @param frameLine String encoding of the frame
	 * @param offset
	 * @return a String encoding the best spans for all roles of the given frame
	 */
	public String getNonOverlappingDecision(FrameFeatures frameFeatures, String frameLine, int offset) {
		final String frameName = frameFeatures.frameName;
		final ArrayList<String> frameElements = frameFeatures.fElements;
		final ArrayList<SpanAndCorrespondingFeatures[]> featuresList = frameFeatures.fElementSpansAndFeatures;
		System.out.println("Frame:" + frameName);
		String decisionLine = getInitialDecisionLine(frameLine, offset);
		if(frameElements.size() == 0) {
			return new FrameArgumentsPrediction(1, decisionLine, new RoleAssignments()).format();
		}

		final RoleAssignments feMap = scoreAllSpans(frameElements, featuresList);
		final RoleAssignments nonNullFrameElements = getOnlyNonNullFrameElements(feMap);
		for (String role : nonNullFrameElements.keySet()) {
			System.out.println(role + "\t" + nonNullFrameElements.get(role));
		}
		final THashSet<String> seenSpans = getSeenSpans(feMap, nonNullFrameElements);

		final THashMap<String, CandidatesForRole> vs =
				getCandidatesForRoles(featuresList, frameElements, nonNullFrameElements);

		final RoleAssignments nonOverlappingMap =
				getCubePruningDecoding(nonNullFrameElements, vs, BEAM_WIDTH, seenSpans);
		feMap.putAll(nonOverlappingMap);

		int count = 1;
		for(String role: feMap.keySet()) {
			String outcome = feMap.get(role);
			if(outcome.equals("-1_-1"))
				continue;
			count++;
			String modTokens = formatSpan(outcome);
			decisionLine += role + "\t" + modTokens + "\t";
		}		
		decisionLine = "0\t" + count + "\t" + decisionLine.trim();
		System.out.println(decisionLine);
		return decisionLine;
	}

	private RoleAssignments scoreAllSpans(List<String> frameElements,
										  List<SpanAndCorrespondingFeatures[]> featuresList) {
		final RoleAssignments feMap = new RoleAssignments();
		for(int i = 0; i < featuresList.size(); i++) {
			final SpanAndCorrespondingFeatures[] featureArray = featuresList.get(i);
			final int featArrLen = featureArray.length;
			final double weightedFeatureSum[] = new double[featArrLen];
			int maxIndex = -1;
			double maxSum = -Double.MAX_VALUE;
			for(int j = 0; j < featArrLen; j++) {
				final int[] feats = featureArray[j].features;
				weightedFeatureSum[j] = getWeightSum(feats, localW);
				if(weightedFeatureSum[j] > maxSum) {
					maxSum = weightedFeatureSum[j];
					maxIndex = j;
				}
			}
			final String outcome = featureArray[maxIndex].span[0] + "_" + featureArray[maxIndex].span[1];
			feMap.put(frameElements.get(i), outcome);
			System.out.println("Frame element:" + frameElements.get(i) + " Found span:" + outcome);
		}
		return feMap;
	}

	private THashMap<String, CandidatesForRole>
			getCandidatesForRoles(ArrayList<SpanAndCorrespondingFeatures[]> featuresList,
								  ArrayList<String> roles,
								  RoleAssignments nonNullFrameElements) {
		int featuresListSize = featuresList.size();
		final THashMap<String,CandidatesForRole> results = new THashMap<String,CandidatesForRole>();
		for(int i = 0; i < featuresListSize; i++) {
			final String role = roles.get(i);
			if(!nonNullFrameElements.contains(role))
				continue;
			final CandidatesForRole candidatesForRole = new CandidatesForRole();
			for (SpanAndCorrespondingFeatures spanAndFeatures : featuresList.get(i)) {
				final String span = spanAndFeatures.span[0] + "_" + spanAndFeatures.span[1];
				final int[] feats = spanAndFeatures.features;
				final LDouble logScore = LDouble.convertToLogDomain(Math.exp(getWeightSum(feats, localW)));
				candidatesForRole.put(span, logScore);
			}
			results.put(role, candidatesForRole);
		}
		return results;
	}

	/**
	 * Take span in "start_end" form and convert to "start:end" form (or "start" if start==end)
	 *
	 * @param span the span to format
	 * @return "start:end" form (or "start" if start==end)
	 */
	private String formatSpan(String span) {
		String[] spanTokens = span.split("_");
		return spanTokens[0].equals(spanTokens[1])
				? spanTokens[0]
				: spanTokens[0] + ":" + spanTokens[1];
	}

	private THashSet<String> getSeenSpans(RoleAssignments feMap, RoleAssignments oMap) {
		final THashSet<String> seenSpans = new THashSet<String>();
		final Set<String> roles = feMap.keySet();
		for(String role : roles) {
			final String span = feMap.get(role);
			if (!span.equals("-1_-1")) {
				if (!oMap.contains(role)) {
					seenSpans.add(span);
				}
			}
		}
		if(seenSpans.size() > 0)
			System.out.println("seenSpans.size() > 0");
		return seenSpans;
	}

	public void wrapUp() { /* no op unless overridden */ }
}
