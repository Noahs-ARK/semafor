package edu.cmu.cs.lti.ark.fn.parsing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.concurrent.Immutable;
import java.util.List;

/**
 * POJO for serializing Semafor parse results to json
 *
 * @author sthomson@cs.cmu.edu
 */
@Immutable
public class SemaforParseResult {
	/** The list of predicted frames **/
	final public List<Frame> frames;
	/** The original text of the sentence **/
	final public List<String> tokens;

	@JsonCreator
	public SemaforParseResult(@JsonProperty("frames") List<Frame> frames,
							  @JsonProperty("tokens") List<String> tokens) {
		this.frames = frames;
		this.tokens = tokens;
	}

	public static class Frame {
		/** The target of the predicted frame **/
		final public Span target;
		/** The list of predicted frame elements for the frame */
		final public List<Span> frameElements;

		@JsonCreator
		public Frame(@JsonProperty("target") Span target,
					 @JsonProperty("frame_elements") List<Span> frameElements) {
			this.target = target;
			this.frameElements = frameElements;
		}

		public static class Span {
			/** The start index of the target or frame element, 0-indexed on word boundaries **/
			final public int start;
			/** The end index of the target or frame element, 0-indexed on word boundaries **/
			final public int end;
			/** The name of the target or frame element **/
			final public String name;
			/** The original text of the target or frame element **/
			final public String text;

			@JsonCreator
			public Span(@JsonProperty("start") int start,
						@JsonProperty("end") int end,
						@JsonProperty("name") String name,
						@JsonProperty("text") String text) {
				this.start = start;
				this.end = end;
				this.name = name;
				this.text = text;
			}
		}
	}
}
