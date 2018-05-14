package com.hevelian.exonite.core;

import org.w3c.dom.Element;

public class WhereClause {

	public String type 				= null;
	public String conjunction 		= null;
	public String lhs				= null;
	public String rhs				= null;
	
	public WhereClause(Element e) {
		type		= e.getNodeName();
		conjunction = e.getAttribute("con");
		lhs			= e.getAttribute("left");
		rhs			= e.getAttribute("right");
	}
	
}
