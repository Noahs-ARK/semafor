package edu.cmu.cs.lti.ark.fn.identification;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Token;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import gnu.trove.TIntDoubleHashMap;

import java.io.IOException;
import java.util.*;

import static com.google.common.base.Strings.nullToEmpty;
import static edu.cmu.cs.lti.ark.fn.identification.FrameFeatureExtractor.AncestorFrameFeatureExtractor;
import static edu.cmu.cs.lti.ark.fn.identification.FrameFeatureExtractor.BasicFrameFeatureExtractor;

/**
 * Extracts features for the frame identification model
 *
 * @author sthomson@cs.cmu.edu
 */
public class IdFeatureExtractor {
	protected static final Joiner UNDERSCORE = Joiner.on("_");

	private final boolean useSentenceContextFeatures;
	private final FrameFeatureExtractor frameFeatureExtractor;
	private final Optional<SennaFeatureExtractor> sennaFeatureExtractor;

	public IdFeatureExtractor(boolean useSentenceContextFeatures, boolean useAncestors, boolean useSenna) {
		this.useSentenceContextFeatures = useSentenceContextFeatures;
		try {
			sennaFeatureExtractor = useSenna ? Optional.of(SennaFeatureExtractor.load()) : Optional.<SennaFeatureExtractor>absent();
			frameFeatureExtractor = useAncestors ? AncestorFrameFeatureExtractor.load() : new BasicFrameFeatureExtractor();
		} catch (IOException e) { throw new RuntimeException(e); }
	}

	public static IdFeatureExtractor fromName(String name) {
		final Map<String, Supplier<IdFeatureExtractor>> featureExtractorMap = ImmutableMap.of(
				"basic", new Supplier<IdFeatureExtractor>() {
					public IdFeatureExtractor get() { return new IdFeatureExtractor(false, false, false); }
				} ,
				"ancestor", new Supplier<IdFeatureExtractor>() {
					public IdFeatureExtractor get() { return new IdFeatureExtractor(false, true, false); }
				},
				"senna", new Supplier<IdFeatureExtractor>() {
					public IdFeatureExtractor get() { return new IdFeatureExtractor(false, false, true); }
				} );
		return featureExtractorMap.get(name.trim().toLowerCase()).get();
	}

	public Map<String, Double> getBaseFeatures(int[] targetTokenIdxs, Sentence sentence) {
		Arrays.sort(targetTokenIdxs);
		final Map<String, Double> results = Maps.newHashMap();
		// Get lemmas and postags for target
		results.putAll(getTargetWordFeatures(targetTokenIdxs, sentence));
		if (useSentenceContextFeatures) {
			results.putAll(getSentenceContextFeatures(sentence));
		}
		// syntactic features
		results.putAll(getSyntacticFeatures(targetTokenIdxs, sentence));
		if (sennaFeatureExtractor.isPresent()) {
			results.putAll(sennaFeatureExtractor.get().getSennaFeatures(targetTokenIdxs, sentence));
		}
		// add homogenous/bias feature
		results.put("bias", 1.0);
		return results;
	}

	protected Map<String, Double> getTargetWordFeatures(int[] targetTokenIdxs, Sentence sentence) {
		final List<String> tokenAndCpostags = Lists.newArrayListWithExpectedSize(targetTokenIdxs.length);
		final List<String> cpostags = Lists.newArrayListWithExpectedSize(targetTokenIdxs.length);
		final List<String> lemmaAndCpostags = Lists.newArrayListWithExpectedSize(targetTokenIdxs.length);
		final List<Token> tokens = sentence.getTokens();
		for (int tokenIdx : targetTokenIdxs) {
			Token token = tokens.get(tokenIdx);
			final String cpostag = getCpostag(nullToEmpty(token.getPostag()).toUpperCase());
			cpostags.add(cpostag);
			tokenAndCpostags.add(token.getForm() + "_" + cpostag);
			lemmaAndCpostags.add(token.getLemma() + "_" + cpostag);
		}
		final IntCounter<String> featureMap = new IntCounter<String>();
		featureMap.increment("aP:" + UNDERSCORE.join(cpostags));
		featureMap.increment("aTP:" + UNDERSCORE.join(tokenAndCpostags));
		featureMap.increment("aLP:" + UNDERSCORE.join(lemmaAndCpostags));
		return featureMap.scaleBy(1.0);
	}

	protected Map<String, Double> getSentenceContextFeatures(Sentence sentence) {
		// add a feature for each word in the sentence
		final IntCounter<String> featureMap = new IntCounter<String>();
		for (Token token : sentence.getTokens()) {
			final String cpostag = getCpostag(nullToEmpty(token.getPostag()).toUpperCase());
			featureMap.increment("sTP:" + token.getForm() + "_" + cpostag);
			featureMap.increment("sLP:" + token.getLemma() + "_" + cpostag);
		}
		return featureMap.scaleBy(1.0);
	}

