package edu.cmu.cs.lti.ark.fn.identification;

import com.google.common.primitives.Ints;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.parsing.Semafor;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectDoubleHashMap;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static edu.cmu.cs.lti.ark.util.SerializedObjects.readObject;

/**
 * @author sthomson@cs.cmu.edu
 */
public class GraphBasedFrameIdentifier extends FastFrameIdentifier {
	/* trained model files */
	public static final String GRAPH_FILENAME = "sparsegraph.gz";
	public static final String ID_MODEL_FILE = "idmodel.dat";
	final protected SmoothedGraph graph;

	public GraphBasedFrameIdentifier(TObjectDoubleHashMap<String> paramList,
									 String reg,
									 double l,
									 THashMap<String, THashSet<String>> frameMap,
									 THashMap<String, THashSet<String>> hvCorrespondenceMap,
									 Map<String, Set<String>> relatedWordsForWord,
									 Map<String, Map<String, Set<String>>> revisedRelationsMap,
									 Map<String, String> hvLemmas,
									 SmoothedGraph graph) {
		super(paramList, reg, l, frameMap, hvCorrespondenceMap);
		this.graph = graph;
	}

	public static GraphBasedFrameIdentifier getInstance(String modelDirectory) throws IOException, ClassNotFoundException {
		final String graphFilename = new File(modelDirectory, GRAPH_FILENAME).getAbsolutePath();
		final String idParamsFile = new File(modelDirectory, ID_MODEL_FILE).getAbsolutePath();
		final String requiredDataFilename = new File(modelDirectory, Semafor.REQUIRED_DATA_FILENAME).getAbsolutePath();
		System.err.println("Initializing frame identification model...");
		System.err.println("Reading model parameters...");
		final TObjectDoubleHashMap<String> paramList =
				FrameIdentificationRelease.parseParamFile(idParamsFile);
		System.err.println("Done reading model parameters.");
		System.err.println("Reading graph from: " + graphFilename + "...");
		final SmoothedGraph graph = readObject(graphFilename);
		System.err.println("Read graph successfully.");
		final RequiredDataForFrameIdentification r = readObject(requiredDataFilename);
		return new GraphBasedFrameIdentifier(
				paramList,
				"reg",
				0.0,
				r.getFrameMap(),
				r.getcMap(),
				r.getRelatedWordsForWord(),
				r.getRevisedRelMap(),
				r.getHvLemmaCache(),
				graph);
	}

	public String getBestFrame(Collection<Integer> indices, Sentence sentence) {
		return getBestFrame(Ints.toArray(indices), sentence, graph);
	}
}
