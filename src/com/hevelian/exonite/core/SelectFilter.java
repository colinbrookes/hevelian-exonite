package com.hevelian.exonite.core;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.hevelian.exonite.interfaces.Action;

import javax.servlet.http.HttpServletRequest;

public class SelectFilter {

	private Evaluator evaluator 				= null;
	private ArrayList<WhereClause> wheres		= new ArrayList<WhereClause>();
	
	public SelectFilter(HttpServletRequest request, HashMap<String, Action> objects, Element select) {

		for(int i=0; i<select.getChildNodes().getLength(); i++) {
			Node c = select.getChildNodes().item(i);
			
			if(c.getNodeType()!=Element.ELEMENT_NODE) continue;
			
			/* we have a 'where' clause to build */
			if(c.getNodeName().equalsIgnoreCase("where")) {
				buildWhereArray((Element) c);
			}
			
		}
		
		evaluator = new Evaluator(request, objects);

	}
	
	private void buildWhereArray(Element e) {
		for(int i=0; i<e.getChildNodes().getLength(); i++) {
			Node c = e.getChildNodes().item(i);
			
			if(c.getNodeType()!=Element.ELEMENT_NODE) continue;
			
			if(c.getNodeName().equalsIgnoreCase("and") || c.getNodeName().equalsIgnoreCase("or")) {
				wheres.add(new WhereClause((Element) c));
			}
		}
	}
	
	public boolean matches(CollectionItem item) {
		boolean flagMatches = false;
		
		for(int i=0; i<wheres.size(); i++) {
			WhereClause w = wheres.get(i);
			
			String lhs = evaluator.evaluate(w.lhs, item).toLowerCase();
			String rhs = evaluator.evaluate(w.rhs, item).toLowerCase();
			String con = evaluator.evaluate(w.conjunction, item);
			boolean thisMatches = false;
			
			switch(con) {
			case "equals":
				if(lhs.equalsIgnoreCase(rhs)) thisMatches = true;
				break;
				
			case "contains":
				if(lhs.contains(rhs)) thisMatches = true;
				break;
				
			case "startsWith":
				if(lhs.startsWith(rhs)) thisMatches = true;
				break;
				
			case "notEquals":
				if(!lhs.equalsIgnoreCase(rhs)) thisMatches = true;
				break;
				
			case "notContains":
				if(!lhs.contains(rhs)) thisMatches = true;
				break;
				
			case "notStartsWith":
				if(!lhs.startsWith(rhs)) thisMatches = true;
				break;
				
			}
			
			// now we need to check if this is an 'AND' or an 'OR' and join the results
			if(i==0) {
				flagMatches = thisMatches;
			} else {
				if(w.type.equalsIgnoreCase("or")) {
					flagMatches = flagMatches || thisMatches;
				}
				
				if(w.type.equalsIgnoreCase("and")) {
					flagMatches = flagMatches && thisMatches;
				}
			}
		}
		
		return flagMatches;
	}
}