	protected Map<String, Double> getSyntacticFeatures(int[] targetTokenIdxs, Sentence sentence) {
		final DependencyParse parse = DependencyParse.processFN(sentence.toAllLemmaTagsArray(), 0.0);
		final DependencyParse head = DependencyParse.getHeuristicHead(parse.getIndexSortedListOfNodes(), targetTokenIdxs);
		final String headCpostag = getCpostag(head.getPOS());

		final List<DependencyParse> children = head.getChildren();

		final SortedSet<String> depLabels = Sets.newTreeSet(); // unordered set of arc labels of children
		for (DependencyParse child : children) {
			depLabels.add(child.getLabelType().toUpperCase());
		}
		final IntCounter<String> featureMap = new IntCounter<String>();
		featureMap.increment("d:" + UNDERSCORE.join(depLabels));

		final String depLabel = head.getLabelType().toUpperCase(); // dep. label between head and its parent
		featureMap.increment("dH:" + depLabel);

		if (headCpostag.equals("V")) {
			final List<String> subcat = Lists.newArrayListWithExpectedSize(children.size()); // ordered arc labels of children
			for (DependencyParse child : children) {
				final String labelType = child.getLabelType().toUpperCase();
				if (!labelType.equals("SUB") && !labelType.equals("P") && !labelType.equals("CC")) {
					// TODO(smt): why exclude "sub"?
					subcat.add(labelType);
				}
			}
			featureMap.increment("sC:" + UNDERSCORE.join(subcat));
		}
		final IntCounter<String> parentFeatures = getParentFeatures(head.getParent());
		return featureMap.addAll(parentFeatures).scaleBy(1.0);
	}

	protected IntCounter<String> getParentFeatures(DependencyParse parent) {
		IntCounter<String> featureMap = new IntCounter<String>();
		if (parent == null) {
			featureMap.increment("pP:NULL");
			featureMap.increment("pPL:NULL");
			featureMap.increment("pLab:NULL");
		} else {
			final String parentPostag = parent.getPOS().toUpperCase();
			// if parent is a preposition, collapse the dependency, Stanford-style
			if (parentPostag.startsWith("I") && parent.getParent() != null) {
				final DependencyParse gp = parent.getParent();
				final String gpPostag = gp.getPOS().toUpperCase();
				featureMap.increment("pP:" + gpPostag);
				featureMap.increment("pLP:" + gp.getLemma() + "_" + gpPostag);
				featureMap.increment("pLab:" + gp.getLabelType().toUpperCase() + "_" + parent.getLemma());
			} else {
				featureMap.increment("pP:" + parentPostag);
				featureMap.increment("pLP:" + parent.getLemma() + "_" + parentPostag);
				featureMap.increment("pLab:" + parent.getLabelType().toUpperCase());
			}
		}
		return featureMap;
	}

	public Map<String, Map<String, Double>> extractFeaturesByName(Iterable<String> frameNames,
																  int[] targetTokenIdxs,
																  Sentence sentence) {
		return frameFeatureExtractor.conjoinAll(frameNames, getBaseFeatures(targetTokenIdxs, sentence));
	}

	public Map<String, TIntDoubleHashMap> extractFeaturesByIndex(Iterable<String> frames,
																 int[] targetTokenIdxs,
																 Sentence sentence,
																 Map<String, Integer> alphabet) {
		return convertToIndexes(extractFeaturesByName(frames, targetTokenIdxs, sentence), alphabet);
	}

	public Set<String> getConjoinedFeatureNames(Iterable<String> frameNames, String feature) {
		return frameFeatureExtractor.getAllConjoinedFeatureNames(frameNames, feature);
	}

	/** Replaces feature names with feature indexes */
	protected static Map<String, TIntDoubleHashMap> convertToIndexes(Map<String, Map<String, Double>> featuresByFrame,
																	 Map<String, Integer> alphabet) {
		final Map<String, TIntDoubleHashMap> results = Maps.newHashMap();
		for (String frame : featuresByFrame.keySet()) {
			final Map<String, Double> features = featuresByFrame.get(frame);
			final TIntDoubleHashMap featsForFrame = new TIntDoubleHashMap(features.size());
			for (String feat : features.keySet()) {
				if (alphabet.containsKey(feat)) {
					featsForFrame.put(alphabet.get(feat), features.get(feat));
				}
			}
			results.put(frame, featsForFrame);
		}
		return results;
	}

	public static String getCpostag(String postag) {
		return postag.substring(0, 1).toUpperCase();
	}
}
