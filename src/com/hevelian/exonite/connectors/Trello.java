package com.hevelian.exonite.connectors;

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;

import com.hevelian.exonite.core.CollectionItem;
import com.hevelian.exonite.core.Connector;
import com.hevelian.exonite.interfaces.Action;

public class Trello implements com.hevelian.exonite.interfaces.ConnectorImpl {

	private ArrayList<String> columns = new ArrayList<String>();
	private ArrayList<CollectionItem> items = null;
	
	@Override
	public ArrayList<CollectionItem> select() {
		return items;
	}

	@Override
	public ArrayList<CollectionItem> select(Document _doc,
			com.hevelian.exonite.core.Connector connector,
			HttpServletRequest request) {
		return null;
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

	@Override
	public ArrayList<CollectionItem> select(Document _doc, Connector connector,
			HttpServletRequest request, HashMap<String, Action> map) {
		// TODO Auto-generated method stub
		return null;
	}

}
