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
import edu.cmu.cs.lti.ark.fn.utils.DataPointWithFrameElements;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;
import edu.cmu.cs.lti.ark.util.ds.Range1Based;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

import static edu.cmu.cs.lti.ark.fn.parsing.CandidateFrameElementFilters.isEmptySpan;
import static edu.cmu.cs.lti.ark.fn.parsing.FeatureExtractor.ConjoinLevel.*;

/**
 * Extract features for the parsing model. Based on FeatureExtractor for the
 * frame identification model.
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2009-04-07
 * @see CandidateFrameElementFilters
 * @see edu.cmu.cs.lti.ark.fn.identification.IdFeatureExtractor
 */
@NotThreadSafe
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

		private final String name;

		Voice(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	protected String frameName;
	protected String roleName;

	/**
	 * @param featureName feature to add
	 * @param level indicates whether to conjoin with role name and/or frame name.
	 */
	protected void conjoinAndIncrement(IntCounter<String> featureMap, String featureName, ConjoinLevel level) {
		switch(level) {
			case FRAME_AND_ROLE_NAME:
				String frameAndRoleName = frameName + "." + roleName;
				featureMap.increment(UNDERSCORE.join(featureName, frameAndRoleName));
				//intentional fall through
			case ROLE_NAME:
				featureMap.increment(UNDERSCORE.join(featureName, roleName));
			case NO_CONJOIN:
				featureMap.increment(featureName);
			default:
				break;
		}
	}

	public IntCounter<String> extractFeatures(DataPointWithFrameElements dp,
											  String frameName,
											  String roleName,
											  final Range0Based fillerSpanRange,
											  DependencyParse parse) {
		final IntCounter<String> featureMap = new IntCounter<String>();
		this.frameName = frameName;
		this.roleName = roleName;
		
		int[] targetTokenNums = dp.getTargetTokenIdxs();

		DependencyParse[] nodes = parse.getIndexSortedListOfNodes();
		DependencyParse targetHeadNode = DependencyParse.getHeuristicHead(nodes, targetTokenNums);

		final boolean isEmpty = isEmptySpan(fillerSpanRange);
		String overtness = isEmpty ? "NULL" : "OVERT";
		conjoinAndIncrement(featureMap, overtness, FRAME_AND_ROLE_NAME);	// overtness of the role
		
		String nullness = isEmpty ? "NULL_" : "";

		for (int targetTokenNum : targetTokenNums) {
			final Voice voice = findVoice(nodes[targetTokenNum + 1]);
			conjoinAndIncrement(featureMap, nullness + "targetLemma_"
					+ nodes[targetTokenNum + 1].getLemma(), FRAME_AND_ROLE_NAME);
			conjoinAndIncrement(featureMap, nullness + "targetLemma_"
					+ nodes[targetTokenNum + 1].getLemma() + "_"
					+ voice, ROLE_NAME);
			conjoinAndIncrement(featureMap, nullness + "targetPOS_"
					+ nodes[targetTokenNum + 1].getPOS(), FRAME_AND_ROLE_NAME);
		}
		

		List<DependencyParse> tgtChildren = targetHeadNode.getChildren();
		conjoinAndIncrement(featureMap, nullness + "NCHILDREN_" + tgtChildren.size(), FRAME_AND_ROLE_NAME);	// number of children

		// Dependency subcategorization
		String dsubcat = "";
		if (tgtChildren.size() > 0) {
			for (DependencyParse dpn : tgtChildren) {
				dsubcat += dpn.getLabelType() + "_";
				conjoinAndIncrement(featureMap, nullness + "SUBCAT_" + dpn.getLabelType(), ROLE_NAME);
			}
			conjoinAndIncrement(featureMap, nullness + "SUBCATSEQ" + dsubcat, ROLE_NAME);
		}
		
		
		if (!isEmpty) { // null span
			 // lemma, POS tag, voice, and relative position (with respect to target)
			 // of each word in the candidate span
			extractChildPOSFeatures(featureMap, dp, nodes, fillerSpanRange);
			DependencyParse fillerHeadNode = DependencyParse.getHeuristicHead(
					nodes, fillerSpanRange);
			List<Pair<String, DependencyParse>> targetToFillerPath =
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
			final int start = fillerSpanRange.start;
			final int end = fillerSpanRange.end;
			if (isOverlap(start, end,
					targetTokenNums[0],
					targetTokenNums[targetTokenNums.length - 1])) {
				
				//a few features describing
				//relative position of the span with respect to the target
				
				//does the span overlap with target
				conjoinAndIncrement(featureMap, "O_W_T", FRAME_AND_ROLE_NAME);
				if (targetTokenNums.length > 1) {
					if ((start < targetTokenNums[0] && end < targetTokenNums[targetTokenNums.length - 1])
							|| (start > targetTokenNums[0] && end > targetTokenNums[targetTokenNums.length - 1])) {
						//does the span cross the target
						conjoinAndIncrement(featureMap, "CROS_TAR", NO_CONJOIN);
					}
				}
			} else {
				// distance between nearest words of span and target
				String dist = getDistToTarget(targetTokenNums[0],
						targetTokenNums[targetTokenNums.length - 1],
						start, end);
				String feature = "dist_" + dist;
				conjoinAndIncrement(featureMap, feature, NO_CONJOIN);
				if (dist.charAt(0) == '-') {
					//span is left to target
					feature = "LEFTTAR";
				} else {
					//span is right to target
					feature = "RIGHTTAR";
				}
				conjoinAndIncrement(featureMap, feature, FRAME_AND_ROLE_NAME);
				int fmid = (targetTokenNums[0] + targetTokenNums[targetTokenNums.length - 1]) / 2;
				int femid = (start + end) / 2;
				//distance between words in the middle
				//of target span and candidate span
				feature = "midDist_"
						+ getDistToTarget(fmid, fmid, femid, femid);
				conjoinAndIncrement(featureMap, feature, NO_CONJOIN);
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
			conjoinAndIncrement(featureMap, "depPath_" + depTypePath, NO_CONJOIN);
			conjoinAndIncrement(featureMap, "pathLength_" + quantizeLength(pathSize), NO_CONJOIN);
			

			// head word
			// left and right most dependents
			List<DependencyParse> children = fillerHeadNode.getChildren();
			conjoinAndIncrement(featureMap, "headLemma_" + fillerHeadNode.getLemma(), NO_CONJOIN);
			conjoinAndIncrement(featureMap, "headPOS_" + fillerHeadNode.getPOS(), NO_CONJOIN);
			conjoinAndIncrement(featureMap, "headLabel_" + fillerHeadNode.getLabelType(), NO_CONJOIN);
			if (children.size() > 0) {
				conjoinAndIncrement(featureMap, "leftLemma_" + children.get(0).getLemma(), NO_CONJOIN);
				conjoinAndIncrement(featureMap, "leftPOS_" + children.get(0).getPOS(), NO_CONJOIN);
				conjoinAndIncrement(featureMap, "rightLemma_"
						+ children.get(children.size() - 1).getLemma(), NO_CONJOIN);
				conjoinAndIncrement(featureMap, "rightPOS_"
						+ children.get(children.size() - 1).getPOS(), NO_CONJOIN);
			}
			
			// word/POS/dependency type of 1st,  FrameAndRoleNamend, last word in the span
			int startNode = new Range1Based(fillerSpanRange).start;
			int endNode = new Range1Based(fillerSpanRange).end;

			if (isClosedClass(nodes[startNode].getPOS()))
				conjoinAndIncrement(featureMap, "w[0]pos[0]_" + nodes[startNode].getWord()
						+ " " + nodes[startNode].getPOS(), FRAME_AND_ROLE_NAME);
			conjoinAndIncrement(featureMap, "dep[0]_" + nodes[startNode].getLabelType(), FRAME_AND_ROLE_NAME);

			if (endNode - startNode > 0) {
				if (isClosedClass(nodes[startNode + 1].getPOS()))
					conjoinAndIncrement(featureMap, "w[1]pos[1]_"
							+ nodes[startNode + 1].getWord() + " "
							+ nodes[startNode + 1].getPOS(), FRAME_AND_ROLE_NAME
					);
				conjoinAndIncrement(featureMap, "dep[1]_" + nodes[startNode + 1].getLabelType(), FRAME_AND_ROLE_NAME);
				if (endNode - startNode > 1) {
					if (isClosedClass(nodes[endNode].getPOS()))
						conjoinAndIncrement(featureMap, "w[-1]pos[-1]_"
								+ nodes[endNode].getWord() + " "
								+ nodes[endNode].getPOS() + "_"
								, FRAME_AND_ROLE_NAME);
					conjoinAndIncrement(featureMap, "dep[-1]_"
							+ nodes[endNode].getLabelType() + "_"
							, FRAME_AND_ROLE_NAME);
				}
			}
			
			// length of the filler span
			conjoinAndIncrement(featureMap, "len_" + quantizeLength(endNode - startNode + 1), FRAME_AND_ROLE_NAME);
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
	private void extractChildPOSFeatures(IntCounter<String> featureMap,
										 DataPointWithFrameElements dp,
										 DependencyParse[] nodes,
										 final Range0Based fillerSpanRange) {
		int targetStart = dp.getTargetTokenIdxs()[0];
		int targetEnd = dp.getTargetTokenIdxs()[dp.getTargetTokenIdxs().length - 1];
		//for each word in the frame element span
		final int start = fillerSpanRange.start;
		final int end = fillerSpanRange.end;
		for (int i = start; i <= end; i++) {
			DependencyParse node = nodes[i + 1];
			final Voice voice = findVoice(node);
			//lemma of the word
			String feature;
			feature = "ltok_"+node.getLemma();
			conjoinAndIncrement(featureMap, feature, ROLE_NAME);
			//POS tag of the word
			feature = "POS_" + node.getPOS();
			conjoinAndIncrement(featureMap, feature, NO_CONJOIN);
			if (!voice.equals(Voice.NO_VOICE)) {
				//lemma and voice of the token
				conjoinAndIncrement(featureMap, "Voice_" + node.getLemma() + "_" + voice, ROLE_NAME);
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
				conjoinAndIncrement(featureMap, "Voice_" + node.getLemma() + "_" + voice
						+ "_" + before, ROLE_NAME);
			}
			/*feature="BIGRAM"+prevword+"_"+node.getLemma();
			featureMap.increment(feature);
			prevword=node.getLemma();
			feature="POSBIGRAM"+prevPOS+"_"+node.getPOS();
			featureMap.increment(feature);
			prevPOS=node.getPOS();
			 feature = "ltok_"+node.getLemma()+"_"+roleName;
			 featureMap.increment(feature);
			 feature = "ltok_"+node.getLemma()+"_"+frameAndRoleName;
			 featureMap.increment(feature);
			 */
		}
		// up to 3 preceding POS tags
		for (int i = start - 3; i < start; i++) {
			if (i < 0)
				continue;
			DependencyParse node = nodes[i + 1];
			conjoinAndIncrement(featureMap, "pPOS_" + node.getPOS(), NO_CONJOIN);
		}
		// up to 3 following POS tags
		for (int i = end +1; i <= end + 3; i++) {
			if (i >= nodes.length - 1)
				break;
			DependencyParse node = nodes[i + 1];
			conjoinAndIncrement(featureMap, "nPOS_" + node.getPOS(), NO_CONJOIN);
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
		String word = n.getWord().toLowerCase(), pos = n.getPOS();

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
