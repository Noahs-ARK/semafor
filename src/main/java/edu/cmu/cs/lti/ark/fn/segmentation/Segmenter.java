package edu.cmu.cs.lti.ark.fn.segmentation;

import java.util.List;
import java.util.Set;


public interface Segmenter {
	List<String> getSegmentations(List<String> tokenNums, List<String> parses, Set<String> allRelatedWords);
}
