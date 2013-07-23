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
package edu.cmu.cs.lti.ark.fn.parsing;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;
import edu.cmu.cs.lti.ark.util.ds.Range1Based;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;

import java.util.List;

import static edu.cmu.cs.lti.ark.fn.parsing.CandidateFrameElementFilters.isEmptySpan;
import static edu.cmu.cs.lti.ark.fn.parsing.FeatureExtractor.ConjoinLevel.*;
import static java.lang.Math.max;

/**
 * Extract features for the parsing model. Based on FeatureExtractor for the
 * frame identification model.
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2009-04-07
 * @see CandidateFrameElementFilters
 * @see edu.cmu.cs.lti.ark.fn.identification.IdFeatureExtractor
 */
public class FeatureExtractor {
	private static final Joiner UNDERSCORE = Joiner.on("_");

	protected enum ConjoinLevel {
		FRAME_AND_ROLE_NAME,
		ROLE_NAME,
		NO_CONJOIN
	}

	protected enum Voice {
		PASSIVE("PAS"),
		ACTIVE("ACT"),
		NO_VOICE("");

		public final String name;

		Voice(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/**
	 * @param featureName feature to add
	 * @param level indicates whether to conjoin with role name and/or frame name.
	 */
	protected static void conjoinAndAdd(String featureName,
										String frameAndRoleName,
										String roleName,
										ConjoinLevel level,
										Multiset<String> featureMap) {
		switch(level) {
			case FRAME_AND_ROLE_NAME:
				featureMap.add(UNDERSCORE.join(featureName, frameAndRoleName));
				//intentional fall through
			case ROLE_NAME:
				featureMap.add(UNDERSCORE.join(featureName, roleName));
			case NO_CONJOIN:
				featureMap.add(featureName);
			default:
				break;
		}
	}

	public Multiset<String> extractFeatures(DataPointWithFrameElements dp,
											String frameName,
											String roleName,
											final Range0Based fillerSpanRange,
											DependencyParse parse) {
		final Multiset<String> featureMap = HashMultiset.create();
		final String frameAndRoleName = frameName + "." + roleName;
		int[] targetTokenNums = dp.getTargetTokenIdxs();
		final DependencyParse[] nodes = parse.getIndexSortedListOfNodes();
		final DependencyParse targetHeadNode = DependencyParse.getHeuristicHead(nodes, targetTokenNums);

		final boolean isEmpty = isEmptySpan(fillerSpanRange);
		String overtness = isEmpty ? "NULL" : "OVERT";
		conjoinAndAdd(overtness, frameAndRoleName, roleName, FRAME_AND_ROLE_NAME, featureMap);	// overtness of the role
		
		String nullness = isEmpty ? "NULL_" : "";
		for (int targetTokenNum : targetTokenNums) {
			final DependencyParse node = nodes[targetTokenNum + 1];
			final Voice voice = findVoice(node);
			final String lemma = node.getLemma();
			final String feature = nullness + "targetLemma_" + lemma;
			conjoinAndAdd(feature, frameAndRoleName, roleName, FRAME_AND_ROLE_NAME, featureMap);
			conjoinAndAdd(UNDERSCORE.join(feature, voice.name), frameAndRoleName, roleName, ROLE_NAME, featureMap);
			conjoinAndAdd(nullness + "targetPOS_" + node.getPOS(), frameAndRoleName, roleName, FRAME_AND_ROLE_NAME, featureMap);
		}

		final List<DependencyParse> tgtChildren = targetHeadNode.getChildren();
		conjoinAndAdd(nullness + "NCHILDREN_" + tgtChildren.size(), frameAndRoleName, roleName, FRAME_AND_ROLE_NAME, featureMap);	// number of children

		// Dependency subcategorization
		String dsubcat = "";
		if (tgtChildren.size() > 0) {
			for (DependencyParse dpn : tgtChildren) {
				dsubcat += dpn.getLabelType() + "_";
				conjoinAndAdd(nullness + "SUBCAT_" + dpn.getLabelType(), frameAndRoleName, roleName, ROLE_NAME, featureMap);
			}
			conjoinAndAdd(nullness + "SUBCATSEQ" + dsubcat, frameAndRoleName, roleName, ROLE_NAME, featureMap);
		}

		if (!isEmpty) { // null span
			 // lemma, POS tag, voice, and relative position (with respect to target)
			 // of each word in the candidate span
			extractChildPOSFeatures(featureMap, dp, nodes, fillerSpanRange, frameAndRoleName, roleName);
			final DependencyParse fillerHeadNode = DependencyParse.getHeuristicHead(
					nodes, fillerSpanRange);
			final List<Pair<String, DependencyParse>> targetToFillerPath =
					DependencyParse.getPath(targetHeadNode, fillerHeadNode);
			int pathSize = targetToFillerPath.size();
			String depTypePath = ""; // target's POS and dependency types on the
			// path to the head of the filler
			// To avoid feature explosion, only include paths of length <=7, and
			// only include the dependency types if the path's length is <=5.  
			// (To put this in perspective, < FRAME_AND_ROLE_NAME% of FE fillers have a path length
			// of >5 in the training/dev data, and hardly any have length >7.)
			// e.g. "=<VB> !VMOD !PMOD" ( GIVE to [the paper *boy*] )
			// "=<PRP> ^OBJ ^VMOD ^VMOD !OBJ" ( want [*him*] to make a REQUEST )
			final int spanStart = fillerSpanRange.start;
			final int spanEnd = fillerSpanRange.end;
			final int targetStart = targetTokenNums[0];
			final int targetEnd = targetTokenNums[targetTokenNums.length-1];
			if (isOverlap(spanStart, spanEnd, targetStart, targetEnd)) {
				
				//a few features describing
				//relative position of the span with respect to the target
				
				//does the span overlap with target
				conjoinAndAdd("O_W_T", frameAndRoleName, roleName, FRAME_AND_ROLE_NAME, featureMap);
				if (targetTokenNums.length > 1) {
					if ((spanStart < targetStart && spanEnd < targetEnd)
							|| (spanStart > targetStart && spanEnd > targetEnd)) {
						//does the span cross the target
						conjoinAndAdd("CROS_TAR", frameAndRoleName, roleName, NO_CONJOIN, featureMap);
					}
				}
			} else {
				// distance between nearest words of span and target
				String dist = getDistToTarget(targetStart, targetEnd, spanStart, spanEnd);
				String feature = "dist_" + dist;
				conjoinAndAdd(feature, frameAndRoleName, roleName, NO_CONJOIN, featureMap);
				if (dist.charAt(0) == '-') {
					//span is left to target
					feature = "LEFTTAR";
				} else {
					//span is right to target
					feature = "RIGHTTAR";
				}
				conjoinAndAdd(feature, frameAndRoleName, roleName, FRAME_AND_ROLE_NAME, featureMap);
				int targetMidpoint = (targetStart + targetEnd) / 2;
				int feMidpoint = (spanStart + spanEnd) / 2;
				//distance between words in the middle
				//of target span and candidate span
				feature = "midDist_" + getDistToTarget(targetMidpoint, targetMidpoint, feMidpoint, feMidpoint);
				conjoinAndAdd(feature, frameAndRoleName, roleName, NO_CONJOIN, featureMap);
			}

			if (pathSize <= 7) {
				DependencyParse lastNode = null;
				for (int i = 0; i < targetToFillerPath.size(); i++) {
					final Pair<String, DependencyParse> item = targetToFillerPath.get(i);
					final DependencyParse node = item.second;
					if (i == 0)
						depTypePath += "=<" + node.getPOS() + ">";
					else {
						if (item.first.equals("^"))
							depTypePath += " ^"
									+ ((pathSize <= 5) ? lastNode.getLabelType()
											: "");
						else {
							if (item.first.equals("!"))
								depTypePath += " !"
										+ ((pathSize <= 5) ? node
										.getLabelType() : "");
						}
					}
					lastNode = node;
				}
			} else {
				depTypePath = "=<"
						+ targetToFillerPath.get(0).second.getPOS()
						+ "> ...";
			}
			conjoinAndAdd("depPath_" + depTypePath, frameAndRoleName, roleName, NO_CONJOIN, featureMap);
			conjoinAndAdd("pathLength_" + quantizeLength(pathSize), frameAndRoleName, roleName, NO_CONJOIN, featureMap);

			// head word
			// left and right most dependents
			List<DependencyParse> children = fillerHeadNode.getChildren();
			conjoinAndAdd("headLemma_" + fillerHeadNode.getLemma(), frameAndRoleName, roleName, NO_CONJOIN, featureMap);
			conjoinAndAdd("headPOS_" + fillerHeadNode.getPOS(), frameAndRoleName, roleName, NO_CONJOIN, featureMap);
			conjoinAndAdd("headLabel_" + fillerHeadNode.getLabelType(), frameAndRoleName, roleName, NO_CONJOIN, featureMap);
			if (children.size() > 0) {
				final DependencyParse firstChild = children.get(0);
				final DependencyParse lastChild = children.get(children.size() - 1);
				conjoinAndAdd("leftLemma_" + firstChild.getLemma(), frameAndRoleName, roleName, NO_CONJOIN, featureMap);
				conjoinAndAdd("leftPOS_" + firstChild.getPOS(), frameAndRoleName, roleName, NO_CONJOIN, featureMap);
				conjoinAndAdd("rightLemma_" + lastChild.getLemma(), frameAndRoleName, roleName, NO_CONJOIN, featureMap);
				conjoinAndAdd("rightPOS_" + lastChild.getPOS(), frameAndRoleName, roleName, NO_CONJOIN, featureMap);
			}
			
			// word/POS/dependency type of 1st,  FrameAndRoleNamend, last word in the span
			int startNode = new Range1Based(fillerSpanRange).start;
			int endNode = new Range1Based(fillerSpanRange).end;

			if (isClosedClass(nodes[startNode].getPOS()))
				conjoinAndAdd("w[0]pos[0]_" + nodes[startNode].getWord()
						+ " " + nodes[startNode].getPOS(), frameAndRoleName, roleName, FRAME_AND_ROLE_NAME, featureMap);
			conjoinAndAdd("dep[0]_" + nodes[startNode].getLabelType(), frameAndRoleName, roleName, FRAME_AND_ROLE_NAME, featureMap);

			if (endNode - startNode > 0) {
				if (isClosedClass(nodes[startNode + 1].getPOS()))
					conjoinAndAdd("w[1]pos[1]_"
							+ nodes[startNode + 1].getWord() + " "
							+ nodes[startNode + 1].getPOS(), frameAndRoleName, roleName, FRAME_AND_ROLE_NAME, featureMap
					);
				conjoinAndAdd("dep[1]_" + nodes[startNode + 1].getLabelType(), frameAndRoleName, roleName, FRAME_AND_ROLE_NAME, featureMap);
				if (endNode - startNode > 1) {
					if (isClosedClass(nodes[endNode].getPOS()))
						conjoinAndAdd("w[-1]pos[-1]_"
								+ nodes[endNode].getWord() + " "
								+ nodes[endNode].getPOS() + "_", frameAndRoleName, roleName, FRAME_AND_ROLE_NAME, featureMap
						);
					conjoinAndAdd("dep[-1]_"
							+ nodes[endNode].getLabelType() + "_", frameAndRoleName, roleName, FRAME_AND_ROLE_NAME, featureMap
					);
				}
			}
			
			// length of the filler span
			conjoinAndAdd("len_" + quantizeLength(endNode - startNode + 1), frameAndRoleName, roleName, FRAME_AND_ROLE_NAME, featureMap);
		}
		return featureMap;
	}

	public static String quantizeLength(int numWords) {
		if (numWords < 0) {
			if (numWords > -5)
				return "" + numWords;
			if (numWords > -10)
				return "-5-9";
			if (numWords > -20)
				return "-10-19";
			return "-20-";
		}

		if (numWords < 5)
			return "" + numWords;
		else if (numWords < 10)
			return "5-9";
		else if (numWords < 20)
			return "10-19";
		else
			return "20+";
	}

	public static boolean isContentPOS(String pos) {
		return (pos.startsWith("N") || pos.startsWith("V")
				|| pos.startsWith("A") || pos.startsWith("R"));
	}

	public static boolean isNumber(String pos) {
		return (pos.startsWith("CD"));
	}

	public static boolean isClosedClass(String pos) {
		return (!isContentPOS(pos) && !isNumber(pos));
	}

	/**
	 * @param mStart begin of candidate span
	 * @param mEnd end of candidate span
	 * @param fstart begin of target span
	 * @param fend end of target span
	 * @return true if candidate span overlaps with target span.
	 */
	private static boolean isOverlap(int mStart, int mEnd, int fstart, int fend) {
		return (fstart <= mStart && mStart <= fend) || (mStart <= fstart && fstart <= mEnd);
	}

	/**
	 * lemma ,POS tag ,voice and relative position(with respect to target)
	 * of each word in the candidate span
	 */
	private void extractChildPOSFeatures(Multiset<String> featureMap,
										 DataPointWithFrameElements dp,
										 DependencyParse[] nodes,
										 final Range0Based fillerSpanRange,
										 String frameAndRoleName,
										 String roleName) {
		int targetStart = dp.getTargetTokenIdxs()[0];
		int targetEnd = dp.getTargetTokenIdxs()[dp.getTargetTokenIdxs().length - 1];
		//for each word in the frame element span
		final int start = fillerSpanRange.start;
		final int end = fillerSpanRange.end;
		for (int i = start; i <= end; i++) {
			DependencyParse node = nodes[i + 1];
			final Voice voice = findVoice(node);
			//lemma of the word
			final String lemma = node.getLemma();
			conjoinAndAdd("ltok_" + lemma, frameAndRoleName, roleName, ROLE_NAME, featureMap);
			//POS tag of the word
			conjoinAndAdd("POS_" + node.getPOS(), frameAndRoleName, roleName, NO_CONJOIN, featureMap);
			if (!voice.equals(Voice.NO_VOICE)) {
				//lemma and voice of the token
				conjoinAndAdd(UNDERSCORE.join("Voice", lemma, voice.name), frameAndRoleName, roleName, ROLE_NAME, featureMap);
				String before;
				if (i < targetStart) {
					before = "B4_TARGET";
				} else if (i > targetEnd) {
					before = "AF_TARGET";
				} else {
					before = "IN_TARGET";
				}
				//is the word before or after the frame evoking word
				//+ voice + lemma of the word
				conjoinAndAdd(UNDERSCORE.join("Voice", lemma, voice.name, before), frameAndRoleName, roleName, ROLE_NAME, featureMap);
			}
		}
		// up to 3 preceding POS tags
		for (int i = max(0, start - 3); i < start; i++) {
			final DependencyParse node = nodes[i + 1];
			conjoinAndAdd("pPOS_" + node.getPOS(), frameAndRoleName, roleName, NO_CONJOIN, featureMap);
		}
		// up to 3 following POS tags
		for (int i = end +  1; i <= end + 3; i++) {
			if (i >= nodes.length - 1)
				break;
			final DependencyParse node = nodes[i + 1];
			conjoinAndAdd("nPOS_" + node.getPOS(), frameAndRoleName, roleName, NO_CONJOIN, featureMap);
		}
	}

	private static String getDistToTarget(int fstart, int fend, int festart, int feend) {
		if (festart >= fstart) {
			return quantizeLength(fend - festart);
		}
		if (festart <= fstart) {
			return quantizeLength(fstart - feend);
		}
		return "0";
	}

	public static Voice findVoice(DependencyParse tn) {
		if (!tn.getPOS().startsWith("V"))
			return Voice.NO_VOICE;
		if (tn.getWord().toLowerCase().equals("been"))
			return Voice.ACTIVE;
		if (!tn.getPOS().equals("VBN"))
			return Voice.ACTIVE;

		return findVoiceInParents(tn.getParent());
	}

	private static Voice findVoiceInParents(DependencyParse n) {
		if (n == null || n.getIndex() == 0)
			return Voice.PASSIVE;
		final String word = n.getWord().toLowerCase(), pos = n.getPOS();

		if (pos.startsWith("NN"))
			return Voice.PASSIVE;

		if (word.matches("am|are|is|was|were|be|been|being"))
			return Voice.PASSIVE;

		if (word.matches("ha(ve|s|d|ving)"))
			return Voice.ACTIVE;

		if (pos.matches("VBZ|VBD|VBP|MD"))
			return Voice.PASSIVE;

		return findVoiceInParents(n.getParent());
	}
}
