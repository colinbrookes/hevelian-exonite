package com.hevelian.exonite.core;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.hevelian.exonite.interfaces.Action;

/**
 * JoinCollection is a simple join function based on matching unique primary keys.
 * 
 * @author cb
 *
 */
public class JoinCollection {

	private String collectionName = null;
	private Collection collection = null;
	
	private String fromKey = null;
	private String toKey = null;
	private String usePrefix = null;
	
	public JoinCollection(HttpServletRequest request, HashMap<String, Action> objects, Element node) {
		
		collectionName = node.getAttribute("toCollection");
		fromKey = node.getAttribute("fromKey");
		toKey = node.getAttribute("toKey");
		usePrefix = node.getAttribute("usePrefix");

		collection = new Collection(collectionName, request);
	}

	/**
	 * We call the select method on the joined-to collection and parse the xml response into a set of items. 
	 * (not very efficient actually, better to get the 'items' set instead).
	 * 
	 * @param items
	 * @return
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public ArrayList<CollectionItem> run(ArrayList<CollectionItem> items) throws ParserConfigurationException, SAXException, IOException {
		
		String toCollectionRaw = collection.select();

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(toCollectionRaw));
		Document document = dBuilder.parse(is);
		
		NodeList nodes = document.getElementsByTagName("record");
		
		for(int i=0; i<items.size(); i++) {
			CollectionItem item = items.get(i);
			
			for(int n=0; n<nodes.getLength(); n++) {
				Element node = (Element) nodes.item(n);
				
				String to = node.getElementsByTagName(toKey).item(0).getTextContent();
				
				if(item.getValue(fromKey).equalsIgnoreCase(to)) {
					// we have a key match, so we copy over the attributes
					for(int c=0; c<node.getChildNodes().getLength(); c++) {
						if(node.getChildNodes().item(c).getNodeType()!=Element.ELEMENT_NODE) continue;
						
						item.setValue(usePrefix + node.getChildNodes().item(c).getNodeName(), node.getChildNodes().item(c).getTextContent());
					}
					break;
				}
			}
		}
		
		return items;
	}

}
