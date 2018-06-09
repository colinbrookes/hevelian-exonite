package com.hevelian.exonite.connectors;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hevelian.exonite.core.CollectionItem;
import com.hevelian.exonite.core.Connector;
import com.hevelian.exonite.core.Evaluator;
import com.hevelian.exonite.utils.J2X;
import com.hevelian.exonite.interfaces.Action;
import com.hevelian.exonite.interfaces.ConnectorImpl;

public class KeenIO implements ConnectorImpl {

	private ArrayList<String> columns = new ArrayList<String>();
	private ArrayList<CollectionItem> items = null;
	private HashMap<String, Action> objects = null;
	private Evaluator evaluator = null;
	private String jsonRaw = new String();
	
	public KeenIO() {
		return;
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
		
		String keen_api_project_id 	= connector.getProperty("keenio_api_project_id");
		String keen_api_key 		= connector.getProperty("keenio_api_key");
		String keen_api_version 	= connector.getProperty("keenio_api_version");
		String queryType 			= _doc.getElementsByTagName("type").item(0).getTextContent();
		
		String BaseURL = "https://api.keen.io/" + keen_api_version + "/projects/" + keen_api_project_id + "/queries/" + queryType;
		String BaseURLParameters = "?api_key=" + keen_api_key;
		
		// we now need to build the full URL and fetch the data.
		Element nodeProperties = (Element) _doc.getElementsByTagName("properties").item(0);
		for(int i=0; i<nodeProperties.getChildNodes().getLength(); i++) {
			Node node = nodeProperties.getChildNodes().item(i);
			
			 if(node.getNodeType()!=Element.ELEMENT_NODE) continue;
			 
			 BaseURLParameters = BaseURLParameters + "&" + node.getNodeName() + "=" + evaluator.evaluate(node.getTextContent());

		}
		
		try {
			// try to fetch data and process the resulting JSON into XML
			URL url = new URL(BaseURL + BaseURLParameters);
			String inputLine;
			
			BufferedReader in;
		    URLConnection con = url.openConnection();
		    in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		    while ((inputLine = in.readLine()) != null) {
		        jsonRaw += inputLine;
		    }
		    in.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		try {
			J2X converter = new J2X();
			Document xml = converter.X(jsonRaw);
			
			if(xml==null) {
				System.out.println("EXONITE: XML DOCUMENT IS NULL AFTER JSON CONVERSION!");
			}
			
			// DEBUG: send XML to console
			/*
			try {
				String buf = J2X.prettyPrint(xml);
				System.out.println("EXONITE: XML: " + buf);
			} catch (Exception e) {
				e.printStackTrace();
			}
			*/
			
			// simple extraction instruction - retrieves the collections
			if(queryType.equalsIgnoreCase("extraction")) {
				processExtraction(xml);
			}
			
			// count, count unique, average, minimum, maximum, sum
			if(queryType.equalsIgnoreCase("count") || 
					queryType.equalsIgnoreCase("count_unique") || 
					queryType.equalsIgnoreCase("average") || 
					queryType.equalsIgnoreCase("sum") || 
					queryType.equalsIgnoreCase("maximum") || 
					queryType.equalsIgnoreCase("minimum")) {
				processMetric(xml);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return items;
	}

	/**
	 * reformat the response into a set of CollectionItems.
	 * @param xml
	 */
	private void processMetric(Document xml) {
		Node root = xml.getFirstChild();
		
		if(xml.getFirstChild().getFirstChild().getFirstChild().getFirstChild().getChildNodes().getLength()>0) {
			root = xml.getFirstChild().getFirstChild().getFirstChild().getFirstChild();
		}
		
		NodeList nodes = root.getChildNodes();
		processNodeSet(nodes);
		
		return;
	}
	
	/**
	 * reformat the response into a set of CollectionItems.
	 * @param xml
	 */
	private void processExtraction(Document xml) {
		Node root = xml.getFirstChild().getFirstChild().getFirstChild().getFirstChild();

		NodeList nodes = root.getChildNodes();
		processNodeSet(nodes);
		
		return;
	}
	
	private void processNodeSet(NodeList nodes) {
		
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
        		
        		item.setValue(node.getNodeName(), node.getChildNodes().item(0).getNodeValue());
        		
        		if(countChildElements(node)>0) ProcessChildren(item, node, node.getNodeName(), i);

        	}
        	
			items.add(item);
        }
		
	}
	
	private void ProcessChildren(CollectionItem item, Node node, String prefix, int addColumns) {

		if(node.getChildNodes().getLength()==0) return;
		
		String _prefix = prefix;
		
		for(int i=0; i<node.getChildNodes().getLength(); i++) {
			Node n = node.getChildNodes().item(i);
			
			if(n.getNodeType()!=Element.ELEMENT_NODE) continue;
			
			if(addColumns==0) {
				columns.add(_prefix + "_" + n.getNodeName());
			}
			
			if(n.getChildNodes().item(0)!=null) item.setValue(_prefix + "_" + n.getNodeName(), n.getChildNodes().item(0).getNodeValue());
			if(countChildElements(n)>0) ProcessChildren(item, n, _prefix + "_" + n.getNodeName(), addColumns);
		}
	}
	
	private int countChildElements(Node node) {

		int cnt=0;
		for(int i=0; i<node.getChildNodes().getLength(); i++) {
			if(node.getChildNodes().item(i).getNodeType()==Element.ELEMENT_NODE) cnt++;
		}
		
		return cnt;
	}

	@Override
	public String update() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String delete() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String insert() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<String> getRawColumnNames() {
		return columns;
	}

}
