/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * IdentificationOptions.java is part of SEMAFOR 2.0.
 * 
 * SEMAFOR 2.0 is free software: you can redistribute it and/or modify  it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * SEMAFOR 2.0 is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. 
 * 
 * You should have received a copy of the GNU General Public License along
 * with SEMAFOR 2.0.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package edu.cmu.cs.lti.ark.fn.identification;


/**
 * Processes command-line options and stores a variety of configuration parameters. 
 * Options should be formatted as argname:value (or simply the argname if boolean).
 * See the code for details.
 * 
 * @author dipanjan
 * @deprecated Use FNModelOptions instead.
 * @see edu.cmu.cs.lti.ark.fn.utils.FNModelOptions
 *
 */
public final class IdentificationOptions
{
	public String frameNetFrameFile = null;
	public String frameNetElementsMapFile = null;
	public String frameNetFrameElementFile = null;
	public String frameNetParseFile = null;
	public String frameLexiconFile = null;
	public String trainFrameFile = null;
	public String trainFrameElementFile = null;
	public String trainElementsMapFile = null;
	public String trainParseFile = null;
	public String reg = "reg";
	public double lambda = 0.0;
	public String frameNetMapFile = null;
	public String wnConfigFile = null;
	public String stopWordsFile = null;
	public String spanFilterFile = null;
	
	/**
	 * Partially-annotated sentences from the FrameNet lexicon.
	 */
	public boolean originalFrameNet = false;
	/**
	 * Semeval training data (full-text annotation)
	 */
	public boolean train = false;
	public boolean dev = false;
	public boolean test = false;
	public String testFrameFile = null;
	public String testParseFile = null;
	public String devFrameFile = null;
	public String devParseFile = null;
	public String modelFile = null;
	public String m45FrameMap = null;
	public String m45AllParsesFile = null;
	public String m45TrainParseFile = null;
	public String m45TrainElementsMapFile = null;
	public String m45FrameLexiconFile = null;
	public String m45SpanFilterFile = null;
	public String m45InputDir=null;
	public String m45OutputDir = null;
	public String m45IntermediateDir = null;
	public String m45ParameterFile = null;
	public String m45HadoopHome = null;
	public int numMapTasks = 20;
	public int numReduceTasks = 1;
	public int memory = 3000;
	public String m45WordNetZipFile = null;
	public String projectRoot = null;
	public String outputParamFile = null;
	public String hodDir = null;
	public String alphabetFile = null;
	public String tempDir = null;
	public int dataSize = 0;
	public String m45TestParsesFile = null;
	public int maxMapTasksPerNode = 2;
	public String m45WordNetRelationsCacheFile = null;
	public int featureCountCutoff = 0;
	public String m45SegmentationParameterFile = null;
	public String m45IdentificationParameterFile = null;
	
	
	/*
	 * Twelfth arg: local disk location of possible parameters file
	 * Fifteenth arg: data size
	 * Sixteenth arg: temp dir
	 */
	
