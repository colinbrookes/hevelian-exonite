package com.hevelian.exonite.core;

import java.util.ArrayList;
import java.util.HashMap;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Element;

import com.hevelian.exonite.interfaces.Action;

/**
 * JoinCollection is a simple join function based on matching unique primary keys.
 * 
 * @author cb
 *
 */
public class SetPreprocessor {

	ScriptEngineManager factory 		= new ScriptEngineManager();
	ScriptEngine engine 				= factory.getEngineByName("JavaScript");

	private String script = null;
	
	public SetPreprocessor(HttpServletRequest request, HashMap<String, Action> objects, Element node) {
		
		script = node.getTextContent();
	}

	/**
	 * We call the select method on the joined-to collection and parse the xml response into a set of items. 
	 * (not very efficient actually, better to get the 'items' set instead).
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<CollectionItem> run(ArrayList<CollectionItem> items) {
		
		try {
			engine.put("items",  items);
			engine.eval(script);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return (ArrayList<CollectionItem>) engine.get("items");
	}

}
