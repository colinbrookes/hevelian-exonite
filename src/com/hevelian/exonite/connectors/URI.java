package com.hevelian.exonite.connectors;

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hevelian.exonite.core.CollectionItem;
import com.hevelian.exonite.core.Connector;
import com.hevelian.exonite.core.Evaluator;
import com.hevelian.exonite.interfaces.Action;
import com.hevelian.exonite.interfaces.ConnectorImpl;

public class URI implements ConnectorImpl {

	private ArrayList<String> columns = new ArrayList<String>();
	private ArrayList<CollectionItem> items = null;
	private HashMap<String, Action> objects = null;
	private Evaluator evaluator = null;
	private Document doc = null;
	
	public URI() {
	}
	
	@Override
	public ArrayList<CollectionItem> select() {
		return items;
	}

	@Override
	public ArrayList<CollectionItem> select(Document _doc, Connector connector, HttpServletRequest request, HashMap<String, Action> map) {
		objects = map;
		return select(_doc, connector, request);
	}

	@Override
	public ArrayList<CollectionItem> select(Document _doc, Connector connector, HttpServletRequest request) {
		
		items = new ArrayList<CollectionItem>();
		evaluator = new Evaluator(request, objects);

		String uri = evaluator.evaluate(_doc.getElementsByTagName("uri").item(0).getTextContent());
		String root = evaluator.evaluate(_doc.getElementsByTagName("root").item(0).getTextContent());
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(uri);
			
	        NodeList nodes = doc.getElementsByTagName(root);
	        
	        for(int i=0; i<nodes.getLength(); i++) {
	        	Element e = (Element) nodes.item(i);
				CollectionItem item = new CollectionItem();
				
	        	for(int c=0; c<e.getChildNodes().getLength(); c++) {
	        		Node node = e.getChildNodes().item(c);
	        		
	        		if(node.getNodeType()!=Element.ELEMENT_NODE) continue;
	        		
	        		// bad assumption here ...
	        		if(i==0) {
	        			columns.add(node.getNodeName());
	        		}
	        		
	        		item.setValue(node.getNodeName(), node.getTextContent());
	        		
	        		ProcessChildren(item, node, "", i);

	        	}
	        	
    			items.add(item);
	        }
			
		} catch(Exception e) {
			e.printStackTrace();
		}
        
		return items;
	}

	/**
	 * Recurses through the child elements and flattens the contents.
	 * @param item
	 * @param node
	 * @param prefix
	 * @param addColumns
	 */
	private void ProcessChildren(CollectionItem item, Node node, String prefix, int addColumns) {

		if(node.getChildNodes().getLength()==0) return;
		
		String _prefix = prefix + node.getNodeName();
		
		for(int i=0; i<node.getChildNodes().getLength(); i++) {
			Node n = node.getChildNodes().item(i);
			
			if(n.getNodeType()!=Element.ELEMENT_NODE) continue;
			
			if(addColumns==0) {
				columns.add(_prefix + "_" + n.getNodeName());
			}
			
			item.setValue(_prefix + "_" + n.getNodeName(), n.getTextContent());
			
			ProcessChildren(item, n, _prefix + "_" + n.getNodeName(), addColumns);
		}
		
		
	}
	
	@Override
	public String update() {
		return null;
	}

	@Override
	public String delete() {
		return null;
	}

	@Override
	public String insert() {
		return null;
	}

	@Override
	public ArrayList<String> getRawColumnNames() {
		return columns;
	}

}
