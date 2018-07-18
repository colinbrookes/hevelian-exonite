package com.hevelian.exonite.actions;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.hevelian.exonite.core.CollectionItem;
import com.hevelian.exonite.core.Configuration;
import com.hevelian.exonite.core.Evaluator;
import com.hevelian.exonite.interfaces.Action;

public class Collection implements Action {

	ArrayList<CollectionItem> items 	= null;
	HashMap<String, String> params 		= new HashMap<String, String>();
	int itemPtr 						= -1;
	Configuration configuration			= new Configuration();

	NodeList nodes						= null;
	Document doc						= null;
	DocumentBuilderFactory dbFactory 	= null;
	DocumentBuilder dBuilder;
	
	com.hevelian.exonite.core.Collection collection = null;	 

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
		
		String _name	= child.getAttribute("src");
		collection 		= new com.hevelian.exonite.core.Collection(_name, null, map);

		String storeToFile = child.getAttribute("storeToFile");
		if(storeToFile!=null && !storeToFile.equalsIgnoreCase("")) {
			try {
				PrintWriter out = new PrintWriter(configuration.getProperty("folder_home") + storeToFile);
				out.print(collection.select());
				out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		items = collection.selectRaw();
	}
	
	/**
	 * returns the complete xmlised record currently being pointed to.
	 * We convert the xml into a doc if this has not already been done yet - JIT.
	 * 
	 * @return
	 */
	@Override
	public Element getRecord() {
		
		if(doc==null) {
			String records = collection.select();
			try {
				InputStream is = new ByteArrayInputStream(records.getBytes("UTF-8"));
				dBuilder = dbFactory.newDocumentBuilder();
				doc = dBuilder.parse(is);
			} catch (Exception e) {
				e.printStackTrace();
			}
			nodes = doc.getElementsByTagName("record");
		}
		
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
