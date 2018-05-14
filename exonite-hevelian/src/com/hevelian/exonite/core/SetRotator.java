package com.hevelian.exonite.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Element;

import com.hevelian.exonite.interfaces.Action;

/**
 * A SetRotator converts groups of rows into a single row based on a composiste key.
 * The basic assumption is that a column contains a name and a column contains a value and
 * this is flattened into a key-value pair. The set of key-value-pairs are grouped by the
 * composite key into a single row.
 * 
 * @author cb
 *
 */
public class SetRotator {

	private Evaluator evaluator = null;
	private String NameColumn = null;
	private String ValueColumn = null;
	private String CompositeKey = null;
	private String CarryForward = null;
	private HttpServletRequest request = null;
	
	private String[] KeyParts = null;
	private String _delimiter = "^";
	
	private String[] CarryForwardColumns = null;
	
	public SetRotator(HttpServletRequest request,
			HashMap<String, Action> objects, Element item) {
		
		this.request = request;
		evaluator = new Evaluator(this.request, objects);
		
		NameColumn = evaluator.evaluate(item.getAttribute("nameFrom"));
		ValueColumn = evaluator.evaluate(item.getAttribute("valueFrom"));
		CompositeKey = evaluator.evaluate(item.getAttribute("by"));
		CarryForward = evaluator.evaluate(item.getAttribute("carryForward"));
	}

	/**
	 * run() analyses the item collection and creates a completely new item collection from it.
	 * this is returned to the item collection and should be used for any subsequent instructions
	 * such as filtering and limiting etc.
	 * 
	 * @param items
	 * @return
	 */
	public ArrayList<CollectionItem> run(ArrayList<CollectionItem> items) {
		
		ArrayList<CollectionItem> compositeItems = new ArrayList<CollectionItem>();
		
		CollectionItem SetItem = null;
		CollectionItem item = null;
		String SetKey = null;
		
		Iterator<CollectionItem> it = items.iterator();
		while(it.hasNext()) {
			item = it.next();
			
			String key = CalculateKey(item);
			
			if(SetKey!=null && SetItem!=null && key.equalsIgnoreCase(SetKey)) {
				if(CarryForward!=null) {
					AddCarryForwardColumns(item, SetItem);
				}
			}
				
			if(SetKey==null || SetItem==null || !key.equalsIgnoreCase(SetKey)) {
				
				if(SetItem!=null) { 
					compositeItems.add(SetItem);
				}
				
				SetItem = new CollectionItem();
				SetKey = key;
			}
			
			SetItem.setValue("KEY", key);
			SetItem.setValue(item.getValue(NameColumn), item.getValue(ValueColumn));
		}
		
		if(CarryForward!=null) {
			AddCarryForwardColumns(item, SetItem);
		}
		
		if(SetItem!=null) compositeItems.add(SetItem);
		return compositeItems;
	}

	/**
	 * The collection can specify a list of additional columns to added to the row. Normally these
	 * columns should be identical for all rows in the set because we can only pick one and not add them all.
	 * If the values are not identical, then the value added could, theoreticaly, be any of the possible values.
	 * 
	 * @param itemFrom
	 * @param itemTo
	 */
	private void AddCarryForwardColumns(CollectionItem itemFrom, CollectionItem itemTo) {

		if(CarryForwardColumns==null) {
			CarryForwardColumns = CarryForward.split(",");
			for(int i=0; i<CarryForwardColumns.length; i++) {
				CarryForwardColumns[i] = CarryForwardColumns[i].trim();
			}
		}
		
		for(int i=0; i<CarryForwardColumns.length; i++) {
			try {
				itemTo.setValue(CarryForwardColumns[i], itemFrom.getValue(CarryForwardColumns[i]));
			} catch(Exception e) { }
		}
	}
	
	/**
	 * We extract the field values specified for the composite key and concatenate them
	 * with a delimiter between the values to avoid bleeding between them.
	 * @param item
	 * @return
	 */
	private String CalculateKey(CollectionItem item) {
		String key = "";
		
		if(KeyParts==null) {
			KeyParts = CompositeKey.split(",");
			for(int i=0; i<KeyParts.length; i++) {
				KeyParts[i] = KeyParts[i].trim();
			}
		}
		
		for(int i=0; i<KeyParts.length; i++) {
			key += item.getValue(KeyParts[i]) + _delimiter;
		}
		
		return key;
	}
}
