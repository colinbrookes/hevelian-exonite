package com.hevelian.exonite.connectors;

import com.hevelian.exonite.core.CollectionItem;
import com.hevelian.exonite.core.Connector;
import com.hevelian.exonite.core.Evaluator;
import com.hevelian.exonite.interfaces.Action;

import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import org.w3c.dom.*;

import java.sql.*;
import java.util.ArrayList;
// import java.util.Enumeration;
import java.util.HashMap;

public class Database implements com.hevelian.exonite.interfaces.ConnectorImpl {

	private ArrayList<String> columns = new ArrayList<String>();
	private ArrayList<CollectionItem> items = null;
	private Evaluator evaluator = null;
	private HashMap<String, Action> objects = null;

	private DataSource ds;
	private Connection conn;
	private Statement stmt;
	private ResultSet rs;
	
	public Database() {
	}
	
	@Override
	public ArrayList<CollectionItem> select(Document _doc, Connector connector,	HttpServletRequest request, HashMap<String, Action> map) {
		objects = map;
		return select(_doc, connector, request);
	}

	@Override
	public ArrayList<CollectionItem> select(Document _doc, Connector connector, HttpServletRequest request) {
		
		items = new ArrayList<CollectionItem>();
		String query = _doc.getElementsByTagName("query").item(0).getTextContent();
		String type = connector.getProperty("type");
		
		// check for variable substitution in the query string
		evaluator = new Evaluator(request, objects);
		query = evaluator.evaluate(query);
		
		try {
			if(type.equalsIgnoreCase("jndi")) {
				InitialContext ctx = new InitialContext();
			    ds = (DataSource) ctx.lookup(connector.getProperty("jndi_name"));
			    conn = ds.getConnection();
			}
			
			if(type.equalsIgnoreCase("jdbc")) {
				String jdbc_driver = connector.getProperty("jdbc_driver");
				String jdbc_url = connector.getProperty("jdbc_url");
				String jdbc_username = connector.getProperty("jdbc_username");
				String jdbc_password = connector.getProperty("jdbc_password");
				
				Class.forName(jdbc_driver);
				conn = DriverManager.getConnection (jdbc_url, jdbc_username, jdbc_password);
			}
			
		    stmt = conn.createStatement();
		    rs = stmt.executeQuery(query);
		    
		} catch(Exception e) {
		}
		
		try {
			
			while(rs.next()) {
				
				if(columns.size()==0) {
					ResultSetMetaData rsm = rs.getMetaData();
					for(int i=1; i<=rsm.getColumnCount(); i++) {
						columns.add(rsm.getColumnName(i));
					}
					
				}
				
				CollectionItem item = new CollectionItem();
				for(int i=0; i<columns.size(); i++) {
					String value = rs.getString(columns.get(i));
					item.setValue(columns.get(i), value);
				}
				
				items.add(item);
			}
			
		} catch(Exception e) {
			try { if(rs!=null) rs.close();		} catch(Exception es) {}
			try { if(stmt!=null) stmt.close();	} catch(Exception es) {}
			try { if(conn!=null) conn.close();	} catch(Exception es) {}
			
			e.printStackTrace();
		}
		
		/* cleanup database stuff */
		try { if(rs!=null) rs.close();		} catch(Exception es) {}
		try { if(stmt!=null) stmt.close();	} catch(Exception es) {}
		try { if(conn!=null) conn.close();	} catch(Exception es) {}
		
		return items;
	}
	
	public ArrayList<CollectionItem> select() {
		return items;
	}

	public String update() {
		return "";
	}
	
	public String delete() {
		return "";
	}
	
	public String insert() {
		return "";
	}

	public ArrayList<String> getRawColumnNames() {
		return columns;
	}

}
