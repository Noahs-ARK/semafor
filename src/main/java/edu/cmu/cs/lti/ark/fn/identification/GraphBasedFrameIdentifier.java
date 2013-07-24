package edu.cmu.cs.lti.ark.fn.identification;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Token;
import edu.cmu.cs.lti.ark.fn.identification.latentmodel.LatentFeatureExtractor;
import edu.cmu.cs.lti.ark.fn.Semafor;
import edu.cmu.cs.lti.ark.fn.wordnet.CachedRelations;
import edu.cmu.cs.lti.ark.fn.wordnet.Relations;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.nlp.MorphaLemmatizer;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectDoubleHashMap;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.nullToEmpty;
import static edu.cmu.cs.lti.ark.util.SerializedObjects.readObject;

/**
 * A FastFrameIdentifier that uses a pre-computed graph to limit the number of frames it considers for each target.
 *
 * @author sthomson@cs.cmu.edu
 */
public class GraphBasedFrameIdentifier extends FastFrameIdentifier {
	/* trained model files */
	public static final String GRAPH_FILENAME = "sparsegraph.gz";
	public static final String ID_MODEL_FILE = "idmodel.dat";

	final protected SmoothedGraph graph;

	public GraphBasedFrameIdentifier(IdFeatureExtractor featureExtractor,
									 Set<String> allFrames,
									 THashMap<String, THashSet<String>> framesByLemma,
									 TObjectDoubleHashMap<String> params,
									 SmoothedGraph graph) {
		super(featureExtractor, params, allFrames, framesByLemma);
		this.graph = graph;
	}

	public static GraphBasedFrameIdentifier getInstance(String modelDirectory) throws IOException, ClassNotFoundException {
		final String graphFilename = new File(modelDirectory, GRAPH_FILENAME).getAbsolutePath();
		final String idParamsFile = new File(modelDirectory, ID_MODEL_FILE).getAbsolutePath();
		final String requiredDataFilename = new File(modelDirectory, Semafor.REQUIRED_DATA_FILENAME).getAbsolutePath();
		System.err.println("Initializing frame identification model...");
		System.err.println("Reading serialized required data");
		final RequiredDataForFrameIdentification r = readObject(requiredDataFilename);
		System.err.println("Done reading serialized required data");
		System.err.println("Reading graph from: " + graphFilename + "...");
		final SmoothedGraph graph = readObject(graphFilename);
		System.err.println("Read graph successfully.");
		System.err.println("Reading model parameters...");
		try {
			final Pair<IdFeatureExtractor,TObjectDoubleHashMap<String>> extractorAndParams =
					FrameIdentificationRelease.parseParamFile(idParamsFile);
			System.err.println("Done reading model parameters.");
			final IdFeatureExtractor featureExtractor = extractorAndParams.first;
			final TObjectDoubleHashMap<String> params = extractorAndParams.second;
			return new GraphBasedFrameIdentifier(
					featureExtractor,
					r.getFrameMap().keySet(),
					r.getcMap(),
					params,
					graph);
		} catch (NullPointerException e) { // TODO: this is a gross way to fallback
			final TObjectDoubleHashMap<String> params =
					FrameIdentificationRelease.readOldModel(idParamsFile);
			System.err.println("Done reading model parameters.");
			final Relations wnRelations =
					new CachedRelations(r.getRevisedRelMap(), r.getRelatedWordsForWord());
			final LatentFeatureExtractor latentFeatureExtractor =
					new LatentFeatureExtractor(wnRelations, new MorphaLemmatizer());
			return new LatentGraphBasedFrameIdentifier(
					latentFeatureExtractor,
					r.getFrameMap(),
					r.getcMap(),
					params,
					graph);
		}
	}

	@Override
	public String getBestFrame(int[] tokenIndices, Sentence sentence) {
		final Set<String> candidateFrames = getCandidateFrames(tokenIndices, sentence);
		return pickBestFrame(candidateFrames, sentence, tokenIndices);
	}

	protected Set<String> getCandidateFrames(int[] tokenIndices, Sentence sentence) {
		final List<Token> sentenceTokens = sentence.getTokens();
		final Optional<THashSet<String>> frames = checkPresenceOfTokensInMap(tokenIndices, sentence);
		if (frames.isPresent()) return frames.get();

		final List<Token> frameTokens = Lists.newArrayList();
		final List<String> lowerCaseForms = Lists.newArrayList();
		for (int tokNum : tokenIndices) {
			final Token token = sentenceTokens.get(tokNum);
			frameTokens.add(token);
			lowerCaseForms.add(token.getForm().toLowerCase());
		}
		final Map<String, Set<String>> coarseMap = graph.getCoarseMap();
		if (frameTokens.size() > 1) {
			final String coarseToken = getCanonicalForm(Joiner.on(" ").join(lowerCaseForms));
			if (coarseMap.containsKey(coarseToken)) return coarseMap.get(coarseToken);
		} else {
			final Token token = frameTokens.get(0);
			final String lemma = token.getLemma();
			final String pos = convertPostag(token.getPostag());
			if (pos != null) {
				final String fineToken = getCanonicalForm(lemma + "." + pos);
				final Map<String, Set<String>> fineMap = graph.getFineMap();
				if (fineMap.containsKey(fineToken)) return fineMap.get(fineToken);
			}
			final String coarseToken = getCanonicalForm(lemma);
			if (coarseMap.containsKey(coarseToken)) return coarseMap.get(coarseToken);
		}
		return allFrames;
	}

	private static String getCanonicalForm(String word) {
		int len = word.length();
		String ans = "";
		for (int i = 0; i < len; i ++) {
			char c = word.charAt(i);
			if (Character.isDigit(c)) {
				ans += "@";
			} else {
				ans += c;
			}
		}
		return ans.toLowerCase();
	}

	/* convert from PTB postags to FrameNet postags */
	private String convertPostag(String postag) {
		final String postagUpper = nullToEmpty(postag).toUpperCase();
		if (postagUpper.startsWith("N")) {
			return  "n";
		} else if (postagUpper.startsWith("V")) {
			return  "v";
		} else if (postagUpper.startsWith("J")) {
			return "a";
		} else if (postagUpper.startsWith("RB")) {
			return "adv";
		} else if (postagUpper.startsWith("I") || postagUpper.startsWith("TO")) {
			return "prep";
		} else {
			return null;
		}
	}
}
