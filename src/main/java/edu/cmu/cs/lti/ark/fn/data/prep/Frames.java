/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * Frames.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.data.prep;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.cmu.cs.lti.ark.fn.constants.FNConstants;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.XmlUtils;
import gnu.trove.THashMap;
import gnu.trove.THashSet;


public class Frames
{
	public static void main(String[] args)
	{
		getUnknownFrameNames("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/frXML/frames.xml");
		//getFrameNames("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/frXML/frames.xml");
		//getLEStatistics("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/leXML");
//		try
//		{
//			BufferedWriter bWriter = new BufferedWriter(new FileWriter("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.sentences"));
//			getLUStatistics("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/luXML",bWriter);
//			bWriter.close();
//		}
//		catch(Exception e)
//		{
//			e.printStackTrace();
//		}
//	
//	
	}
	
	public static void getUnknownFrameNames(String file)
	{
		String mapFile = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData/framenet.original.map";
		THashMap<String, THashSet<String>> map = (THashMap<String, THashSet<String>>)SerializedObjects.readSerializedObject(mapFile);
		Document fDoc = XmlUtils.parseXmlFile(file, false);
		Element[] eList = XmlUtils.applyXPath(fDoc, FNConstants.FRAME_PATH);
		int len = eList.length;
		String[] frameNames = new String[len];
		for(int i = 0; i < len; i ++)
		{
			Element e = eList[i];
			frameNames[i] = e.getAttribute(FNConstants.FRAME_NAME_ATTR);
		}		
		Arrays.sort(frameNames);
		int countUnknown = 0;
		for(int i = 0; i < len; i ++)
		{
			if(map.get(frameNames[i])==null)
			{
				System.out.println(frameNames[i]);
				countUnknown++;
			}
		}
		System.out.println(countUnknown);
	}
	
	public static void getFrameNames(String file)
	{
		Document fDoc = XmlUtils.parseXmlFile(file, false);
		Element[] eList = XmlUtils.applyXPath(fDoc, FNConstants.FRAME_PATH);
		int len = eList.length;
		String[] frameNames = new String[len];
		for(int i = 0; i < len; i ++)
		{
			Element e = eList[i];
			frameNames[i] = e.getAttribute(FNConstants.FRAME_NAME_ATTR);
		}		
		Arrays.sort(frameNames);
		for(int i = 0; i < len; i ++)
		{
			System.out.println(frameNames[i]);
		}
		System.out.println("Total number of frames:"+len);
	}
	
	public static void getLEStatistics(String directory)
	{
		File dir = new File(directory);
		FilenameFilter filter = new FilenameFilter(){
			public boolean accept(File dir, String name) {
				return name.endsWith(".xml");
			}
		};
		String[] fileNames = dir.list(filter);
		int len = fileNames.length;
		System.out.println("Length of files:"+len);
		ArrayList<String> units = new ArrayList<String>();
		for(int i = 0; i < len; i ++)
		{
			String fileName = dir.getAbsolutePath()+"/"+fileNames[i];
			Document d = XmlUtils.parseXmlFile(fileName, false);
			Element[] leList = XmlUtils.applyXPath(d, FNConstants.LE_PATH);
			String leName = leList[0].getAttribute(FNConstants.LE_NAME_ATTR);
			if(leList.length>1)
			{
				System.out.println("Yes:+"+fileNames[i]);
				System.exit(0);
			}
			//System.out.println(i+":"+leName);
			units.add(leName);
		}		
		String[] unitsArray = new String[units.size()];
		units.toArray(unitsArray);
		Arrays.sort(unitsArray);
		for(int i = 0; i < unitsArray.length; i ++)
		{
			System.out.println(unitsArray[i]);
		}
	}
	
