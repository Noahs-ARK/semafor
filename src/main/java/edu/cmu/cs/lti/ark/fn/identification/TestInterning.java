/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * TestInterning.java is part of SEMAFOR 2.0.
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
import java.io.FileReader;

import edu.cmu.cs.lti.ark.util.optimization.LDouble;


public class TestInterning
{
	public static void main(String[] args) throws Exception
	{
		String file1 = "/mal2/dipanjan/experiments/FramenetParsing/FrameStructureExtraction/scripts/alphabet-cache-1-0";
		String file2 = "/mal2/dipanjan/experiments/FramenetParsing/FrameStructureExtraction/scripts/alphabet-cache-1-1";
		
		BufferedReader b1 = new BufferedReader(new FileReader(file1));
		BufferedReader b2 = new BufferedReader(new FileReader(file2));
		
		String line1 = null;
		String line2 = null;
		
		int count1 = 0;
		
		int countThresh = 0;
		
		while((line1=b1.readLine())!=null)
		{
			line1=line1.trim();
			line2=b2.readLine().trim();
			String[] arr1 = line1.split("\t");
			String[] arr2 = line2.split("\t");
			
			String[] ldouble1 = arr1[1].trim().split(",");
			String[] ldouble2 = arr2[1].trim().split(",");
			
			boolean bl1;
			boolean bl2;
			
			if(ldouble1[1].trim().equals("true"))
			{
				bl1 = true;
			}
			else
			{
				bl1 = false;
			}
			
			if(ldouble2[1].trim().equals("true"))
			{
				bl2 = true;
			}
			else
			{
				bl2 = false;
			}		
			
			double d1 = new Double(ldouble1[0].trim());
			double d2 = new Double(ldouble2[0].trim());
			
			double val1=(new LDouble(d1,bl1)).exponentiate();
			double val2=(new LDouble(d2,bl2)).exponentiate();
			
			if(Math.abs(val1-val2)>0.000001)
			{
				System.out.println("Problem: "+val1+"\t"+val2);
				System.exit(0);
			}		
//			if(threshold(val1)&&!threshold(val2))
//			{
//				System.out.println("\n"+val1+"\t"+val2);
//			}
//			if(!threshold(val1)&&threshold(val2))
//			{
//				System.out.println("\n"+val1+"\t"+val2);
//			}			
//			
			
			if(val1>1.0)
			{
				countThresh++;
			}
			if(count1%1000==0&&count1!=0)
			{
				System.out.print(". ");
			}
			if(count1%10000==0)
				System.out.println();
			count1++;
		}
		System.out.println("\n"+count1);
		System.out.println("Count more than threshold:"+countThresh);
		b1.close();
		b2.close();	
	}
	
	
	public static boolean threshold(double val)
	{
		if(val>0.9999)
		{
			System.out.println("\n"+val);
			return true;
		}
		else
		{
			return false;
		}
	}
}
