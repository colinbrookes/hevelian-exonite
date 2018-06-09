package com.hevelian.exonite.actions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hevelian.exonite.core.CollectionItem;
import com.hevelian.exonite.core.Evaluator;
import com.hevelian.exonite.interfaces.Action;

public class Collection implements Action {

	 ArrayList<CollectionItem> items 	= null;
	 NodeList nodes						= null;
	 HashMap<String, String> params 	= new HashMap<String, String>();
	 int itemPtr 						= -1;

	public Collection() {
		items = new ArrayList<CollectionItem>();
	}

	/**
	 * A Collection action is not a container, so doesnt affect the object list. It does create and initialise
	 * a collection object, which has all the evaluated results in it.
	 */
	@Override
	public void run(Element child, HashMap<String, Action> map) {
		
		Evaluator evaluator = new Evaluator(map);
		
		// first we need to process any params
		if(child.hasChildNodes()) {
			NodeList children = child.getChildNodes();
			for(int i=0; i<children.getLength(); i++) {
				if(children.item(i).getNodeType()!=Element.ELEMENT_NODE) continue;
				Element node = (Element) children.item(i);
				params.put(node.getNodeName(), evaluator.evaluate(node.getTextContent()));
			}
		}
		
		String _name 										= child.getAttribute("src");
		com.hevelian.exonite.core.Collection collection 	= new com.hevelian.exonite.core.Collection(_name, null, map);
		String records 										= collection.select();
		Document doc										= null;
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			InputStream is = new ByteArrayInputStream(records.getBytes("UTF-8"));
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(is);
		} catch (Exception e) {
			e.printStackTrace();
			items = new ArrayList<CollectionItem>();
			return;
		}
		
		// we got a set of records, so we create an array of CollectionItems from it
		nodes = doc.getElementsByTagName("record");
		
        for(int i=0; i<nodes.getLength(); i++) {
        	Element e = (Element) nodes.item(i);
			CollectionItem item = new CollectionItem();
			
        	for(int c=0; c<e.getChildNodes().getLength(); c++) {
        		Node node = e.getChildNodes().item(c);
        		
        		if(node.getNodeType()!=Element.ELEMENT_NODE) continue;
        		
        		item.setValue(node.getNodeName(), node.getTextContent());
        	}
        	
			items.add(item);
        }

	}
	
	/**
	 * returns the complete xmlised record currently being pointed to.
	 * @return
	 */
	@Override
	public Element getRecord() {
		if(nodes==null || nodes.getLength()==0) {
			return null;
		}
		
		return (Element) nodes.item(itemPtr);
	}
	
	/**
	 * returns the named property of the current record.
	 * @param name
	 * @return
	 */
	@Override
	public String getProperty(String name) {
		
		if(itemPtr==-1) {
			if(items==null || items.isEmpty()) {
				return "";
			}
			
			itemPtr=0;
		}
		
		return items.get(itemPtr).getValue(name);
	}
	
	/**
	 * returns the count of the number of records in the collection
	 * @return
	 */
	@Override
	public int size() {
		return items.size();
	}
	
	/**
	 * moves the pointer to the next record in the set
	 */
	@Override
	public void next() {
		if(items.isEmpty()) {
			itemPtr=-1;
			return;
		}
		
		itemPtr++;
		if(items.size() <= itemPtr) {
			itemPtr=0;
		}
	}
}
