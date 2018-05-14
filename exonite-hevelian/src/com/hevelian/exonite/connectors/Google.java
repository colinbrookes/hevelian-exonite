package com.hevelian.exonite.connectors;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;

import com.hevelian.exonite.core.CollectionItem;
import com.hevelian.exonite.core.Connector;
import com.hevelian.exonite.core.Evaluator;
import com.hevelian.exonite.google.GoogleAuthenticator;
import com.hevelian.exonite.interfaces.Action;
import com.hevelian.exonite.interfaces.ConnectorImpl;

public class Google implements ConnectorImpl {

	private ArrayList<String> columns = new ArrayList<String>();
	private ArrayList<CollectionItem> items = null;
	private HashMap<String, Action> objects = null;
	@SuppressWarnings("unused")
	private Evaluator evaluator = null;
	private GoogleAuthenticator authenticator = null;
	@SuppressWarnings("unused")
	private String jsonRaw = new String();
	
	private static String google_auth_type = null;
	private static String google_iss = null;
	private static String google_scope = null;
	private static String google_private_key = null;
	private static String google_key_password = null;
	
	private static String google_client_id = null;
	private static String google_client_secret = null;
	private static String google_refresh_token = null;
	
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
	public ArrayList<CollectionItem> select(Document _doc, Connector connector, HttpServletRequest request) {
		items = new ArrayList<CollectionItem>();
		evaluator = new Evaluator(request, objects);
		authenticator = new GoogleAuthenticator();
		
		// get the connector properties
		google_auth_type 		= connector.getProperty("google_auth_type");
		google_iss 				= connector.getProperty("google_iss");
		google_scope 			= connector.getProperty("google_scope");
		google_private_key 		= connector.getProperty("google_private_key");
		google_key_password 	= connector.getProperty("google_key_password");
		google_client_id 		= connector.getProperty("google_client_id");
		google_client_secret 	= connector.getProperty("google_client_secret");
		google_refresh_token 	= connector.getProperty("google_refresh_token");
		
		try {
			String suc = authorise();
			if(suc==null) {
				System.out.println("EXONITE: google authorisation failed.");
				return items;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return items;
		}
		
		return items;
	}

	/**
	 * Perform google oauth2 authentication and authorisation and get the bearer token
	 * @return
	 * @throws Exception 
	 * @throws UnsupportedEncodingException
	 */
	private String authorise() throws Exception {

		if(google_auth_type.equalsIgnoreCase("service")) {
			String auth_token = authenticator.authenticateService(google_iss, google_scope, google_private_key, google_key_password);
			return auth_token;
		}
		
		if(google_auth_type.equalsIgnoreCase("application")) {
			String auth_token = authenticator.authenticateApplication(google_client_id, google_client_secret, google_refresh_token);
			return auth_token;
		}
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

}
