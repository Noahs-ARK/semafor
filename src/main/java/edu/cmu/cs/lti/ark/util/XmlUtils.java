/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * XmlUtils.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.util;

import com.sun.org.apache.xpath.internal.XPathAPI;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;

public class XmlUtils {
	//Parses an XML file and returns a DOM document.
	// If validating is true, the contents is validated against the DTD
	// specified in the file.
	public static Document parseXmlFile(String filename, boolean validating)
			throws ParserConfigurationException, IOException, SAXException {
		// Create a builder factory
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(validating);
		// Create the builder and parse the file
		return factory.newDocumentBuilder().parse(new File(filename));
	}

	public static Element[] applyXPath(Node node, String xpath) throws TransformerException {
		// Get the matching elements
		NodeList nodelist = XPathAPI.selectNodeList(node, xpath);
		Element[] elements = new Element[nodelist.getLength()];
		// Process the elements in the nodelist
		for (int i = 0; i < nodelist.getLength(); i++) {
			// Get element
			Element elem = (Element) nodelist.item(i);
			elements[i] = elem;
		}
		return elements;
	}

	public static Element getUniqueChildNodeFromXPathTM(Document doc, String xpath) throws TransformerException {
		Element elements = null;
		// Get the matching elements
		NodeList nodelist = XPathAPI.selectNodeList(doc, xpath);
		if (nodelist == null)
			return null;
		if (nodelist.getLength() > 1) {
			System.err.println("WARNING! was expecting only one child but there are more Returning first one!!");
			return (Element) nodelist.item(0);
		} else if (nodelist.getLength() == 1) {
			return (Element) nodelist.item(0);
		}
		return elements;
	}

	public static void writeXML(String OUTPUT_XML_FILE, org.w3c.dom.Document xmlDoc) throws TransformerException {
		javax.xml.transform.Source source = new DOMSource(xmlDoc);
		System.out.println("writing File: " + OUTPUT_XML_FILE);
		// Prepare the output file
		File file = new File(OUTPUT_XML_FILE);
		Result result = new StreamResult(file);
		// Write the DOM document to the file
		javax.xml.transform.Transformer xformer = javax.xml.transform.TransformerFactory.newInstance().newTransformer();
		xformer.setOutputProperty(OutputKeys.INDENT, "yes");
		xformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		xformer.transform(source, result);
	}

	public static void addAttribute(Document doc, String name, Element e, String value) {
		Node attrNode = doc.createAttribute(name);
		attrNode.setNodeValue(value);
		NamedNodeMap attrs = e.getAttributes();
		attrs.setNamedItem(attrNode);
	}
	
	public static Document getNewDocument() throws ParserConfigurationException {
		return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
	}
			
}
