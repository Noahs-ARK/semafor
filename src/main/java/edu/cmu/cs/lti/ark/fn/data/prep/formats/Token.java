package edu.cmu.cs.lti.ark.fn.data.prep.formats;

import com.google.common.base.*;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * Represents one token, with space for all fields in the Conll format.
 * Only `form` is mandatory.
 * See constructor for details
 *
 * @author sthomson@cs.cmu.edu
 */
@Immutable
public class Token {
	private final @Nullable Integer id;
	private final String form;
	private final @Nullable String lemma;
	private final @Nullable String cpostag;
	private final @Nullable String postag;
	private final @Nullable String feats;
	private final @Nullable Integer head;
	private final @Nullable String deprel;
	private final @Nullable Integer phead;
	private final @Nullable String pdeprel;

	private static final String MISSING_INDICATOR = "_";

	private static final Function<String, String> parseStr = Functions.identity();
	private static final Function<String, Integer> parseInt = new Function<String, Integer>() {
		@Nullable @Override
		public Integer apply(@Nullable String input) { return Integer.parseInt(input); }
	};

	/**
	 * Specification of the CoNLL format (from http://ilk.uvt.nl/conll/ ):
	 *
	 * @param id Token counter, starting at 1 for each new sentence
	 * @param form Word form or punctuation symbol
	 * @param lemma Lemma or stem (depending on particular data set) of word
	 *                 form, or an underscore if not available
	 * @param cpostag Fine-grained part-of-speech tag, where the tagset
	 *                  depends on the language, or identical to the
	 *                  coarse-grained part-of-speech tag if not available
	 * @param postag  Fine-grained part-of-speech tag, where the tagset
	 *                   depends on the language, or identical to the
	 *                   coarse-grained part-of-speech tag if not available
	 * @param feats Unordered set of syntactic and/or morphological features
	 *                 (depending on the  particular language), separated by a
	 *                 vertical bar (|), or an underscore if not available
	 * @param head Head of the current token, which is either a value of ID or
	 *                zero ('0'). Note that depending on the original treebank
	 *                annotation, there may be multiple tokens with an ID of
	 *                zero
	 * @param deprel Dependency relation to the HEAD. The set of dependency
	 *                  relations depends on the particular language. Note that
	 *                  depending on the original treebank annotation, the
	 *                  dependency relation may be meaningfull or simply 'ROOT'
	 * @param phead Projective head of current token, which is either a value
	 *                 of ID or zero ('0'), or an underscore if not available.
	 *                 Note that depending on the original treebank annotation,
	 *                 there may be multiple tokens an with ID of zero. The
	 *                 dependency structure resulting from the PHEAD column is
	 *                 guaranteed to be projective (but is not available for
	 *                 all languages), whereas the structures resulting from
	 *                 the HEAD column will be non-projective for some
	 *                 sentences of some languages (but is always available)
	 * @param pdeprel Dependency relation to the PHEAD, or an underscore if not
	 *                   available. The set of dependency relations depends on
	 *                   the particular language. Note that depending on the
	 *                   original treebank annotation, the dependency relation
	 */
	public Token(@Nullable Integer id,
				 String form,
				 @Nullable String lemma,
				 @Nullable String cpostag,
				 @Nullable String postag,
				 @Nullable String feats,
				 @Nullable Integer head,
				 @Nullable String deprel,
				 @Nullable Integer phead,
				 @Nullable String pdeprel) {
		this.id = id;
		this.form = form;
		this.lemma = lemma;
		this.cpostag = cpostag;
		this.postag = postag;
		this.feats = feats;
		this.head = head;
		this.deprel = deprel;
		this.phead = phead;
		this.pdeprel = pdeprel;
	}

	/**
	 * Convenience constructor for (word, pos, head, deprel)
	 * cpostag also gets the value of postag
	 */
	public Token(String form, @Nullable String postag, @Nullable Integer head, @Nullable String deprel) {
		this(null, form, null, postag, postag, null, head, deprel, null, null);
	}
	/**
	 * Convenience constructor for (word, pos)
	 */
	public Token(String form, @Nullable String postag) {
		this(form, postag, null, null);
	}
	/**
	 * Convenience constructor for (word,)
	 */
	public Token(String form) {
		this(form, null);
	}

