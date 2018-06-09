package com.hevelian.exonite.utils;

public class NameValuePair {

	private String name 	= null;
	private String value 	= null;
	
	public NameValuePair(String n, String v) {
		name 	= n;
		value 	= v;
	}
	
	public String getName() {
		return name;
	}
	
	public String getValue() {
		return value;
	}
}
