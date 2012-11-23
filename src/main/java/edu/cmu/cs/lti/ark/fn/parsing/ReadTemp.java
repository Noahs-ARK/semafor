/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * ReadTemp.java is part of SEMAFOR 2.0.
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
import java.util.*;
import java.io.*;

import edu.cmu.cs.lti.ark.util.FileUtil;
public class ReadTemp {
	//test
	//public static int offSet[]={0,67,106};
	public static ArrayList<String []>tagLines;
	public static void main(String[] args) {
		tagLines=readTagLines(FEFileName.tagFilename);
		for(int i=0;i<FEFileName.offSet.length;i++){
			writeFEFile(FEFileName.predictionFilename+FEFileName.offSet[i],
					FEFileName.spanfilename+FEFileName.offSet[i],
					FEFileName.feFilename+FEFileName.offSet[i],
					FEFileName.outfefilename+FEFileName.offSet[i]);
					
		}
	}	
	
	public static void run(int index){	
		tagLines=readTagLines(FEFileName.tagFilename);
		PrintStream ps=FileUtil.openOutFile(FEFileName.feFilename);
		Scanner localsc=FileUtil.openInFile(FEFileName.tempFilename);
		while(localsc.hasNextLine()){
			String toks[]=localsc.nextLine().trim().split("\t");
			int sentNum=Integer.parseInt(toks[2])+index;
			String tags[]=tagLines.get(sentNum);
			ps.print(1);
			String tokenNumToks[]=toks[1].trim().split("_");
			StringBuilder sb=new StringBuilder();
			int tokenNums[]=new int[tokenNumToks.length];
			for(int i=0;i<tokenNums.length;i++){
				tokenNums[i]=Integer.parseInt(tokenNumToks[i]);
				sb.append(tags[1+tokenNums[i]]+" ");
			}
			sb.deleteCharAt(sb.length()-1);
			
			int sentLen=Integer.parseInt(tags[0]);
			
			//frame name
			ps.print("\t" + toks[0]);
			//dummy lexical unit
			String LU=tags[1+tokenNums[0]]+"."+tags[1+sentLen+tokenNums[0]].toLowerCase().charAt(0);
			ps.print("\t" + LU);
			//tok nums
			ps.print("\t"+toks[1]);
			//words in sentence
			ps.print("\t"+sb.toString());
			//sent num
			ps.print("\t"+sentNum+"\n");
		}
		
	}
	public static ArrayList<String[]> readTagLines(String tagFilename){
		ArrayList<String[] >lines=new ArrayList<String[] > ();
		Scanner localsc=FileUtil.openInFile(tagFilename);
		while(localsc.hasNextLine()){
			String toks[]=localsc.nextLine().trim().split("\t");
			lines.add(toks);
		}
		localsc.close();
		return lines;
	}
	public static void writeFEFile(String predictFilename,
			String spanFilename,String feFilename,String outFilename){
		Scanner psc, spansc,fsc;
		psc=FileUtil.openInFile(predictFilename);
		spansc=FileUtil.openInFile(spanFilename);
		fsc=FileUtil.openInFile(feFilename);
		PrintStream localps=FileUtil.openOutFile(outFilename);
		String spantoks[]=spansc.nextLine().split("\t");
		int index=0;
		while(fsc.hasNextLine()){
			String fline=fsc.nextLine().trim();
			String ftoks[]=fline.split("\t");
			if(ftoks.length<3)break;
		//	String frameName=ftoks[1];
		//	String tokenNums[]=ftoks[3].split("_");
			int feindex=Integer.parseInt(spantoks[5]);
			ArrayList<FrameElement> felist=new ArrayList<FrameElement>();
			while(feindex==index){
				
				String span[]=spansc.nextLine().split("\t");
				ArrayList <Integer>canstart,canend;
				canstart=new ArrayList<Integer>();
				canend=new ArrayList<Integer>();
				while(span.length>1){
					canstart.add(Integer.parseInt(span[0]));
					canend.add(Integer.parseInt(span[1]));
					span=spansc.nextLine().split("\t");
				}
				int prediction=psc.nextInt();
				psc.nextInt();
				//predicted start and end index of frame element
				int pstart,pend;
				pstart=canstart.get(prediction);
				pend=canend.get(prediction);
				//if not null frame element
				if(pstart!=-1 && pend!=-1){
					felist.add(new FrameElement(spantoks[1],pstart,pend));
				}
				if(!spansc.hasNextLine())break;
				spantoks=spansc.nextLine().split("\t");
				feindex=Integer.parseInt(spantoks[5]);
			}
			localps.print("0\t"+(felist.size()+1));
			for(int i=1;i<ftoks.length;i++){
				localps.print("\t"+ftoks[i]);
			}
			for(FrameElement fe:felist){
				localps.print("\t"+fe);
			}
			localps.println();
			index++;
		}
		localps.close();
		psc.close();
		spansc.close();
		fsc.close();
	}
	public static boolean isSameFrame(String [] spantoks,String [] tokenNums,String frameName){
		return spantoks[2].equals(frameName)&& spantoks[3].equals(tokenNums[0])
		     && spantoks[4].equals(tokenNums[tokenNums.length-1]);
	}
	public static void WriteFEFile(){
		writeFEFile(FEFileName.predictionFilename,
				FEFileName.spanfilename,
				FEFileName.feFilename,
				FEFileName.outfefilename);
	}
}
