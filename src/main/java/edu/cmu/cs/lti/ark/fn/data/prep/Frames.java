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

import edu.cmu.cs.lti.ark.fn.constants.FNConstants;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import edu.cmu.cs.lti.ark.util.XmlUtils;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Arrays;


public class Frames
{
	public static void main(String[] args)
	{
		getUnknownFrameNames("/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/frXML/frames.xml");
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


















