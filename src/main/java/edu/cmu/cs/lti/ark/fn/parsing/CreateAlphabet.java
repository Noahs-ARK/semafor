/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * CreateAlphabet.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.parsing;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.cmu.cs.lti.ark.util.FileUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Files.readLines;
import static edu.cmu.cs.lti.ark.fn.utils.BitOps.writeInt;
import static org.apache.commons.io.IOUtils.closeQuietly;

public class CreateAlphabet {
	public static void main(String[] args) throws IOException {
		FEFileName.feFilename = args[0];
		FEFileName.tagFilename =  args[1];
		FEFileName.eventFilename =  args[2];
		FEFileName.alphafilename = args[3];
		FEFileName.spanfilename = args[4];
		boolean genAlpha = Boolean.parseBoolean(args[5]);
		FEFileName.KBestParse = Integer.parseInt(args[6]);
		FEFileName.KBestParseDirectory = args[7];

		if(genAlpha) System.out.println("Generating alphabet too...");

		final List<String> feLines = readLines(new File(FEFileName.feFilename), UTF_8);
		final List<String> tagLines = readLines(new File(FEFileName.tagFilename), UTF_8);
		run(genAlpha, tagLines, feLines);
	}	
	
	// Used during testing with minimal IO
	public static void setDataFileNames(String alphafilename,
										String eventsFile,
										String spansFile) throws FileNotFoundException {
		FEFileName.alphafilename = alphafilename;
		FEFileName.spanfilename = spansFile;
		FEFileName.eventFilename = eventsFile;
		DataPrep.featureIndex = DataPrep.readFeatureIndex(new File(FEFileName.alphafilename));
		DataPrep.genAlpha = false;
	}

	public static void run(boolean doGenerateAlphabet,
						   List<String> tagLines,
						   List<String> frameElementLines) throws IOException {
		DataPrep.genAlpha = doGenerateAlphabet;
		if(doGenerateAlphabet){
			DataPrep.featureIndex = Maps.newHashMap();
		} else if(DataPrep.featureIndex == null){
			System.err.println("Reading alphabet...");
			long time = System.currentTimeMillis();
			DataPrep.featureIndex = DataPrep.readFeatureIndex(new File(FEFileName.alphafilename));
			System.err.println("Read alphabet in "+(System.currentTimeMillis()-time) + " millis.");
		}
		final List<int[][][]> dataPoints = getDataPoints(tagLines, frameElementLines);
		final long time = System.currentTimeMillis();
		writeEvents(dataPoints, FEFileName.eventFilename);
		System.err.println("Wrote events in " + (System.currentTimeMillis() - time) + " millis.");
		if(doGenerateAlphabet){
			DataPrep.writeFeatureIndex(FEFileName.alphafilename);
		}
	}

	public static void writeEvents(List<int[][][]> dataPoints, String eventFilename) {
		BufferedOutputStream eventOutputStream = new BufferedOutputStream(FileUtil.openOutFile(eventFilename));
		try  {
			int fCount = 0;
			for(int[][][] dataPoint : dataPoints) {
				System.err.print(".");
				if(fCount % 100 == 0){
					System.err.println(fCount);
				}
				for (int[][] aDataPoint : dataPoint) {
					for (int[] anADataPoint : aDataPoint) {
						for (int anAnADataPoint : anADataPoint) {
							writeInt(anAnADataPoint, eventOutputStream);
						}
						writeInt(-1, eventOutputStream);
					}
					writeInt(-1, eventOutputStream);
				}
				fCount++;
			}
			writeInt(-1, eventOutputStream);
		} finally {
			closeQuietly(eventOutputStream);
		}
	}

	public static List<int[][][]> getDataPoints(List<String> tagLines, List<String> frameElementLines)
			throws IOException {
		final DataPrep dataPrep = new DataPrep(tagLines, frameElementLines);
		final List<int[][][]> dataPoints = Lists.newArrayList();
		while(dataPrep.hasNext()){
			dataPoints.add(dataPrep.getNextTrainData());
		}
		return dataPoints;
	}
}
