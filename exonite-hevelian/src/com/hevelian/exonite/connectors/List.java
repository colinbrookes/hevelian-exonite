package com.hevelian.exonite.connectors;

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.hevelian.exonite.core.CollectionItem;
import com.hevelian.exonite.core.Connector;
import com.hevelian.exonite.core.Evaluator;
import com.hevelian.exonite.interfaces.Action;

public class List implements com.hevelian.exonite.interfaces.ConnectorImpl {

	private ArrayList<String> columns = new ArrayList<String>();
	private ArrayList<CollectionItem> items = null;
	private HashMap<String, Action> objects = null;
	private Evaluator evaluator = null;
	
	public List() {
	}
	
	@Override
	public ArrayList<CollectionItem> select() {
		return items;
	}

	@Override
	public ArrayList<CollectionItem> select(Document _doc, Connector connector, HttpServletRequest request, HashMap<String, Action> map) {
		objects = map;
		return select(_doc, connector, request);
	}

	@Override
	public ArrayList<CollectionItem> select(Document _doc, Connector connector,	HttpServletRequest request) {
		
		items = new ArrayList<CollectionItem>();
		evaluator = new Evaluator(request, objects);
		
		columns.add("sequence");
		columns.add("key");
		columns.add("value");
		
		int s = 0;
		Element values = (Element) _doc.getElementsByTagName("values").item(0);
		for(int i=0; i<values.getChildNodes().getLength(); i++) {
			Node node = values.getChildNodes().item(i);

			if(node.getNodeType()!=Element.ELEMENT_NODE) continue;

			CollectionItem item = new CollectionItem();
			item.setValue("sequence", new Integer(s).toString());
			item.setValue("key", node.getNodeName());
			item.setValue("value", evaluator.evaluate(node.getTextContent()));
			
			s++;
			items.add(item);
		}
		return items;
	}

	@Override
	public String update() {
		return null;
	}

	@Override
	public String delete() {
		return null;
	}

	@Override
	public String insert() {
		return null;
	}

	@Override
	public ArrayList<String> getRawColumnNames() {
		return columns;
	}

}
