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
		final public NamedSpanSet target;
		/** The list of predicted frame elements for the frame */
		final public List<ScoredRoleAssignment> annotationSets;

		@JsonCreator
		public Frame(@JsonProperty("target") NamedSpanSet target,
					 @JsonProperty("annotationSets") List<ScoredRoleAssignment> annotationSets) {
			this.target = target;
			this.annotationSets = annotationSets;
		}

		public static class ScoredRoleAssignment {
			/** The rank of this role assignment within a k-best list */
			final public int rank;
			/** The score this role assignment received under our model */
			final public double score;
			/** The assignment of spans to roles */
			final public List<NamedSpanSet> frameElements;

			@JsonCreator
			public ScoredRoleAssignment(@JsonProperty("rank") int rank,
										@JsonProperty("score") double score,
										@JsonProperty("frameElements") List<NamedSpanSet> frameElements) {
				this.rank = rank;
				this.score = score;
				this.frameElements = frameElements;
			}
		}

		public static class NamedSpanSet {
			/** The name of the target or frame element **/
			final public String name;
			/** The set of spans which make up the target or frame element */
			final public List<Span> spans;

			@JsonCreator
			public NamedSpanSet(@JsonProperty("name") String name,
								@JsonProperty("spans") List<Span> spans) {
				this.name = name;
				this.spans = spans;
			}
		}

		public static class Span {
			/** The start index of the target or frame element, 0-indexed on word boundaries **/
			final public int start;
			/** The end index of the target or frame element, 0-indexed on word boundaries **/
			final public int end;
			/** The original text of the target or frame element **/
			final public String text;

			@JsonCreator
			public Span(@JsonProperty("start") int start,
						@JsonProperty("end") int end,
						@JsonProperty("text") String text) {
				this.start = start;
				this.end = end;
				this.text = text;
			}
		}
	}

	public String toJson() throws JsonProcessingException {
		return jsonMapper.writeValueAsString(this);
	}
}
