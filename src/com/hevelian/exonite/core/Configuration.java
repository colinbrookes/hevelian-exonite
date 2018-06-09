package com.hevelian.exonite.core;

import java.io.File;
import java.util.HashMap;

import javax.naming.InitialContext;
import javax.naming.Context;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class Configuration {
	
	Context ctx 								= null;
	HashMap<String,String> config 				= new HashMap<String,String>();
	String homePath								= null;

	private final String CONTEXT_NAME			= "hevelian";
	private final String FOLDER_AUTHENTICATORS	= "authenticators/";
	private final String FOLDER_COLLECTIONS		= "collections/";
	private final String FOLDER_CONNECTORS		= "connectors/";
	private final String FOLDER_LAYOUTS			= "layouts/";
	private final String ADMIN_AUTHENTICATOR	= "exonite_admin";
	private final String CLASS_MAPPING			= "hevelian.xml";
	
	public Configuration() {
		
		try {
		    
			ctx = new InitialContext();
			try {
				homePath = (String) ctx.lookup(CONTEXT_NAME);
			} catch(Exception e) {
				ctx = (Context) ctx.lookup("java:comp/env");
				homePath = (String) ctx.lookup(CONTEXT_NAME);
			}
			
			if(homePath==null) {
				System.out.println("EXONITE: ERROR: context 'hevelian' not found in JNDI.");
				return;
			}
			
			config.put("folder_home", homePath);
			config.put("folder_collections", FOLDER_COLLECTIONS);
			config.put("folder_authenticators", FOLDER_AUTHENTICATORS);
			config.put("folder_connectors", FOLDER_CONNECTORS);
			config.put("folder_layouts", homePath + FOLDER_LAYOUTS);

			config.put("admin_authenticator", ADMIN_AUTHENTICATOR);
			config.put("class_mapping", CLASS_MAPPING);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setProperty(String name, String value) {
		config.put(name, value);
	}
	
	/**
	 * Returns the value of a given property of the JNDI definitions for ExoniteJ
	 * @param _name
	 * @return
	 */
	public String getProperty(String _name) {
		return (String) config.get(_name);
	}
	
	/**
	 * Find all the scheduled tasks defined in the configuration.
	 * @return
	 */
	public NodeList getScheduledTasks() {
		NodeList _nodes = null;
		
		try {
			File _xmlFile = new File(getProperty("folder_home") + getProperty("class_mapping"));
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(_xmlFile);
			
			_nodes = doc.getElementsByTagName("schedule");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return _nodes;
	}
	
	/**
	 * Find the class name for a given action name, return the class name
	 * @param _name
	 * @return
	 */
	public String getActionByName(String _name) {
		String _action = null;
		
		try {
			File _xmlFile = new File(getProperty("folder_home") + getProperty("class_mapping"));
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(_xmlFile);
			
			NodeList _nodes = doc.getElementsByTagName("actions");
			
			for(int i=0; i<_nodes.item(0).getChildNodes().getLength(); i++) {
				if(_nodes.item(0).getChildNodes().item(i).getNodeName().equals(_name)) {
					_action = _nodes.item(0).getChildNodes().item(i).getTextContent();
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return _action;
	}
	
	/**
	 * Find the class name for a given connector type name, return the class name
	 * @param _name
	 * @return
	 */
	public String getTypeByName(String _name) {
		
		String _type = null;
		
		try {
			File _xmlFile = new File(getProperty("folder_home") + getProperty("class_mapping"));
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(_xmlFile);
			
			NodeList _nodes = doc.getElementsByTagName("types");
			
			for(int i=0; i<_nodes.item(0).getChildNodes().getLength(); i++) {
				if(_nodes.item(0).getChildNodes().item(i).getNodeName().equals(_name)) {
					_type = _nodes.item(0).getChildNodes().item(i).getTextContent();
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return _type;
	}
}
