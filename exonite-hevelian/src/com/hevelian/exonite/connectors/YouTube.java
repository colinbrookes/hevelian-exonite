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

/*
 * https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=50&&key={YOUR_API_KEY}
 */
public class YouTube implements ConnectorImpl {

	private ArrayList<String> columns = new ArrayList<String>();
	private ArrayList<CollectionItem> items = null;
	private HashMap<String, Action> objects = null;
	private Evaluator evaluator = null;
	private String jsonRaw = new String();
	
	public YouTube() {
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
	public ArrayList<CollectionItem> select(Document _doc, com.hevelian.exonite.core.Connector connector, HttpServletRequest request) {
		items = new ArrayList<CollectionItem>();
		evaluator = new Evaluator(request, objects);
		
		String youtube_api_key 		= connector.getProperty("youtube_api_key");
		String youtube_api_version 	= connector.getProperty("youtube_api_version");
		
		String BaseURL = "https://www.googleapis.com/youtube/" + youtube_api_version + "/search";
		String BaseURLParameters = "?key=" + youtube_api_key;
		
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
				System.out.println("EXONITE: YOUTUBE XML: " + buf);
			} catch (Exception e) {
				e.printStackTrace();
			}
			*/
			
			processResult(xml);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return items;
	}

	private void processResult(Document xml) {
		Node root = xml.getElementsByTagName("items").item(0).getFirstChild();
		NodeList nodes = root.getChildNodes();
		processNodeSet(nodes);
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
