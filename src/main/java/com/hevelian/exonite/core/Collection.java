package com.hevelian.exonite.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hevelian.exonite.core.Connector;
import com.hevelian.exonite.interfaces.Action;

public class Collection {

	private String Name = "";
	private String filename = null;
	private Connector connector = null;
	private Configuration config = null;
	private Document doc = null;
	private Element columns = null;
	private HttpServletRequest request = null;
	private Evaluator evaluator = null;
	private HashMap<String, Action> objects = null;
	private SelectFilter filter = null;
	private SetRotator rotator = null;
	private Limit limiter = null;
	private JoinCollection joinCollection = null;
	private SetPreprocessor setPreprocessor = null;
	
	public Collection(String _name, HttpServletRequest request, HashMap<String, Action> objects) {
		this.Name 		= _name;
		this.request 	= request;
		this.objects 	= objects;
		this.config 	= new Configuration();
		
		init();
		
	}
	
	public Collection(String _name, HttpServletRequest request) {
		this.Name 		= _name;
		this.request 	= request;
		this.config 	= new Configuration();
		
		init();
	}
		
	private void init() {
		filename = this.config.getProperty("folder_home") + this.config.getProperty("folder_collections") + this.Name + ".xml";
		
		try {
			
			File _xmlFile = new File(filename);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(_xmlFile);
			
			Node rootNode = doc.getElementsByTagName("collection").item(0);
			
			NodeList childNodes = rootNode.getChildNodes();
			for(int i=0; i<childNodes.getLength(); i++) {
				Node node = childNodes.item(i);
				
				if(node.getNodeType()!=Element.ELEMENT_NODE) continue;
				
				switch(node.getNodeName()) {
				case "select":
					filter = new SelectFilter(this.request, this.objects, (Element) node);
					break;
					
				case "columns":
					columns = (Element) node;
					break;
					
				case "rotateSet":
					rotator = new SetRotator(this.request, this.objects, (Element) node);
					break;
					
				case "limit":
					limiter = new Limit(this.request, this.objects, (Element) node);
					break;
					
				case "connector":
					connector = new Connector(node.getTextContent(), doc, this.request, objects);
					break;
					
				case "join":
					joinCollection = new JoinCollection(this.request, this.objects, (Element) node);
					break;
					
				case "preProcessor":
					setPreprocessor = new SetPreprocessor(this.request, this.objects, (Element) node);
					break;
					
				default:
					break;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<CollectionItem> selectRaw() {
		 ArrayList<CollectionItem> items = connector.select();
		 
		 if(joinCollection!=null) {
			 try {
				items = joinCollection.run(items);
			} catch (Exception e) {
				e.printStackTrace();
			}
		 }
		 
		 if(rotator!=null) {
			 items = rotator.run(items);
		 }
		 
		 if(limiter!=null) {
			 items = limiter.run(items, filter);
		 }
		 
		 if(setPreprocessor!=null) {
			 items = setPreprocessor.run(items);
		 }
		 
		 Iterator<CollectionItem> it = items.iterator();
		 if(columns!=null) {
			 
			 ArrayList<CollectionItem> resultSet = new ArrayList<CollectionItem>();
			 evaluator = new Evaluator(request, objects);
			 Boolean includeRaw = false;
			 String rawPrefix = "data_";
			 
			 if(columns.hasAttribute("includeRaw")) {
				 rawPrefix = columns.getAttribute("rawPrefix");
				 includeRaw = true;
			 }
			 
			 while(it.hasNext()) {
				 CollectionItem item = it.next();
				 CollectionItem resultItem = new CollectionItem();
				 
				 if(limiter!=null || (filter!=null && !filter.matches(item))) continue;
				 
				 /* raw columns included, so we add these first */
				 if(includeRaw==true) {
					 ArrayList<String> columns = item.getColumns();
					 ArrayList<String> values = item.getValues();
					 
					 for(int i=0; i<columns.size(); i++) {
						 String column = columns.get(i);
						 String value = values.get(i);
						 if(value==null) value="";
						 
						 resultItem.setValue(rawPrefix + column, value);
					 }
				 }
				 
				 /* now do the mapped columns */
				 for(int i=0; i<columns.getChildNodes().getLength(); i++) {
					 Node node = columns.getChildNodes().item(i);
					 String isScript = null;
					 String value = "";
					 
					 if(node.getNodeType()!=Element.ELEMENT_NODE) continue;
					 
					 if(node.hasAttributes() && node.getAttributes().getNamedItem("type")!=null) {
						 isScript = node.getAttributes().getNamedItem("type").getNodeValue();
						 
					 }
					 
					 String column = node.getNodeName();
					 
					 if(isScript==null || !isScript.equalsIgnoreCase("script")) {
						 value = evaluator.evaluate(node.getTextContent(), item);
					 } else {
						 value = evaluator.evaluateScript(node.getTextContent(), item, items);
					 }
					 
					 resultItem.setValue(column, value);
				 }
				 
				 resultSet.add(resultItem);
			 }
			 return resultSet;
		 }
		 return items;
	}
	
	public String select() {
		 ArrayList<CollectionItem> items = connector.select();
		 
		 if(joinCollection!=null) {
			 try {
				items = joinCollection.run(items);
			} catch (Exception e) {
				e.printStackTrace();
			}
		 }
		 
		 if(rotator!=null) {
			 items = rotator.run(items);
		 }
		 
		 if(limiter!=null) {
			 items = limiter.run(items, filter);
		 }
		 
		 if(setPreprocessor!=null) {
			 items = setPreprocessor.run(items);
		 }
		 
		 StringBuilder xml = new StringBuilder();
		 xml.append("<records>\n");
		 
		 Iterator<CollectionItem> it = items.iterator();
		 if(columns!=null) {
			 evaluator = new Evaluator(request, objects);
			 Boolean includeRaw = false;
			 String rawPrefix = "data_";
			 
			 if(columns.hasAttribute("includeRaw")) {
				 rawPrefix = columns.getAttribute("rawPrefix");
				 includeRaw = true;
			 }
			 
			 while(it.hasNext()) {
				 CollectionItem item = it.next();
				 
				 if(limiter!=null || (filter!=null && !filter.matches(item))) continue;
				 
				 xml.append("	<record>\n");

				 /* raw columns included, so we add these first */
				 if(includeRaw==true) {
					 ArrayList<String> columns = item.getColumns();
					 ArrayList<String> values = item.getValues();
					 
					 for(int i=0; i<columns.size(); i++) {
						 String column = columns.get(i);
						 String value = values.get(i);
						 if(value==null) value="";
						 
						 xml.append("		<" + rawPrefix + column + "><![CDATA[");
						 
						 xml.append(value);
						 xml.append("]]></" + rawPrefix + column + ">\n");
					 }
				 }
				 
				 /* now do the mapped columns */
				 for(int i=0; i<columns.getChildNodes().getLength(); i++) {
					 Node node = columns.getChildNodes().item(i);
					 String isScript = null;
					 String value = "";
					 
					 if(node.getNodeType()!=Element.ELEMENT_NODE) continue;
					 
					 if(node.hasAttributes() && node.getAttributes().getNamedItem("type")!=null) {
						 isScript = node.getAttributes().getNamedItem("type").getNodeValue();
						 
					 }
					 
					 String column = node.getNodeName();
					 
					 if(isScript==null || !isScript.equalsIgnoreCase("script")) {
						 value = evaluator.evaluate(node.getTextContent(), item);
					 } else {
						 value = evaluator.evaluateScript(node.getTextContent(), item, items);
					 }
					 xml.append("		<" + column + "><![CDATA[");
					 xml.append(value);
					 xml.append("]]></" + column + ">\n");
				 }
				 xml.append("	</record>\n");
			 }
		 } else {
			 while(it.hasNext()) {
				 CollectionItem item = it.next();
				 
				 if(filter!=null && !filter.matches(item)) continue;
				 
				 ArrayList<String> columns = item.getColumns();
				 ArrayList<String> values = item.getValues();
				 
				 xml.append("	<record>\n");
				 for(int i=0; i<columns.size(); i++) {
					 String column = columns.get(i);
					 String value = values.get(i);
					 if(value==null) value="";
					 
					 xml.append("		<" + column + "><![CDATA[");
					 xml.append(value);
					 xml.append("]]></" + column + ">\n");
				 }
				 xml.append("	</record>\n");
			 }
			 
		 }
		 
		 xml.append("</records>");
		 return xml.toString();
	}
	
	public ArrayList<String> getRawColumnNames() {
		return connector.getRawColumnNames();
	}
}
