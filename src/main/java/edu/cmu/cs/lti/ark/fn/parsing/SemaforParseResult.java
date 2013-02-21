package edu.cmu.cs.lti.ark.fn.parsing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.concurrent.Immutable;
import java.util.List;

/**
 * POJO for serializing Semafor parse results to json
 *
 * @author sthomson@cs.cmu.edu
 */
@Immutable
public class SemaforParseResult {
	private static final ObjectMapper jsonMapper = new ObjectMapper();

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
		final public List<ScoredSpanList> annotationSets;

		@JsonCreator
		public Frame(@JsonProperty("target") Span target,
					 @JsonProperty("annotationSets") List<ScoredSpanList> annotationSets) {
			this.target = target;
			this.annotationSets = annotationSets;
		}

		public static class ScoredSpanList {
			final public int rank;
			final public double score;
			final public List<Span> frameElements;

			@JsonCreator
			public ScoredSpanList(@JsonProperty("rank") int rank,
								  @JsonProperty("score") double score,
								  @JsonProperty("frameElements") List<Span> frameElements) {
				this.rank = rank;
				this.score = score;
				this.frameElements = frameElements;
			}
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

	public String toJson() throws JsonProcessingException {
		return jsonMapper.writeValueAsString(this);
	}
}
