/*******************************************************************************
 * Copyright (c) 2012 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * CommandLineOptions.java is part of SEMAFOR 2.1.
 * 
 * SEMAFOR 2.1 is free software: you can redistribute it and/or modify  it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * SEMAFOR 2.1 is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. 
 * 
 * You should have received a copy of the GNU General Public License along
 * with SEMAFOR 2.1.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package edu.cmu.cs.lti.ark.fn.utils;

import edu.cmu.cs.lti.ark.util.CommandLineOptions;

/**
 * A hodgepodge of command line options for various models used in training/testing the frame structure parser. 
 * Should probably be refactored into several separate classes
 * @author Nathan Schneider (nschneid)
 * @since 2009-09-25
 */
public final class FNModelOptions extends CommandLineOptions {
	public FNModelOptions(String[] args) {
		this(args, false);
	}
	public FNModelOptions(String[] args, boolean ignoreUnknownOptions) {
		super();
		init(args, ignoreUnknownOptions);
	}
	
	public StringOption alphabetFile = new StringOption("alphabetfile");
	public BoolOption train = new BoolOption("train");
	public BoolOption dev = new BoolOption("dev");
	public BoolOption test = new BoolOption("test");
	public StringOption trainParseFile = new StringOption("train-parsefile");
	public StringOption trainFrameFile = new StringOption("train-framefile");
	public StringOption trainFrameElementFile = new StringOption("train-fefile");
	public StringOption reg = new StringOption("regularization");
	public DoubleOption lambda = new DoubleOption("lambda");
	public StringOption frameNetMapFile = new StringOption("framenet-mapfile");
	public ExistingPathOption wnConfigFile = new ExistingPathOption("wordnet-configfile");
	public ExistingPathOption stopWordsFile = new ExistingPathOption("stopwords-file");
	public StringOption testFrameFile = new StringOption("test-framefile");
	public StringOption testParseFile = new StringOption("test-parsefile");
	public StringOption modelFile = new StringOption("model");
	public StringOption warmStartModelFile = new StringOption("warm-start-model");
	public IntOption saveEveryKBatches = new IntOption("save-every-k-batches");
	public IntOption numModelsToSave = new IntOption("num-models-to-save");
	public IntOption memory = new IntOption("memory");
	public IntOption startIndex = new IntOption("startindex");
	public IntOption endIndex = new IntOption("endindex");
	public ExistingPathOption testTokenizedFile = new ExistingPathOption("testtokenizedfile");
	public StringOption allRelatedWordsFile = new StringOption("allrelatedwordsfile");
	public StringOption wnRelatedWordsForWordsFile = new StringOption("wnrelatedwordsforwordsfile");
	public StringOption wnMapFile = new StringOption("wnmapfile");
	public StringOption hvCorrespondenceFile = new StringOption("hvcorrespondencefile");
	public StringOption fnIdReqDataFile = new StringOption("fnidreqdatafile");
	public ExistingPathOption idParamFile = new ExistingPathOption("idmodelfile");
	public ExistingPathOption luXmlDir = new ExistingPathOption("luxmldir");
	public NewFilePathOption frameElementsOutputFile = new NewFilePathOption("frameelementsoutputfile");
	public NewFilePathOption logOutputFile = new NewFilePathOption("logoutputfile");
	public IntOption minimumCount = new IntOption("minimum-count");
	public IntOption numThreads = new IntOption("numthreads");
	public IntOption batchSize = new PositiveIntOption("batch-size");
	public BoolOption usePartialCredit = new BoolOption("use-partial-credit");
	public DoubleOption costMultiple = new DoubleOption("cost-multiple");
	public ExistingPathOption inputFile = new ExistingPathOption("input-file");
	public NewFilePathOption outputFile = new NewFilePathOption("output-file");

	public StringOption idFeatureExtractorType = new StringOption("id-feature-extractor-type");
	public StringOption eventsFile = new StringOption("eventsfile");
	public StringOption spansFile = new StringOption("spansfile");
	public StringOption frameFeaturesCacheFile = new StringOption("localfeaturescache");
	public StringOption restartFile = new StringOption("restartfile");
	public StringOption lemmaCacheFile = new StringOption("lemmacachefile");
	
	public StringOption revisedMapFile = new StringOption("revisedmapfile");

	public StringOption useGraph = new StringOption("useGraph");
	public IntOption port = new IntOption("port");
	public StringOption modelDirectory = new StringOption("model-dir");
	public PositiveIntOption kBestOutput = new PositiveIntOption("k-best-output");
}
