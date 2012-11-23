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
 * Should probably be refactored into several separate classes, à la {@link edu.cmu.cs.lti.ark.fn.desaic.parsing.Driver.DriverOptions}
 * @author Nathan Schneider (nschneid)
 * @since 2009-09-25
 */
public final class FNModelOptions extends CommandLineOptions
{
	public static final double TOL = 0.00000001;
	
	public FNModelOptions(String[] args) {
		this(args,false);
	}
	public FNModelOptions(String[] args, boolean ignoreUnknownOptions) {
		super();
		init(args, ignoreUnknownOptions);
	}
	
	public StringOption m45SegmentationParameterFile = new StringOption("seg-parameterfile");
	public StringOption m45IdentificationParameterFile = new StringOption("id-parameterfile");
	public IntOption featureCountCutoff = new IntOption("feature-cutoff");
	public StringOption m45WordNetRelationsCacheFile = new StringOption("m45wnrelationscachefile");
	public IntOption maxMapTasksPerNode = new IntOption("maxmaptaskspernode");
	public StringOption m45TestParsesFile = new StringOption("m45testparsesfile");
	public StringOption m45IntermediateDir = new StringOption("m45intermediatedir");
	public StringOption m45HadoopHome = new StringOption("m45hadoophome");
	public StringOption projectRoot = new StringOption("projectroot");
	public StringOption outputParamFile = new StringOption("outputparamfile");
	public StringOption hodDir = new StringOption("hoddir");
	public StringOption alphabetFile = new StringOption("alphabetfile");
	public StringOption tempDir = new StringOption("tempdir");
	public IntOption dataSize = new IntOption("datasize");
	public BoolOption originalFrameNet = new BoolOption("original-fn");	// Exemplar sentences from the FrameNet lexicon (partially-annotated)
	public BoolOption train = new BoolOption("train");
	public BoolOption dev = new BoolOption("dev");
	public BoolOption test = new BoolOption("test");
	public StringOption frameNetFrameFile = new StringOption("framenet-framefile");
	public StringOption frameLexiconFile = new StringOption("lexiconfile");
	public StringOption frameNetElementsMapFile = new StringOption("framenet-femapfile");
	public StringOption frameNetParseFile = new StringOption("framenet-parsefile");
	public StringOption frameNetFrameElementFile = new StringOption("framenet-fefile");
	public StringOption trainParseFile = new StringOption("train-parsefile");
	public StringOption trainFrameFile = new StringOption("train-framefile");
	public StringOption trainTokenNumsFile = new StringOption("train-tokennumsfile");
	public StringOption trainFrameElementFile = new StringOption("train-fefile");
	public StringOption trainElementsMapFile = new StringOption("train-femapfile");
	public StringOption reg = new StringOption("regularization");
	public DoubleOption lambda = new DoubleOption("lambda");
	public StringOption frameNetMapFile = new StringOption("framenet-mapfile");
	public ExistingPathOption wnConfigFile = new ExistingPathOption("wordnet-configfile");
	public ExistingPathOption stopWordsFile = new ExistingPathOption("stopwords-file");
	public StringOption devParseFile = new StringOption("dev-parsefile");
	public StringOption devFrameFile = new StringOption("dev-framefile");
	public StringOption testFrameFile = new StringOption("test-framefile");
	public StringOption testParseFile = new StringOption("test-parsefile");
	public StringOption modelFile = new StringOption("model");
	public StringOption m45InputDir = new StringOption("m45inputdir");
	public StringOption m45OutputDir = new StringOption("m45outputdir");
	public StringOption m45FrameMap = new StringOption("m45framemap");
	public StringOption m45TrainElementsMapFile = new StringOption("m45train-femapfile");
	public StringOption m45FrameLexiconFile = new StringOption("m45lexiconfile");
	public StringOption m45AllParsesFile = new StringOption("m45allparses");
	public StringOption m45TrainParseFile = new StringOption("m45train-parsefile");
	public StringOption m45SpanFilterFile = new StringOption("m45spanfilter");	// for parsing (frame element classification)
	public StringOption spanFilterFile = new StringOption("spanfilter");	// for parsing (frame element classification)
	public IntOption memory = new IntOption("memory");
	public IntOption numMapTasks = new IntOption("nummaptasks");
	public IntOption numReduceTasks = new IntOption("numreducetasks");
	public StringOption m45ParameterFile = new StringOption("m45parameterfile");
	public StringOption m45WordNetZipFile = new StringOption("m45wordnetzipfile");
	public StringOption m45HierarchyFile = new StringOption("m45hierarchyfile");
	public StringOption m45IsHierarchicalModel = new StringOption("m45ishierarchical");
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
	public StringOption printFNIDConfidence = new StringOption("printfnidconfidence");
	public IntOption numThreads = new IntOption("numthreads");
	
	public StringOption eventsFile = new StringOption("eventsfile");
	public StringOption spansFile = new StringOption("spansfile");
	public StringOption lexiconDir = new StringOption("lexicondir");
	public IntOption bpItrs = new IntOption("bpnumitrs");
	public StringOption frameFeaturesCacheFile = new StringOption("localfeaturescache");
	public IntOption batchSize = new IntOption("batchsize");
	public IntOption totalPasses = new IntOption("totalpasses");
	public StringOption binaryOverlapConstraint = new StringOption("binaryoverlapfactor");
	public StringOption hiddenVars = new StringOption("hiddenvars");
	public StringOption clusterFeats = new StringOption("clusterfeats");
	public StringOption synClusterMap = new StringOption("synclustermap");
	public StringOption fileSize = new StringOption("filesize");
	public StringOption restartFile = new StringOption("restartfile");
	public IntOption modelSize = new IntOption("modelsize");
	public StringOption lemmaCacheFile = new StringOption("lemmacachefile");
	
	public StringOption storeInMemory = new StringOption("storeinmemory");
	public IntOption clusterK = new IntOption("clusterk");
	public StringOption revisedMapFile = new StringOption("revisedmapfile");
	public StringOption useRelaxedSegmentation = new StringOption("userelaxed");
	
	public StringOption outputPredicatesFile = new StringOption("predicatesfile");
	public StringOption useGraph = new StringOption("useGraph");
	public StringOption mstServerMode = new StringOption("mstmode");	
	public StringOption mstServerName = new StringOption("mstserver");
	public IntOption mstServerPort = new IntOption("mstport");
	public StringOption goldSegFile = new StringOption("goldsegfile");
	public StringOption posTaggedFile = new StringOption("posfile");
	public StringOption outAllLemmaTagsFile = new StringOption("alllemmatagsfile");
	public StringOption requiresMapFile = new StringOption("requiresmap");
	public StringOption excludesMapFile = new StringOption("excludesmap");
	public StringOption decodingType = new StringOption("decoding");
}
