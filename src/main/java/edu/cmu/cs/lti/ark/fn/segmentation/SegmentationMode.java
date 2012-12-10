package edu.cmu.cs.lti.ark.fn.segmentation;

/**
 * @author sthomson@cs.cmu.edu
 */
public enum SegmentationMode  {
	GOLD("gold"),
	STRICT("strict"),
	RELAXED("relaxed");

	private final String value;

	SegmentationMode(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
