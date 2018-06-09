package com.hevelian.exonite.interfaces;

import java.util.HashMap;

import org.w3c.dom.Element;

public interface Action {

	public void run(Element child, HashMap<String, Action> map);
	public Element getRecord();
	public String getProperty(String name);
	public int size();
	public void next();
}
