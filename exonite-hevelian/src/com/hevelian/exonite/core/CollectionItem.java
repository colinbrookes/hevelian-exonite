package com.hevelian.exonite.core;

import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class CollectionItem {

	private ArrayList<String> columns = new ArrayList<String>();
	private ArrayList<String> values = new ArrayList<String>();
	
	public CollectionItem() {
		
	}
	
	public CollectionItem(Element xml) {
		
		for(int i=0; i<xml.getChildNodes().getLength(); i++) {
			Node child = xml.getChildNodes().item(i);
			if(child.getNodeType()!=Element.ELEMENT_NODE) continue;
			
			setValue(child.getNodeName(), child.getTextContent());
		}
	}
	
	public void setValue(String column, String value) {
		
		// TODO: strip anything that is not alphanumeric and underscore
		String tweakedColumn = column.replace(" ", "_");
		
		for(int i=0; i<columns.size(); i++) {
			if(columns.get(i).toString().equals(tweakedColumn)) {
				values.set(i, value);
				return;
			}
		}
		
		/* previously unknown column, so we add it */
		columns.add(tweakedColumn);
		values.add(value);
	}
	
	public String getValue(String column) {
		String tweakedColumn = column.replace(" ", "_");
		
		for(int i=0; i<columns.size(); i++) {
			if(columns.get(i).equals(tweakedColumn)) {
				return values.get(i);
			}
		}
		
		return "";
	}
	
	public ArrayList<String> getValues() {
		return values;
	}

	public ArrayList<String> getColumns() {
		return columns;
	}
}
