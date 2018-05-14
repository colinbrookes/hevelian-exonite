package com.hevelian.exonite.connectors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
import com.hevelian.exonite.core.Configuration;
import com.hevelian.exonite.core.Connector;
import com.hevelian.exonite.core.Evaluator;
import com.hevelian.exonite.interfaces.Action;
import com.hevelian.exonite.interfaces.ConnectorImpl;

public class Cache implements ConnectorImpl {

	private ArrayList<String> columns 		= new ArrayList<String>();
	private ArrayList<CollectionItem> items = null;
	private Evaluator evaluator 			= null;
	private HashMap<String, Action> objects = null;
	private Configuration config			= new Configuration();

	private String cacheLocation			= null;
	private String cacheIndexes				= null;
	
	// the collection properties
	private String use_index				= null;
	private String full_text_index			= null;
	private String full_text_counters		= null;
	
	DocumentBuilderFactory dbFactory 		= null;
	DocumentBuilder dBuilder 				= null;

	public Cache() {
		try {
			dbFactory = DocumentBuilderFactory.newInstance();
			dBuilder = dbFactory.newDocumentBuilder();
		} catch(Exception e) {
			return;
		}
	}
	
	/**
	 * This is the main select routine for fetching documents from the cache. The docs are appended into a single
	 * array of collection items and returned.
	 */
	@Override
	public ArrayList<CollectionItem> select(Document _doc, Connector connector, HttpServletRequest request) {

		items = new ArrayList<CollectionItem>();
		evaluator = new Evaluator(request, objects);
		
		cacheLocation		= evaluator.evaluate(connector.getProperty("cache_location"));
		cacheIndexes		= evaluator.evaluate(connector.getProperty("cache_indexes"));

		fetchProperties(_doc);
		
		if(cacheLocation==null||cacheIndexes==null && (use_index==null||full_text_index==null)) {
			System.out.println("EXONITE: CACHE CONNECTOR: not configured correctly");
			return items;
		}
		
		// regular index on a data set
		if(use_index!=null) {
			String indexPath = config.getProperty("folder_home") + "data/" + cacheIndexes + "/" + use_index + "/keys/";
			File indexFile = new File(indexPath);
			File[] indexes = indexFile.listFiles();

			for(int i=0; i<indexes.length; i++) {
				// we read the indexes file, and create a CollectionItem for each record
				readAllRecords(indexes[i]);
			}
		}
		
		// full text index on a data set
		if(full_text_index!=null) {
			if(full_text_counters!=null) {
				// we want the counters and not the records
				String indexPath = config.getProperty("folder_home") + "data/" + cacheIndexes + "/" + full_text_index + "/" + full_text_counters + "/index.dat";
				File indexFile = new File(indexPath);
				readCountersFile(indexFile);
			}
		}
		
		return items;
	}

	/**
	 * this fetches the counters for words in a full-text index, not the actual records that had the word
	 * @param index
	 */
	private void readCountersFile(File index) {
		try {
			
			columns.add("word");
			columns.add("stopword");
			columns.add("occurred");
			
			BufferedReader br = new BufferedReader(new FileReader(index));
			for(String line; (line = br.readLine()) != null; ) {
				String[] parts = line.split("@@");
				
				CollectionItem item = new CollectionItem();
				
				item.setValue("word",  parts[0]);
				item.setValue("stopword",  parts[1]);
				item.setValue("occurred",  parts[2]);
				
				items.add(item);
			}
			
			br.close();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * this fetches all the records for a regular index file
	 * @param index
	 */
	private void readAllRecords(File index) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(index));
			for(String line; (line = br.readLine()) != null; ) {
				String[] parts = line.split("@@");
				String filename = parts[1];
				
				File file = new File(filename);
				Document doc = dBuilder.parse(file);
				
		        NodeList nodes = doc.getElementsByTagName("record");
		        
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
		        	}
	    			items.add(item);
		        }
			}
			br.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void fetchProperties(Document doc) {
		try {
			Node properties = doc.getElementsByTagName("parameters").item(0);
			
			for(int i=0; i<properties.getChildNodes().getLength(); i++) {
				Node n = properties.getChildNodes().item(i);
				
				if(n.getNodeName().equalsIgnoreCase("use_index")) {
					use_index = evaluator.evaluate(n.getTextContent());
				}
				
				if(n.getNodeName().equalsIgnoreCase("full_text_index")) {
					full_text_index = evaluator.evaluate(n.getTextContent());
				}

				if(n.getNodeName().equalsIgnoreCase("full_text_counters")) {
					full_text_counters = evaluator.evaluate(n.getTextContent());
				}
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
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
