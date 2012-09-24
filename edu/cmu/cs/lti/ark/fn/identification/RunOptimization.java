/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * RunOptimization.java is part of SEMAFOR 2.0.
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.util.ExternalCommands;


public class RunOptimization
{
	
	public static void main(String[] args)
	{
		FNModelOptions opts = new FNModelOptions(args);
		removeOutputAndIntermediateDirectories(opts);
		copyOverDataResources(opts);
		OptimizeMapReduce omr = new OptimizeMapReduce(opts);
		omr.optimize();
	}
	
	public static void removeOutputAndIntermediateDirectories(FNModelOptions opts)
	{
		System.out.println("Deleting the output and intermediate directories");
		String HADOOP_HOME=opts.m45HadoopHome.get();
		String outputDirectory=opts.m45OutputDir.get();
		ExternalCommands.runExternalCommand(HADOOP_HOME+"/bin/hadoop dfs -rmr "+outputDirectory);
		System.out.println("\n\n");
	}
	
	/**
	 * Creates an initial parameter file in HDFS from the alphabet file, and then copies it to a local temp directory.
	 * @param opts
	 */
	public static void copyOverDataResources(FNModelOptions opts)
	{
		String HADOOP_HOME=opts.m45HadoopHome.get();
		String intermediateDirectory=opts.m45IntermediateDir.get();
		String tmpDir=opts.tempDir.get();
		String alphabetFile = opts.alphabetFile.get();
		String localInitParamFile = convertAlphabetFileToInitParamFile(alphabetFile, "initParamFile.txt");
		
		String paramFileInHDFS = intermediateDirectory+"/initParamFile.txt";
		System.out.println("Copying over inital parameter file to HDFS");
		ExternalCommands.runExternalCommand(HADOOP_HOME+"/bin/hadoop dfs -put "+localInitParamFile+" "+paramFileInHDFS);
		
		String localInitParamFileInTempDir=tmpDir+"/initParamFile.txt";
		File f = new File(localInitParamFileInTempDir);
		f.delete();
		String command = HADOOP_HOME+"/bin/hadoop dfs -get "+paramFileInHDFS+" "+localInitParamFileInTempDir;
    	ExternalCommands.runExternalCommand(command);
		
		System.out.println("Done...\n");		
	}
	
	/**
	 * Copies the alphabet file, replacing all values with 0.0 (initial parameters).
	 */
	private static String convertAlphabetFileToInitParamFile(String alphabetFilePath, String initParamFileName) {
		try {
			final BufferedReader in = new BufferedReader(new FileReader(alphabetFilePath));
			
			String initParamFilePath = new File(alphabetFilePath).getParentFile().getPath() + File.separator + initParamFileName;
			System.out.println("Writing initial parameter file to " + initParamFilePath);
			BufferedWriter out = new BufferedWriter(new FileWriter(initParamFilePath));
			
			String line = in.readLine();	// FEATURE_NAME\tCOUNT, true
			while (line!=null) {
				if (!line.isEmpty()) {
					String beforeCount = line.substring(0,line.lastIndexOf('\t')+1);
					String afterCount = line.substring(line.lastIndexOf(","));
					String modifiedLine = beforeCount + "0.0" + afterCount;
					out.write(modifiedLine + "\n");
				}
				else
					out.write("\n");
				line = in.readLine();
			}
			
			out.close();
			in.close();
			
			return initParamFilePath;
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}
}
