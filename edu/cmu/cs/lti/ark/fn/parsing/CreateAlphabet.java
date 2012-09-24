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

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

import edu.cmu.cs.lti.ark.fn.clusters.ScrapTest;
import edu.cmu.cs.lti.ark.fn.utils.BitOps;
import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.FileUtil;

public class CreateAlphabet {

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{	 
		FEFileName.feFilename = args[0];
		FEFileName.tagFilename =  args[1];
		FEFileName.eventFilename =  args[2];
		FEFileName.alphafilename = args[3];
		FEFileName.spanfilename = args[4];		
		FEFileName.fedictFilename1 = args[9];
		
		boolean genAlpha=Boolean.parseBoolean(args[5]);
		if(genAlpha)
			System.out.println("Generating alphabet too...");
		FEFileName.useUnlabeledSpans=Boolean.parseBoolean(args[6]);
		if(FEFileName.useUnlabeledSpans)
		{
			System.out.println("Using spans gathered from unlabeled data too...");
			FEFileName.unlabeledSpans=ScrapTest.getSpansWithHeads(FEFileName.unlabeledMatchedSpansFile);
		}
		FEFileName.KBestParse = new Integer(args[7]);
		FEFileName.KBestParseDirectory = args[8];
		run(genAlpha, null, null, null);
	}	
	
	// Used during testing with minimal IO
	public static void setDataFileNames(String alphafilename,
								   		String fedictFilename,
								   		String eventsFile,
								   		String spansFile) {
		FEFileName.alphafilename = alphafilename;
		FEFileName.fedictFilename1 = fedictFilename;
		FEFileName.spanfilename = spansFile;
		FEFileName.eventFilename = eventsFile;
		DataPrep.readFeatureIndex(FEFileName.alphafilename);
		DataPrep.fedict = new FEDict(FEFileName.fedictFilename1);
		DataPrep.genAlpha = false;
	}

	public static void run(boolean genAlpha, 
							ArrayList<String> tL, 	
							ArrayList<String> fL,
							WordNetRelations lwnr) {
		DataPrep dprep=new DataPrep(tL, fL, lwnr);
		long time=System.currentTimeMillis();
		System.out.println("Reading alphabet...");
		if(genAlpha){
			DataPrep.featIndex=new HashMap<String,Integer>();
		}
		if(DataPrep.featIndex==null){
			DataPrep.readFeatureIndex(FEFileName.alphafilename);
			System.out.println("Finished Reading alphabet..."+(System.currentTimeMillis()-time));
		}
		DataPrep.genAlpha=genAlpha;
		BufferedOutputStream bos=new BufferedOutputStream(FileUtil.openOutFile(FEFileName.eventFilename));
		int fCount=0;
		time=System.currentTimeMillis();
		while( dprep.hasNext()){
			int [][][] datapoint=dprep.getTrainData();
			System.out.print(".");
			if(fCount%100==0){
				System.out.println(fCount);
			}
			for(int i=0;i<datapoint.length;i++){
				for(int j=0;j<datapoint[i].length;j++){
					for(int k=0;k<datapoint[i][j].length;k++){
						BitOps.writeInt(datapoint[i][j][k],bos);
					}
					BitOps.writeInt(-1,bos);
				}
				BitOps.writeInt(-1,bos);
			}
			fCount++;
		}
		BitOps.writeInt(-1,bos);
		try{
			bos.close();
		}catch(IOException ioe){
			System.out.println(ioe.getMessage());
		}
		System.out.println(System.currentTimeMillis()-time);
		if(genAlpha){
			DataPrep.writeFeatureIndex(FEFileName.alphafilename);
		}
	}	
}