	/**
	 * Given a list of command-line options, loads the values for various member variables. 
	 * Also prints the options to standard output.
	 * Options should be formatted as argname:value (or simply the argname if boolean).
	 */
	public IdentificationOptions(String[] args)
	{
		
		
		for(int i = 0; i < args.length; i ++)
		{
			System.out.println(args[i]);
			String[] pair = args[i].split(":");
			
			if(pair[0].equals("seg-parameterfile"))
			{
				m45SegmentationParameterFile = pair[1];
			}
			if(pair[0].equals("id-parameterfile"))
			{
				m45IdentificationParameterFile = pair[1];
			}			
			if(pair[0].equals("feature-cutoff"))
			{
				featureCountCutoff = new Integer(pair[1]);
			}
			if(pair[0].equals("m45wnrelationscachefile"))
			{
				m45WordNetRelationsCacheFile = pair[1].trim();
			}
			if(pair[0].equals("maxmaptaskspernode"))
			{
				maxMapTasksPerNode = new Integer(pair[1].trim());
			}			
			if(pair[0].equals("m45testparsesfile"))
			{
				m45TestParsesFile = pair[1].trim();
			}
			if(pair[0].equals("m45intermediatedir"))
			{
				m45IntermediateDir = pair[1].trim();
			}
			if(pair[0].equals("m45hadoophome"))
			{
				m45HadoopHome = pair[1].trim();
			}
			if(pair[0].equals("projectroot"))
			{
				projectRoot = pair[1].trim();
			}
			if(pair[0].equals("outputparamfile"))
			{
				outputParamFile = pair[1].trim();
			}
			if(pair[0].equals("hoddir"))
			{
				hodDir = pair[1].trim();
			}
			if(pair[0].equals("alphabetfile"))
			{
				alphabetFile = pair[1].trim();
			}
			if(pair[0].equals("tempdir"))
			{
				tempDir = pair[1].trim();
			}			
			if(pair[0].equals("datasize"))
			{
				dataSize = new Integer(pair[1].trim());
			}
			if (pair[0].equals("original-fn"))
			{
				originalFrameNet = true;
			}
			if(pair[0].equals("train"))
			{
				train = true;
			}
			if(pair[0].equals("dev"))
			{
				dev = true;
			}
			if(pair[0].equals("test"))
			{
				test = true;
			}
			if(pair[0].equals("framenet-framefile"))
			{
				frameNetFrameFile = pair[1].trim();
			}
			if(pair[0].equals("lexiconfile"))
			{
				frameLexiconFile = pair[1].trim();
			}
			if(pair[0].equals("framenet-femapfile"))
			{
				frameNetElementsMapFile = pair[1].trim();
			}
			if(pair[0].equals("framenet-parsefile"))
			{
				frameNetParseFile = pair[1].trim();
			}
			if(pair[0].equals("framenet-fefile"))
			{
				frameNetFrameElementFile = pair[1].trim();
			}
			if(pair[0].equals("train-parsefile"))
			{
				trainParseFile = pair[1].trim();
			}
			if(pair[0].equals("train-framefile"))
			{
				trainFrameFile = pair[1].trim();
			}
			if(pair[0].equals("train-fefile"))
			{
				trainFrameElementFile = pair[1].trim();
			}
			if(pair[0].equals("train-femapfile"))
			{
				trainElementsMapFile = pair[1].trim();
			}
			if(pair[0].equals("regularization"))
			{
				reg = pair[1].trim();
			}
			if(pair[0].equals("lambda"))
			{
				lambda = new Double(pair[1].trim());
			}
			if(pair[0].equals("framenet-mapfile"))
			{
				frameNetMapFile = pair[1].trim();
			}
			if(pair[0].equals("wordnet-configfile"))
			{
				wnConfigFile = pair[1].trim();
			}
			if(pair[0].equals("stopwords-file"))
			{
				stopWordsFile = pair[1].trim();
			}
			if(pair[0].equals("dev-parsefile"))
			{
				devParseFile = pair[1].trim();
			}
			if(pair[0].equals("dev-framefile"))
			{
				devFrameFile = pair[1].trim();
			}
			if(pair[0].equals("test-framefile"))
			{
				testFrameFile = pair[1].trim();
			}
			if(pair[0].equals("test-parsefile"))
			{
				testParseFile = pair[1].trim();
			}
			if(pair[0].equals("model"))
			{
				modelFile = pair[1].trim();	
			}
			if(pair[0].equals("m45inputdir"))
			{
				m45InputDir = pair[1].trim();
			}
			if(pair[0].equals("m45outputdir"))
			{
				m45OutputDir = pair[1].trim();
			}
			if(pair[0].equals("m45framemap"))
			{
				m45FrameMap = pair[1].trim();
			}
			if(pair[0].equals("m45train-femapfile"))
			{
				m45TrainElementsMapFile = pair[1].trim();
			}
			if(pair[0].equals("m45lexiconfile"))
			{
				m45FrameLexiconFile = pair[1].trim();
			}
			if(pair[0].equals("m45allparses"))
			{
				m45AllParsesFile = pair[1].trim();
			}
			if(pair[0].equals("m45train-parsefile"))
			{
				m45TrainParseFile = pair[1].trim();
			}
			if (pair[0].equals("m45spanfilter"))	// for frame element classification
			{
				m45SpanFilterFile = pair[1].trim();
			}
			if (pair[0].equals("spanfilter"))	// for frame element classification
			{
				spanFilterFile = pair[1].trim();
			}
			if(pair[0].equals("memory"))
			{
				memory = new Integer(pair[1].trim());
			}
			if(pair[0].equals("nummaptasks"))
			{
				numMapTasks = new Integer(pair[1].trim());
			}
			if(pair[0].equals("numreducetasks"))
			{
				numReduceTasks = new Integer(pair[1].trim());
			}
			if(pair[0].equals("m45parameterfile"))
			{
				m45ParameterFile = pair[1].trim();
			}
			if(pair[0].equals("m45wordnetzipfile"))
			{
				m45WordNetZipFile = pair[1].trim();
			}
		}	
		
	}	
}
