package edu.cmu.cs.lti.ark.fn.identification;

import com.google.common.primitives.Ints;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.parsing.Semafor;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectDoubleHashMap;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import static edu.cmu.cs.lti.ark.fn.data.prep.formats.AllLemmaTags.readLine;
import static edu.cmu.cs.lti.ark.util.SerializedObjects.readObject;

/**
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
		System.err.println("Reading model parameters...");
		final Pair<IdFeatureExtractor,TObjectDoubleHashMap<String>> extractorAndParams =
				FrameIdentificationRelease.parseParamFile(idParamsFile);
		final IdFeatureExtractor featureExtractor = extractorAndParams.getFirst();
		final TObjectDoubleHashMap<String> params =
				extractorAndParams.getSecond();
		System.err.println("Done reading model parameters.");
		System.err.println("Reading graph from: " + graphFilename + "...");
		final SmoothedGraph graph = readObject(graphFilename);
		System.err.println("Read graph successfully.");
		final RequiredDataForFrameIdentification r = readObject(requiredDataFilename);
		return new GraphBasedFrameIdentifier(
				featureExtractor,
				r.getFrameMap().keySet(),
				r.getcMap(),
				params,
				graph);
	}

	public String getBestFrame(Collection<Integer> indices, Sentence sentence) {
		return getBestFrame(Ints.toArray(indices), sentence);
	}

	@Override
	public String getBestFrame(String frameLine, String parseLine) {
		return getBestFrame(parseFrameLine(frameLine), Sentence.fromAllLemmaTagsArray(readLine(parseLine)));
	}

	@Override
	public String getBestFrame(int[] tokenIndices, Sentence sentence) {
		final Set<String> candidateFrames = getCandidateFrames(tokenIndices, sentence, graph);
		return pickBestFrame(candidateFrames, sentence, tokenIndices);
	}

}