	public static void getLUStatistics(String directory)
	{
		File dir = new File(directory);
		FilenameFilter filter = new FilenameFilter(){
			public boolean accept(File dir, String name) {
				return name.endsWith(".xml");
			}
		};
		String[] fileNames = dir.list(filter);
		int len = fileNames.length;
		System.out.println("Length of files:"+len);
		ArrayList<String> units = new ArrayList<String>();
		int totalNumberOfSentences = 0;
		for(int i = 0; i < len; i ++)
		{
			String fileName = dir.getAbsolutePath()+"/"+fileNames[i];
			Document d = XmlUtils.parseXmlFile(fileName, false);
			Element[] luList = XmlUtils.applyXPath(d, FNConstants.LU_PATH);
			String luName = luList[0].getAttribute(FNConstants.LU_NAME_ATTR);
			System.out.println(luName+"\t"+fileNames[i]);
			NodeList children = luList[0].getChildNodes();
			for(int j = 0; j < children.getLength(); j ++)
			{
				Node n = children.item(j);
				if(!n.getNodeName().equals(FNConstants.SUB_CORPUS))
					continue;
				NodeList aSetChildren = n.getChildNodes();
				for(int k = 0; k < aSetChildren.getLength(); k ++)
				{
					Node n1 = aSetChildren.item(k);
					if(!n1.getNodeName().equals(FNConstants.ANNOTATION_SET))
						continue;
					NodeList bSetChildren = n1.getChildNodes();
					for(int l = 0; l < bSetChildren.getLength(); l ++)
					{
						Node n2 = bSetChildren.item(l);
						if(!n2.getNodeName().equals(FNConstants.SENTENCE))
							continue;
						String text = getTextOfSentence(n2);
						System.out.println(text);
						totalNumberOfSentences++;
					}
				}				
			}			
		}
		System.out.println("Total number of sentences:"+totalNumberOfSentences);
	}
	
	public static void getLUStatistics(String directory, BufferedWriter bWriter) throws Exception
	{
		File dir = new File(directory);
		FilenameFilter filter = new FilenameFilter(){
			public boolean accept(File dir, String name) {
				return name.endsWith(".xml");
			}
		};
		String[] fileNames = dir.list(filter);
		int len = fileNames.length;
		System.out.println("Length of files:"+len);
		ArrayList<String> units = new ArrayList<String>();
		int totalNumberOfSentences = 0;
		for(int i = 0; i < len; i ++)
		{
			String fileName = dir.getAbsolutePath()+"/"+fileNames[i];
			Document d = XmlUtils.parseXmlFile(fileName, false);
			Element[] luList = XmlUtils.applyXPath(d, FNConstants.LU_PATH);
			String luName = luList[0].getAttribute(FNConstants.LU_NAME_ATTR);
			System.out.println(luName+"\t"+fileNames[i]);
			NodeList children = luList[0].getChildNodes();
			for(int j = 0; j < children.getLength(); j ++)
			{
				Node n = children.item(j);
				if(!n.getNodeName().equals(FNConstants.SUB_CORPUS))
					continue;
				NodeList aSetChildren = n.getChildNodes();
				for(int k = 0; k < aSetChildren.getLength(); k ++)
				{
					Node n1 = aSetChildren.item(k);
					if(!n1.getNodeName().equals(FNConstants.ANNOTATION_SET))
						continue;
					NodeList bSetChildren = n1.getChildNodes();
					for(int l = 0; l < bSetChildren.getLength(); l ++)
					{
						Node n2 = bSetChildren.item(l);
						if(!n2.getNodeName().equals(FNConstants.SENTENCE))
							continue;
						String text = getTextOfSentence(n2);
						bWriter.write(text+"\n");
						totalNumberOfSentences++;
					}
				}				
			}			
		}
		System.out.println("Total number of sentences:"+totalNumberOfSentences);
	}
	
	
	public static String getTextOfSentence(Node n)
	{
		NodeList children = n.getChildNodes();
		for(int i = 0; i < children.getLength(); i ++)
		{
			Node child = children.item(i);
			if(!child.getNodeName().equals("text"))
				continue;
			String content = child.getTextContent();
			return content;
		}
		System.out.println("Error");
		System.exit(0);
		return null;
	}	
}


















