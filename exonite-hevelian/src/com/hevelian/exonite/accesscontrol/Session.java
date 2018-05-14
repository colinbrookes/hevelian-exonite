package com.hevelian.exonite.accesscontrol;

import java.io.File;
import java.io.StringReader;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.hevelian.exonite.core.Configuration;
import com.hevelian.exonite.core.Collection;

public class Session {

	private Configuration config = new Configuration();
	private HttpServletRequest request = null;
	private boolean isLoggedIn = false;
	private HashMap<String,String> user_properties = null;
	
	private String collection_authenticate = null;
	private String collection_properties = null;
	@SuppressWarnings("unused")
	private String collection_tokens = null;
	@SuppressWarnings("unused")
	private String collections_roles = null;

	private String url_login = null;
	private String url_main = null;
	private String username = null;
	
	public Session(HttpServletRequest request) {
		this.request = request;
		
	    String auth = request.getParameter("frm_authenticator");
	    if(auth!=null) {
	    	config.setProperty("admin_authenticator", auth);
	    }
	    
		String _name = config.getProperty("admin_authenticator");
		
		try {
			File _xmlFile = new File(config.getProperty("folder_home") + config.getProperty("folder_authenticators") + _name + ".xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(_xmlFile);
			
			// extract the properties we need from the authenticator
			collection_authenticate = doc.getElementsByTagName("authenticate").item(0).getTextContent();
			collection_properties = doc.getElementsByTagName("properties").item(0).getTextContent();
			collection_tokens = doc.getElementsByTagName("tokens").item(0).getTextContent();
			collections_roles = doc.getElementsByTagName("roles").item(0).getTextContent();
			
			url_login = doc.getElementsByTagName("login").item(0).getTextContent();
			url_main = doc.getElementsByTagName("main").item(0).getTextContent();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getURL() {
		if(this.isLoggedIn) {
			return url_main;
		}
		
		return url_login;
	}
	
	public boolean isLoggedIn() {
		return this.isLoggedIn;
	}
	
	public String getProperty(String name) {
		if(this.user_properties==null) getUserProperties();
		if(this.user_properties==null) return null;
		
		return this.user_properties.get(name);
	}
	
	public HashMap<String,String> getUserProperties(HttpServletRequest req) {
		this.request = req;
		return getUserProperties();
	}
	
	public HashMap<String,String> getUserProperties() {
		if(this.user_properties!=null) return this.user_properties;
		if(this.user_properties==null) this.user_properties = new HashMap<String,String>();
		
		Collection collection = new Collection(collection_properties, this.request);
		String results = collection.select();
		
		System.out.println("USER PROPERTIES: " + results);
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(results));
			Document doc = dBuilder.parse(is);

			Element rec = (Element) doc.getElementsByTagName("record").item(0);
			for(int i=0; i<rec.getChildNodes().getLength(); i++) {
				Node node = rec.getChildNodes().item(i);
				
				if(node.getNodeType()!=Element.ELEMENT_NODE) continue;
				 
				String nodeName = node.getNodeName();
				String nodeValue = node.getTextContent();
				
				this.user_properties.put(nodeName, nodeValue);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return this.user_properties;
	}
	
	public boolean AttemptLogin(String username, String password) {
		this.username = username;
		
		Collection collection = new Collection(collection_authenticate, this.request);
		
		String results = collection.select();
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(results));
			Document doc = dBuilder.parse(is);

			NodeList columnNodes = doc.getElementsByTagName("record");
			
			if(columnNodes.getLength()>0) {
				isLoggedIn = true;
				request.getSession().setAttribute("isLoggedIn", "true");
				request.getSession().setAttribute("username", this.username);
				
				try {
					Element rec = (Element) columnNodes.item(0);
					
					for(int i=0; i<rec.getChildNodes().getLength(); i++) {
						Node node = rec.getChildNodes().item(i);
						
						if(node.getNodeType()!=Element.ELEMENT_NODE) continue;
						
						String nodeName = node.getNodeName();
						String nodeValue = node.getTextContent();
						
						request.getSession().setAttribute(nodeName, nodeValue);
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
				
				return true;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}

		// if not completely sure that we got a valid login then we return false
		request.getSession(true).setAttribute("isLoggedIn", "false");
		request.getSession().removeAttribute("session");
		request.getSession().removeAttribute("username");
		
		return false;
	}
}
