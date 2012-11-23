/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * DecodingMainArgs.java is part of SEMAFOR 2.0.
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

import java.util.ArrayList;
import java.util.Date;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;


public class DecodingMainArgs
{
	public static void main(String[] args)
	{
		Decoding bpd = new Decoding();
		String modelFile = args[0];
		String alphabetFile = args[1];
		String eventsFile = args[2];
		String spanFile = args[3];
		String predictionFile = args[4];
		String frFile = args[5];
		String overlap = args[6];
		LocalFeatureReading lfr = new LocalFeatureReading(eventsFile, spanFile, frFile);
		try
		{
			lfr.readLocalFeatures();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		ArrayList<FrameFeatures> list = lfr.getMFrameFeaturesList();
		bpd.init(modelFile, 
				alphabetFile, 
				predictionFile,
				list,
				ParsePreparation.readSentencesFromFile(frFile)
				);
		bpd.decodeAll(overlap, 0);
	}		
}
