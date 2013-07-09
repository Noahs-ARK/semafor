package edu.cmu.cs.lti.ark.fn.segmentation;

import java.util.List;


public interface Segmenter {
	List<String> getSegmentations(List<String> sentenceIdxs, List<String> parses);
}
