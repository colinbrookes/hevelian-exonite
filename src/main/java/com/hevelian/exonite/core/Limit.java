package com.hevelian.exonite.core;

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Element;

import com.hevelian.exonite.interfaces.Action;

public class Limit {

	private String fetch = null;
	private String offset = null;
	
	private HttpServletRequest request = null;
	private Evaluator evaluator = null;

	public Limit(HttpServletRequest request, HashMap<String, Action> objects,
			Element item) {
		
		this.request  = request;
		evaluator = new Evaluator(this.request, objects);
		
		fetch = evaluator.evaluate(item.getAttribute("fetch"));
		offset = evaluator.evaluate(item.getAttribute("offset"));
	}
	
	/**
	 * Some connectors do not support the ability to return a 'page' of records. Where a database connector
	 * is used that does support it, you should use the database engine to limit the data set.
	 * 
	 * If the offset is greater than size then we return an empty set.
	 * If the offset+fetch is greater than size then we return the remaining records.
	 * Otherwise we return exactly 'fetch' records starting at offset.
	 * 
	 * @param items
	 * @return
	 */
	public ArrayList<CollectionItem> run(ArrayList<CollectionItem> items) {
		
		ArrayList<CollectionItem> subsetItems = new ArrayList<CollectionItem>();
		int start = Integer.parseInt(offset);
		int end = start + Integer.parseInt(fetch);
		int size = items.size();
		
		
		if(start>=size) return subsetItems;
		
		for(int i=start; i<end; i++) {
			if(i>=size) return subsetItems;
			subsetItems.add(items.get(i));
		}
		return subsetItems;
	}

	public ArrayList<CollectionItem> run(ArrayList<CollectionItem> items, SelectFilter filter) {
		
		ArrayList<CollectionItem> subsetItems = new ArrayList<CollectionItem>();
		int start = Integer.parseInt(offset);
		int end = start + Integer.parseInt(fetch);
		int cnt = end - start;
		int matched = 0;
		
		// if(filter!=null && !filter.matches(item)) continue;
		
		for(int i=0; i<items.size(); i++) {
			if(filter!=null && !filter.matches(items.get(i))) continue;
			
			matched++;
			
			if(matched>=start) {
				subsetItems.add(items.get(i));
			}
			
			if(subsetItems.size()>=cnt) {
				return subsetItems;
			}
		}
		
		return subsetItems;
	}
}
