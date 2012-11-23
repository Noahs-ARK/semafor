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

import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import de.saar.coli.salsa.reiter.framenet.FrameElement;

import edu.cmu.cs.lti.ark.fn.utils.DataPointWithElements;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import edu.cmu.cs.lti.ark.util.IFeatureExtractor;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;
import edu.cmu.cs.lti.ark.util.ds.Range1Based;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;

/**
 * Extract features for the parsing model. Based on FeatureExtractor for the
 * frame identification model.
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2009-04-07
 * @see AlphabetCreation
 * @see CandidateFrameElementFilters
 * @see edu.cmu.cs.lti.ark.fn.identification.FeatureExtractor
 */
public class FeatureExtractor implements IFeatureExtractor<FeatureExtractor.ArgumentCandidate> {
	public class ArgumentCandidate {
		DataPointWithElements dp;
		String frameName;
		FrameElement fe;
		/** Only needs to be specified if {@code fe} is {@code null} */
		String roleName;
		Range0Based fillerSpanRange;
		WordNetRelations wnr;
		DependencyParse selectedParse;
	}
	
	public static final String PASSIVE = "PAS";
	public static final String ACTIVE = "ACT";
	public static final String NONE = "";
	
	protected static IntCounter<String> featureMap;
	protected static String frameName;
	protected static String roleName;
	/**
	 * 
	 * @param featureName feature to add
	 * @param comb an integer indicating the combination with role name and frame name.
	 * 2 for frame and role name
	 * 1 for role name
	 * 0 for none
	 */
	protected static void $(String featureName, int comb) {
		switch(comb) {
			case 2:
			String frameAndRoleName = frameName + "." + roleName;
			featureMap.increment(featureName + "_" + frameAndRoleName);
			
			//intentional fall through
			
			case 1:
				featureMap.increment(featureName + "_" + roleName);
			case 0:
				featureMap.increment(featureName);
			default:
				break;
		}
	}
	
	public IntCounter<String> features(ArgumentCandidate arg) {
		if (arg.fe!=null)
			return extractFeatures(arg.dp, arg.frameName, arg.fe, arg.fillerSpanRange, arg.wnr, arg.selectedParse);
		return extractFeatures(arg.dp, arg.frameName, arg.roleName, arg.fillerSpanRange, arg.wnr, arg.selectedParse);
	}
	
	public static IntCounter<String> extractFeatures(DataPointWithElements dp,
			String frameName, FrameElement fe, Range0Based fillerSpanRange,
			WordNetRelations wnr /* , Map<String,Frame> frameLexicon */, DependencyParse selectedParse) {
			return extractFeatures(dp,frameName,fe.getName(),fillerSpanRange,wnr, selectedParse);
	}
	
