package com.hevelian.exonite.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.hevelian.exonite.accesscontrol.Session;
import com.hevelian.exonite.interfaces.Action;

public class Evaluator {

	HttpServletRequest request 			= null;
	Session session 					= null;
	HashMap<String, Action> objects 	= null;
	
	ScriptEngineManager factory 		= new ScriptEngineManager();
	ScriptEngine engine 				= factory.getEngineByName("JavaScript");
	
	public Evaluator(HttpServletRequest request) {
		this.request = request;		
	}
	
	public Evaluator(HttpServletRequest request, Session session) {
		this.request = request;
		this.session = session;
	}

	public Evaluator(HttpServletRequest request, HashMap<String, Action> objects) {
		this.request = request;
		this.objects = objects;
	}

	public Evaluator(HashMap<String, Action> objects) {
		this.objects = objects;
	}
	
	public String evaluateScript(String str, CollectionItem item, ArrayList<CollectionItem> items) {
		try {
			engine.put("item", item);
			engine.put("items",  items);
			if(objects!=null) engine.put("objects", objects);
			engine.eval(str);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return (String) engine.get("result").toString();
	}
	
	public String evaluate(String str) {
		return evaluate(str, null);
	}
	
	public String evaluate(String str, CollectionItem data) {
		StringTokenizer tok = new StringTokenizer(str, "|");
		String evaluated = "";
		
		while(tok.hasMoreElements()) {
			String fragment = tok.nextToken();
			String parts[] = fragment.split("\\.");
			
			if(parts.length==2) {
				
				boolean match = false;
				
				try {
					if(parts[0].equalsIgnoreCase("get") && request!=null) {
						evaluated = evaluated.concat(request.getParameter(parts[1]));
						match = true;
					}
					
					if(parts[0].equalsIgnoreCase("post") && request!=null) {
						evaluated = evaluated.concat(request.getParameter(parts[1]));
						match = true;
					}
					
					if(parts[0].equalsIgnoreCase("data") && data!=null) {
						evaluated = evaluated.concat(data.getValue(parts[1]));
						match = true;
					}
					
					if(parts[0].equalsIgnoreCase("session") && session!=null) {
						evaluated = evaluated.concat(session.getProperty(parts[1]));
						match = true;
					}
					
					if(parts[0].equalsIgnoreCase("request") && request!=null) {
						evaluated = evaluated.concat((String) request.getSession(true).getAttribute(parts[1]));
						match = true;
					}
					
					if(parts[0].equalsIgnoreCase("cookie") && request!=null && request.getCookies()!=null) {
						Cookie[] cookies = request.getCookies();
						for(Cookie ck : cookies) {
							if(ck.getName().equals(parts[1])) {
								evaluated = evaluated.concat(ck.getValue());
							}
						}
						match = true;
					}
					
					// add 'object' evaluator if objects are available and we didnt already match on something built-in
					if(match==false && objects!=null) {
						Action action = objects.get(parts[0]);
						if(action!=null) {
							evaluated = evaluated.concat(action.getProperty(parts[1]));
							match = true;
						}
					}
					
					// we didnt find a match so we just add the fragment as is.
					if(match==false) {
						evaluated = evaluated.concat(fragment);						
					}

				} catch(Exception e) {}
				
			} else {
				evaluated = evaluated.concat(fragment);
			}
		}
		
		return evaluated;
	}
}
