package com.hevelian.exonite.connectors;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.hevelian.exonite.core.CollectionItem;
import com.hevelian.exonite.core.Connector;
import com.hevelian.exonite.core.Evaluator;
import com.hevelian.exonite.interfaces.Action;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class Folder implements com.hevelian.exonite.interfaces.ConnectorImpl {

	private ArrayList<String> columns = new ArrayList<String>();
	private ArrayList<CollectionItem> items = null;
	private Evaluator evaluator = null;
	private HashMap<String, Action> objects = null;
	
	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ'Z'";
	
	private String path = "";
	private String pathname;
	private boolean include_folders = false;
	private boolean include_subfolders = false;
	private boolean include_fullpath = true;
	
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
		
		fetchProperties(_doc);
		pathname = connector.getProperty("folder_root") + evaluator.evaluate(path);
		
		columns.add("name");
		columns.add("type");
		columns.add("size");
		columns.add("uri");
		columns.add("can_execute");
		columns.add("can_read");
		columns.add("can_write");
		columns.add("is_hidden");
		columns.add("date_lastmodified");
		if(include_fullpath==true) columns.add("path");
		if(include_fullpath==true) columns.add("parent");

		File file = new File(pathname);
		if(file.isFile()==true) {
			addItemFrom(file);
			return items;
		}
		
		addItemsFromFolder(file);
		return items;
	}

	private void addItemsFromFolder(File folder) {
		
		File[] files = folder.listFiles();
		for(int i=0; i<files.length; i++) {
			File f = files[i];
			
			if(f.getName()=="." || f.getName() =="..") continue;
			
			if(f.isFile()) {
				addItemFrom(f);
			}
			
			if(f.isDirectory() && include_folders==true) {
				addItemFrom(f);
			}
			
			if(f.isDirectory() && include_subfolders==true) {
				addItemsFromFolder(f);
			}
		}
		
	}
	
	private void addItemFrom(File file) {
		
		CollectionItem item = new CollectionItem();
		item.setValue("name", file.getName());

		if(file.isFile()) item.setValue("type", "file");
		if(file.isDirectory()) item.setValue("type", "folder");

		item.setValue("size", String.valueOf(file.length()));
		item.setValue("uri", file.toURI().toString());
		item.setValue("can_execute", String.valueOf(file.canExecute()));
		item.setValue("can_read", String.valueOf(file.canRead()));
		item.setValue("can_write", String.valueOf(file.canWrite()));
		item.setValue("is_hidden", String.valueOf(file.isHidden()));
		
		Date date = new Date(file.lastModified());
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		item.setValue("date_lastmodified", sdf.format(date));
		
		if(include_fullpath==true) item.setValue("path", file.getPath());
		if(include_fullpath==true) item.setValue("parent", file.getParent());
		
		items.add(item);
	}
	
	private void fetchProperties(Document doc) {
		
		try {
			Node properties = doc.getElementsByTagName("parameters").item(0);
			
			for(int i=0; i<properties.getChildNodes().getLength(); i++) {
				Node n = properties.getChildNodes().item(i);
				
				if(n.getNodeName().equalsIgnoreCase("include_folders")) {
					if(n.getTextContent().equalsIgnoreCase("true")) {
						include_folders = true;
					} else {
						include_folders = false;
					}
					continue;
				}
				
				if(n.getNodeName().equalsIgnoreCase("include_subfolders")) {
					if(n.getTextContent().equalsIgnoreCase("true")) {
						include_subfolders = true;
					} else {
						include_subfolders = false;
					}
					continue;
				}
				
				if(n.getNodeName().equalsIgnoreCase("include_fullpath")) {
					if(n.getTextContent().equalsIgnoreCase("true")) {
						include_fullpath = true;
					} else {
						include_fullpath = false;
					}
					continue;
				}
				
				if(n.getNodeName().equalsIgnoreCase("path")) {
					path = n.getTextContent();
					continue;
				}
			}
			
		} catch(Exception e) { 
			e.printStackTrace();
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