	public static class PosToken extends Token {
		public PosToken(String form, String postag) {
			super(form, postag);
		}
	}

	public static class MaltToken extends Token {
		public MaltToken(String form, String postag, int head, String deprel) {
			super(form, postag, head, deprel);

		}
	}

	public Token withIndex(int id) {
		return new Token(id, form,lemma, cpostag, postag, feats, head, deprel, phead, pdeprel);
	}

	/*
	Getters
	 */
	@Nullable
	public Integer getId() {
		return id;
	}

	public String getForm() {
		return form;
	}

	@Nullable
	public String getLemma() {
		return lemma;
	}

	public Token setLemma(@Nullable String lemma) {
		return new Token(getId(), getForm(), lemma, getCpostag(), getPostag(), getFeats(),
				getHead(), getDeprel(), getPhead(), getPdeprel());
	}

	@Nullable
	public String getCpostag() {
		return cpostag;
	}

	@Nullable
	public String getPostag() {
		return postag;
	}

	@Nullable
	public String getFeats() {
		return feats;
	}

	@Nullable
	public Integer getHead() {
		return head;
	}

	@Nullable
	public String getDeprel() {
		return deprel;
	}

	@Nullable
	public Integer getPhead() {
		return phead;
	}

	@Nullable
	public String getPdeprel() {
		return pdeprel;
	}

	@Override
	public String toString() {
		return getForm();
	}

	/*
	Codecs
	 */
	/**
	 * Replace nulls with "_"
	 * @param a the value of the field to convert to a string
	 * @return either the field's encode, or "_" if it's null
	 */
	private static String fieldToConll(Object a) {
		return Optional.fromNullable(a).or(MISSING_INDICATOR).toString();
	}

	public String toConll() {
		return Joiner.on("\t").join(
				fieldToConll(id),
				fieldToConll(form),
				fieldToConll(lemma),
				fieldToConll(cpostag),
				fieldToConll(postag),
				fieldToConll(feats),
				fieldToConll(head),
				fieldToConll(deprel),
				fieldToConll(phead),
				fieldToConll(pdeprel)
		);
	}

	@Nullable
	private static <A> A parseConllField(String field, Function<String, A> fromStr) {
		return field.equals(MISSING_INDICATOR) ? null : fromStr.apply(field);
	}

	public static Token fromConll(String line) {
		final String[] fields = line.trim().split("\t");
		checkArgument(fields.length == 10, "ConllToken must have 10 \"\\t\"-separated fields");
		return new Token(
				parseConllField(fields[0], parseInt),  // id
				fields[1],  // form
				parseConllField(fields[2], parseStr),  // lemma
				parseConllField(fields[3], parseStr),  // cpostag
				parseConllField(fields[4], parseStr),  // postag
				parseConllField(fields[5], parseStr),  // feats
				parseConllField(fields[6], parseInt),  // head
				parseConllField(fields[7], parseStr),  // deprel
				parseConllField(fields[8], parseInt),  // phead
				parseConllField(fields[9], parseStr)); // pdeprel
	}

	public static PosToken fromPosTagged(String tokenStr) {
		final int splitPoint = tokenStr.lastIndexOf("_");
		checkArgument(splitPoint != -1, "PosToken must have 2 \"_\"-separated fields");
		final String token = tokenStr.substring(0, splitPoint);
		final String pos = tokenStr.substring(splitPoint + 1, tokenStr.length());
		return new PosToken(token, pos);
	}

	public String toPosTagged() {
		return form + "_" + postag;
	}

	public static MaltToken fromMalt(String tokenStr) {
		final String[] fields = tokenStr.split("/");
		checkArgument(fields.length == 4, "MaltToken must have 4 \"/\"-separated fields");
		return new MaltToken(fields[0], fields[1], Integer.parseInt(fields[2]), fields[3]);
	}

	public String toMalt() {
		return Joiner.on("/").join(new Object[] {form, postag, head, deprel});
	}
}
