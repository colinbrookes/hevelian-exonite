package com.hevelian.exonite.core;

import com.hevelian.exonite.core.Configuration;
import com.hevelian.exonite.interfaces.Action;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.*;

public class Connector {

	private Configuration config = new Configuration();
	private Properties Properties = new Properties();
	private com.hevelian.exonite.interfaces.ConnectorImpl connector = null;
	private HttpServletRequest request = null;

	private String Name = "";
	private String File = null;
	private Document document = null;
	private HashMap<String, Action> objects = null;
	
	public Connector() {
		return;
	}
	
	public Connector(String _name, Document _doc, HttpServletRequest request, HashMap<String, Action> map) {
		this.Name = _name;
		this.File = _name;
		this.document = _doc;
		this.request = request;
		this.objects = map;
		
		run();
	}
	
	public Connector(String _name, Document _doc, HttpServletRequest request) {
		this.Name = _name;
		this.File = _name;
		this.document = _doc;
		this.request = request;
		
		run();
	}
	
	private void run() {
		/* read the XML file and extract the properties */
		try {
			this.File = this.config.getProperty("folder_home") + this.config.getProperty("folder_connectors") + this.File + ".xml";
			
			File _xmlFile = new File(this.File);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(_xmlFile);
			Element root = (Element) doc.getChildNodes().item(0);
			
			for(int i=0; i<root.getChildNodes().getLength(); i++) {
				Node _node = root.getChildNodes().item(i);
				
				if(_node.getNodeType() == Node.ELEMENT_NODE) {
					Properties.setProperty(_node.getNodeName(), _node.getTextContent());
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		/* now we create an instance of the specified class */
		try {
			Class<?> c = Class.forName(config.getTypeByName(Properties.getProperty("type")));
			connector = (com.hevelian.exonite.interfaces.ConnectorImpl) c.newInstance();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<CollectionItem> select() {
		if(connector==null) {
			System.out.println("CONNECTOR: is NULL");
		}
		
		return connector.select(this.document, this, request, objects);
	}
	
	public ArrayList<CollectionItem> select(Document _doc, Connector con) {
		return connector.select(_doc, this, request, objects);
	}
	
	public String update() {
		return connector.update();
	}
	
	public String delete() {
		return connector.delete();
	}
	
	public String insert() {
		return connector.insert();
	}
	
	public String getName() {
		return Name;
	}
	
	public ArrayList<String> getRawColumnNames() {
		return connector.getRawColumnNames();
	}
	
	public String getProperty(String _name) {
		return Properties.getProperty(_name);
	}
}
