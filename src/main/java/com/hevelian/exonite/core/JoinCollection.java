package com.hevelian.exonite.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.hevelian.exonite.interfaces.Action;

/**
 * JoinCollection is a simple join function based on matching unique primary keys.
 * 
 * @author cb
 *
 */
public class JoinCollection {

	private String collectionName = null;
	private Collection collection = null;
	
	private String fromKey = null;
	private String toKey = null;
	private String usePrefix = null;
	
	public JoinCollection(HttpServletRequest request, HashMap<String, Action> objects, Element node) {
		
		collectionName = node.getAttribute("toCollection");
		fromKey = node.getAttribute("fromKey");
		toKey = node.getAttribute("toKey");
		usePrefix = node.getAttribute("usePrefix");

		collection = new Collection(collectionName, request);
	}

	public ArrayList<CollectionItem> run(ArrayList<CollectionItem> items) throws ParserConfigurationException, SAXException, IOException {
		
		ArrayList<CollectionItem> toCollectionRaw = collection.selectRaw();

		for(int i=0; i<items.size(); i++) {
			CollectionItem item = items.get(i);

			for(int n=0; n<toCollectionRaw.size(); n++) {
				if(item.getValue(fromKey).equalsIgnoreCase(toCollectionRaw.get(n).getValue(toKey))) {
					CollectionItem itemFrom = toCollectionRaw.get(n);
					ArrayList<String> columns = itemFrom.getColumns();
					for(int c=0; c<columns.size(); c++) {
						item.setValue(usePrefix + columns.get(c), itemFrom.getValue(columns.get(c)));
					}
					break;
				}
			}
			
		}
		
		return items;
	}
}