	public static IntCounter<String> extractFeatures(DataPointWithElements dp,
			String frameName, String roleName, Range0Based fillerSpanRange,
			WordNetRelations wnr /* , Map<String,Frame> frameLexicon */, DependencyParse selectedParse) {
		featureMap = new IntCounter<String>();
		FeatureExtractor.frameName = frameName;
		FeatureExtractor.roleName = roleName;
		
		// String target = dp.getTarget();
		int[] targetTokenNums = dp.getTokenNums();
		DependencyParse parse = selectedParse;

		// String sentence = parse.getSentence();
		DependencyParse[] nodes = DependencyParse
				.getIndexSortedListOfNodes(parse);
		DependencyParse targetHeadNode = DependencyParse.getHeuristicHead(nodes, targetTokenNums);
		DependencyParse targetFirstNode = nodes[targetTokenNums[0] + 1];
		// String targetPOS = targetFirstNode.getPOS();
		// String targetLemma = wnr.getLemmaForWord(target, targetPOS);
		String targetLemma = targetFirstNode.getLemma();

		// StringTokenizer st = new StringTokenizer(target);
		// while(st.hasMoreTokens())
		// String word = st.nextToken().trim();
		// lemma+=mWnr.getLemmaForWord(word, pos)+" ";
		// mFeatureMap.put("l_"+lemma+"_"+mFrameAndRoleName, 1.0);

		String lemma = "";
		String finePOSSeq = "";
		String coarsePOSSeq = "";

		/*
		 * String feS = ""; for (int i=mFillerStart; i<=mFillerEnd; i++) {
		 * String word = nodes[i].getWord(); feS += word + " "; String pos =
		 * nodes[i].getPOS(); finePOSSeq+= pos+" "; coarsePOSSeq+=
		 * pos.substring(0,1)+" "; }
		 */

		lemma = lemma.trim();
		finePOSSeq = finePOSSeq.trim();
		coarsePOSSeq = coarsePOSSeq.trim();

		String overtness = (CandidateFrameElementFilters.isEmptySpan(fillerSpanRange)) ? "NULL" : "OVERT";
		$(overtness,2);	// overtness of the role
		
		String nullness = (CandidateFrameElementFilters.isEmptySpan(fillerSpanRange)) ? "NULL_" : "";

		for (int i = 0; i < targetTokenNums.length; i++) {
			String voice = findVoice(nodes[targetTokenNums[i] + 1]);
			$(nullness+"targetLemma_"
					+ nodes[targetTokenNums[i] + 1].getLemma(),2);
			$(nullness+"targetLemma_"
					+ nodes[targetTokenNums[i] + 1].getLemma() + "_"
					+ voice,1);
			$(nullness+"targetPOS_"
					+ nodes[targetTokenNums[i] + 1].getPOS(),2);
		}
		

		List<DependencyParse> tgtChildren = targetHeadNode.getChildren();
		$(nullness+"NCHILDREN_" + tgtChildren.size(),2);	// number of children

		// Dependency subcategorization
		String dsubcat = "";
		if (tgtChildren.size() > 0) {
			for (DependencyParse dpn : tgtChildren) {
				dsubcat += dpn.getLabelType() + "_";
				$(nullness+"SUBCAT_" + dpn.getLabelType(),1);
			}
			$(nullness+"SUBCATSEQ" + dsubcat,1);
		}
		
		
		if (!CandidateFrameElementFilters.isEmptySpan(fillerSpanRange)) { // null span
			 // lemma, POS tag, voice, and relative position (with respect to target)
			 // of each word in the candidate span
			extractChildPOSFeatures(dp, nodes, frameName, roleName,
					fillerSpanRange);
			DependencyParse fillerHeadNode = DependencyParse.getHeuristicHead(
					nodes, fillerSpanRange);
			List<Pair<String, DependencyParse>> targetToFillerPath = DependencyParse
					.getPath(targetHeadNode, fillerHeadNode);
			int pathSize = targetToFillerPath.size();
			String depTypePath = ""; // target's POS and dependency types on the
			// path to the head of the filler
			// To avoid feature explosion, only include paths of length <=7, and
			// only include the dependency types if the path's length is <=5.  
			// (To put this in perspective, <2% of FE fillers have a path length
			// of >5 in the training/dev data, and hardly any have length >7.)
			// e.g. "=<VB> !VMOD !PMOD" ( GIVE to [the paper *boy*] )
			// "=<PRP> ^OBJ ^VMOD ^VMOD !OBJ" ( want [*him*] to make a REQUEST )
			if (isOverlap(fillerSpanRange.getStart(), fillerSpanRange.getEnd(),
					targetTokenNums[0],
					targetTokenNums[targetTokenNums.length - 1])) {
				
				//a few features describing
				//relative position of the span with respect to the target
				
				//does the span overlap with target
				$("O_W_T",2);
				if (targetTokenNums.length > 1) {
					if ((fillerSpanRange.getStart() < targetTokenNums[0] && fillerSpanRange
							.getEnd() < targetTokenNums[targetTokenNums.length - 1])
							|| (fillerSpanRange.getStart() > targetTokenNums[0] && fillerSpanRange
									.getEnd() > targetTokenNums[targetTokenNums.length - 1])) {
						//does the span cross the target
						$("CROS_TAR",0);
					}
				}
			} else {
				// distance between nearest words of span and target
				String dist = getDistToTarget(targetTokenNums[0],
						targetTokenNums[targetTokenNums.length - 1],
						fillerSpanRange.getStart(), fillerSpanRange.getEnd());
				String feature = "dist_" + dist;
				$(feature,0);
				if (dist.charAt(0) == '-') {
					//span is left to target
					feature = "LEFTTAR";
				} else {
					//span is right to target
					feature = "RIGHTTAR";
				}
				$(feature,2);
				int fmid = (targetTokenNums[0] + targetTokenNums[targetTokenNums.length - 1]) / 2;
				int femid = (fillerSpanRange.getStart() + fillerSpanRange
						.getEnd()) / 2;
				//distance between words in the middle
				//of target span and candidate span
				feature = "midDist_"
						+ getDistToTarget(fmid, fmid, femid, femid);
				$(feature,0);
			}
			
			
			if (pathSize <= 7) {
				DependencyParse lastNode = null;
				for (int i = 0; i < targetToFillerPath.size(); i++) {
					Pair<String, DependencyParse> item = targetToFillerPath
							.get(i);
					if (i == 0)
						depTypePath += "=<" + item.getSecond().getPOS() + ">";
					else if (item.getFirst().equals("^"))
						depTypePath += " ^"
								+ ((pathSize <= 5) ? lastNode.getLabelType()
										: "");
					else if (item.getFirst().equals("!"))
						depTypePath += " !"
								+ ((pathSize <= 5) ? item.getSecond()
										.getLabelType() : "");
					lastNode = item.getSecond();
				}
			} else {
				depTypePath = "=<"
						+ targetToFillerPath.get(0).getSecond().getPOS()
						+ "> ...";
			}
			$("depPath_" + depTypePath,0);
			$("pathLength_" + quantizeLength(pathSize),0);
			

			// head word
			// left and right most dependents
			List<DependencyParse> children = fillerHeadNode.getChildren();
			$("headLemma_" + fillerHeadNode.getLemma(),0);
			$("headPOS_" + fillerHeadNode.getPOS(),0);
			$("headLabel_" + fillerHeadNode.getLabelType(),0);
			if (children.size() > 0) {
				$("leftLemma_" + children.get(0).getLemma(),0);
				$("leftPOS_" + children.get(0).getPOS(),0);
				$("rightLemma_"
						+ children.get(children.size() - 1).getLemma(),0);
				$("rightPOS_"
						+ children.get(children.size() - 1).getPOS(),0);
			}
			
			// word/POS/dependency type of 1st, 2nd, last word in the span
			int startNode = new Range1Based(fillerSpanRange).getStart();
			int endNode = new Range1Based(fillerSpanRange).getEnd();

			if (isClosedClass(nodes[startNode].getPOS()))
				$("w[0]pos[0]_" + nodes[startNode].getWord()
						+ " " + nodes[startNode].getPOS() ,2);
			$("dep[0]_" + nodes[startNode].getLabelType(),2);

			if (endNode - startNode > 0) {
				if (isClosedClass(nodes[startNode + 1].getPOS()))
					$("w[1]pos[1]_"
							+ nodes[startNode + 1].getWord() + " "
							+ nodes[startNode + 1].getPOS() ,2
							);
				$("dep[1]_"+ nodes[startNode + 1].getLabelType() ,2);
				if (endNode - startNode > 1) {
					if (isClosedClass(nodes[endNode].getPOS()))
						$("w[-1]pos[-1]_"
								+ nodes[endNode].getWord() + " "
								+ nodes[endNode].getPOS() + "_"
								,2);
					$("dep[-1]_"
							+ nodes[endNode].getLabelType() + "_"
							,2);
				}
			}
			
			// length of the filler span
			$("len_" + quantizeLength(endNode - startNode + 1) ,2);
		}
		// featureMap.put("fe_" + feS +"_"+mFrameAndRoleName, 1.0);
		// featureMap.put("fPS_"+finePOSSeq+"_"+mFrameAndRoleName, 1.0);
		// featureMap.put("cPS_:"+coarsePOSSeq+"_"+mFrameAndRoleName, 1.0);

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

	public String getTokens(String sentence, int[] intNums) {
		StringTokenizer st = new StringTokenizer(sentence, " ", true);
		int count = 0;
		String result = "";
		Arrays.sort(intNums);
		while (st.hasMoreTokens()) {
			String token = st.nextToken().trim();
			if (token.equals(""))
				continue;
			if (Arrays.binarySearch(intNums, count) >= 0)
				result += token + " ";
			count++;
		}
		return result.trim();
	}

	/**
	 * 
	 * @param mStart
	 *            begin of candidate span
	 * @param mEnd
	 *            end of candidate span
	 * @param fstart
	 *            begin of target span
	 * @param fend
	 *            end of target span
	 * @return true if candidate span overlaps with target span.
	 */
	private static boolean isOverlap(int mStart, int mEnd, int fstart, int fend) {
		if (mStart >= fstart && fend >= mStart) {
			return true;
		}
		if (mStart <= fstart && fstart <= mEnd) {
			return true;
		}
		return false;

	}
	/**
	 * lemma ,POS tag ,voice and relative position(with respect to target)
	 * of each word in the candidate span
	 * @param dp
	 * @param nodes
	 * @param frameName
	 * @param roleName
	 * @param fillerSpanRange
	 */
	private static void extractChildPOSFeatures(DataPointWithElements dp,
			DependencyParse[] nodes, String frameName, String roleName,
			Range0Based fillerSpanRange) {
		int targetStart = dp.getTokenNums()[0];
		int targetEnd = dp.getTokenNums()[dp.getTokenNums().length - 1];
		//for each word in the frame element span
		for (int i = fillerSpanRange.getStart(); i <= fillerSpanRange.getEnd(); i++) {
			DependencyParse node = nodes[i + 1];
			String voice = findVoice(node);
			//lemma of the word
			String feature;
			feature = "ltok_"+node.getLemma();
			$(feature,1);
			//POS tag of the word
			feature = "POS_" + node.getPOS();
			$(feature,0);
			if (voice != NONE) {
				//lemma and voice of the token
				$("Voice_" + node.getLemma() + "_" + voice,1);
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
				$("Voice_" + node.getLemma() + "_" + voice
						+ "_" + before,1);
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
		for (int i = fillerSpanRange.getStart() - 3; i < fillerSpanRange.getStart(); i++) {
			if (i < 0)
				continue;
			DependencyParse node = nodes[i + 1];
			$("pPOS_" + node.getPOS(),0);
		}
		// up to 3 following POS tags
		for (int i = fillerSpanRange.getEnd()+1; i <= fillerSpanRange.getEnd() + 3; i++) {
			if (i >= nodes.length - 1)
				break;
			DependencyParse node = nodes[i + 1];
			$("nPOS_" + node.getPOS(),0);
		}
	}

	private static String getDistToTarget(int fstart, int fend, int festart,
			int feend) {
		if (festart >= fstart) {
			String dist = quantizeLength(fend - festart);
			return dist;
		}
		if (festart <= fstart) {
			String dist = quantizeLength(fstart - feend);
			return dist;
		}
		return "0";
	}

	public static String findVoice(DependencyParse tn) {
		if (!tn.getPOS().startsWith("V"))
			return NONE;
		if (tn.getWord().toLowerCase().equals("been"))
			return ACTIVE;
		if (!tn.getPOS().equals("VBN"))
			return ACTIVE;

		return findPassiveInParents(tn.getParent());
	}

	private static String findPassiveInParents(DependencyParse n) {
		if (n == null || n.getIndex() == 0)
			return PASSIVE;
		String word = n.getWord().toLowerCase(), pos = n.getPOS();

		if (pos.startsWith("NN"))
			return PASSIVE;

		if (word.matches("am|are|is|was|were|be|been|being"))
			return PASSIVE;

		if (word.matches("ha(ve|s|d|ving)"))
			return ACTIVE;

		if (pos.matches("VBZ|VBD|VBP|MD"))
			return PASSIVE;

		return findPassiveInParents(n.getParent());
	}
}
