package edu.cmu.cs.lti.ark.fn.parsing;

import java.util.List;

/**
 * POJO for serializing results to json
 *
 * @author sthomson@cs.cmu.edu
 */
public class SemaforParse {
	public List<Frame> frames;
	public List<String> text;

	public static class Frame {
		public Span target;
		public List<Span> frame_elements;

		public static class Span {
			public int start;
			public int end;
			public String name;
			public String text;
		}
	}
}
