package edu.cmu.cs.lti.ark.fn.identification;

import com.google.common.primitives.Ints;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectDoubleHashMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author sthomson@cs.cmu.edu
 */
public class GraphBasedFrameIdentifier extends FastFrameIdentifier {
	final protected SmoothedGraph graph;
	public GraphBasedFrameIdentifier(TObjectDoubleHashMap<String> paramList,
									 String reg,
									 double l,
									 THashMap<String, THashSet<String>> frameMap,
									 THashMap<String, THashSet<String>> hvCorrespondenceMap,
									 Map<String, Set<String>> relatedWordsForWord,
									 Map<String, Map<String, Set<String>>> revisedRelationsMap,
									 Map<String, String> hvLemmas,
									 SmoothedGraph graph) {
		super(paramList, reg, l, frameMap, hvCorrespondenceMap, relatedWordsForWord, revisedRelationsMap, hvLemmas);
		this.graph = graph;
	}

	public String getBestFrame(List<Integer> indices, Sentence sentence) {
		return getBestFrame(Ints.toArray(indices), sentence, graph);
	}
}
