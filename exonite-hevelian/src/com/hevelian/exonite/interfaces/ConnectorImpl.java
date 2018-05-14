package com.hevelian.exonite.interfaces;

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import org.w3c.dom.Document;
import com.hevelian.exonite.core.CollectionItem;
import com.hevelian.exonite.core.Connector;
 
public interface ConnectorImpl {

	/* main CRUD functions */
	public ArrayList<CollectionItem> select();
	public ArrayList<CollectionItem> select(Document _doc, Connector connector, HttpServletRequest request);
	public ArrayList<CollectionItem> select(Document _doc, Connector connector, HttpServletRequest request, HashMap<String, Action> map);
	public String update();
	public String delete();
	public String insert();
	
	/* accessory functions */
	public ArrayList<String> getRawColumnNames();
}
