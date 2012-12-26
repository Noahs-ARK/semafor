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
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.utils.BitOps;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.FileUtil;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static org.apache.commons.io.IOUtils.closeQuietly;

public class CreateAlphabet {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		FEFileName.feFilename = args[0];
		FEFileName.tagFilename =  args[1];
		FEFileName.eventFilename =  args[2];
		FEFileName.alphafilename = args[3];
		FEFileName.spanfilename = args[4];		
		FEFileName.feDictFilename = args[9];
		
		boolean genAlpha = Boolean.parseBoolean(args[5]);
		if(genAlpha) System.out.println("Generating alphabet too...");
		FEFileName.KBestParse = new Integer(args[6]);
		FEFileName.KBestParseDirectory = args[7];
		run(genAlpha, null, null, null);
	}	
	
	// Used during testing with minimal IO
	public static void setDataFileNames(String alphafilename,
								   		String fedictFilename,
								   		String eventsFile,
								   		String spansFile) throws FEDict.LoadingException, FileNotFoundException {
		FEFileName.alphafilename = alphafilename;
		FEFileName.feDictFilename = fedictFilename;
		FEFileName.spanfilename = spansFile;
		FEFileName.eventFilename = eventsFile;
		DataPrep.loadFeatureIndex(FEFileName.alphafilename);
		DataPrep.frameElementsForFrame = new FEDict(FEFileName.feDictFilename);
		DataPrep.genAlpha = false;
	}

	public static void run(boolean doGenerateAlphabet,
						   List<String> tagLines,
						   List<String> frameElementLines,
						   WordNetRelations lwnr) throws IOException {
		List<int[][][]> dataPoints = getDataPoints(tagLines, frameElementLines, lwnr);
		long time = System.currentTimeMillis();
		System.err.println("Reading alphabet...");
		if(doGenerateAlphabet){
			DataPrep.featureIndex = new HashMap<String,Integer>();
		}
		if(DataPrep.featureIndex == null){
			DataPrep.loadFeatureIndex(FEFileName.alphafilename);
			System.out.println("Read alphabet in "+(System.currentTimeMillis()-time) + " millis.");
		}
		DataPrep.genAlpha = doGenerateAlphabet;
		time = System.currentTimeMillis();
		writeEvents(dataPoints, FEFileName.eventFilename);
		System.err.println("Wrote events in " + (System.currentTimeMillis() - time) + " millis.");
		if(doGenerateAlphabet){
			DataPrep.writeFeatureIndex(FEFileName.alphafilename);
		}
	}

	private static void writeEvents(List<int[][][]> dataPoints, String eventFilename) {
		BufferedOutputStream eventOutputStream = new BufferedOutputStream(FileUtil.openOutFile(eventFilename));
		try  {
			int fCount = 0;
			for(int[][][] dataPoint : dataPoints) {
				System.err.print(".");
				if(fCount % 100 == 0){
					System.err.println(fCount);
				}
				for(int i=0; i<dataPoint.length; i++){
					for(int j=0; j<dataPoint[i].length; j++){
						for(int k=0; k<dataPoint[i][j].length; k++){
							BitOps.writeInt(dataPoint[i][j][k], eventOutputStream);
						}
						BitOps.writeInt(-1, eventOutputStream);
					}
					BitOps.writeInt(-1, eventOutputStream);
				}
				fCount++;
			}
			BitOps.writeInt(-1, eventOutputStream);
		} finally {
			closeQuietly(eventOutputStream);
		}
	}

	public static List<int[][][]> getDataPoints(List<String> tagLines,
												List<String> frameElementLines,
												WordNetRelations lwnr) throws IOException {
		DataPrep dataPrep = new DataPrep(tagLines, frameElementLines, lwnr);
		List<int[][][]> dataPoints = Lists.newArrayList();
		while(dataPrep.hasNext()){
			dataPoints.add(dataPrep.getNextTrainData());
		}
		return dataPoints;
	}

	public static int[][][] getDataPoints(Sentence sentence,
										  String frameElements,
										  WordNetRelations lwnr) throws IOException {
		DataPrep dataPrep = new DataPrep(sentence, frameElements, lwnr);
		List<int[][][]> dataPoints = Lists.newArrayList();
		while(dataPrep.hasNext()){
			dataPoints.add(dataPrep.getNextTrainData());
		}
		return dataPoints.get(0);
	}
}
