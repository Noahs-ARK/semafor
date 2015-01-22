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
import java.io.IOException;
import java.util.List;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Files.readLines;
import static edu.cmu.cs.lti.ark.fn.utils.BitOps.writeInt;
import static org.apache.commons.io.IOUtils.closeQuietly;

public class CreateAlphabet {
	public static void main(String[] args) throws IOException {
		final String feFilename = args[0];
		final String tagFilename =  args[1];
		final String eventFilename =  args[2];
		final String alphabetFilename = args[3];
		final String spanFilename = args[4];
		final boolean doGenerateAlphabet = Boolean.parseBoolean(args[5]);
		final int kBestParse = Integer.parseInt(args[6]);

		if(doGenerateAlphabet) System.out.println("Generating alphabet...");

		final List<String> feLines = readLines(new File(feFilename), UTF_8);
		final List<String> tagLines = readLines(new File(tagFilename), UTF_8);
		run(doGenerateAlphabet, tagLines, feLines, eventFilename, alphabetFilename, spanFilename, kBestParse);
	}

	private static void run(boolean doGenerateAlphabet,
							List<String> tagLines,
							List<String> frameElementLines,
							String eventFilename,
							String alphabetFilename,
							String spanFilename,
							int kBestParse) throws IOException {
		DataPrep.genAlpha = doGenerateAlphabet;
		if(doGenerateAlphabet){
			DataPrep.featureIndex = Maps.newHashMap();
		} else if(DataPrep.featureIndex == null){
			System.err.println("Reading alphabet...");
			long time = System.currentTimeMillis();
			DataPrep.featureIndex = DataPrep.readFeatureIndex(new File(alphabetFilename));
			System.err.println("Read alphabet in "+(System.currentTimeMillis()-time) + " millis.");
		}
		final List<int[][][]> dataPoints = getDataPoints(tagLines, frameElementLines, spanFilename, kBestParse);
		final long time = System.currentTimeMillis();
		writeEvents(dataPoints, eventFilename);
		System.err.println("Wrote events in " + (System.currentTimeMillis() - time) + " millis.");
		if(doGenerateAlphabet){
			DataPrep.writeFeatureIndex(alphabetFilename);
		}
	}

	private static void writeEvents(List<int[][][]> dataPoints, String eventFilename) {
		BufferedOutputStream eventOutputStream = new BufferedOutputStream(FileUtil.openOutFile(eventFilename));
		try  {
			int fCount = 0;
			for(int[][][] dataPoint : dataPoints) {
				System.err.print(".");
				if(fCount % 100 == 0) System.err.println(fCount);
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

	private static List<int[][][]> getDataPoints(List<String> tagLines,
												 List<String> frameElementLines,
												 String spanFilename,
												 int kBestParse) throws IOException {
		final DataPrep dataPrep = new DataPrep(tagLines, frameElementLines, spanFilename, kBestParse);
		final List<int[][][]> dataPoints = Lists.newArrayList();
		while(dataPrep.hasNext()){
			dataPoints.add(dataPrep.getNextTrainData(spanFilename));
		}
		return dataPoints;
	}
}
