package com.hevelian.exonite.scim;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;

import com.hevelian.exonite.utils.J2X;

/**
 * wso2is SCIM connector class. Uses basic auth over ssl.
 * 
 * You can request both Users and Groups, in JSON or XML string format.
 * The 'filter' option is also supported, as is requesting a specific ID.
 * 
 */
public class SCIMReader {

	public static final String GROUP_ENDPOINT		= "group_endpoint";
	public static final String USER_ENDPOINT		= "user_endpoint";
	public static final String USERNAME				= "username";
	public static final String PASSWORD				= "password";
	public static final String FETCH				= "fetch";
	public static final String FILTER				= "filter";
	public static final String ID					= "id";
	public static final String FORMAT				= "format";
	public static final String CACHE				= "cache";
	
	public static final String USERS				= "users";
	public static final String GROUPS				= "groups";
	public static final String AS_JSON				= "json";
	public static final String AS_XML_STRING		= "xml";
	
	private HashMap<String, String> connectionProperties = new HashMap<String, String>();
	private HashMap<String, String> requestProperties = new HashMap<String, String>();
	
	public SCIMReader(HashMap<String, String> props) {
		connectionProperties = props;
	}
	
	/**
	 * Fetches the data from the SCIM server and returns it in the requested format.
	 */
	public String read(HashMap<String, String> props) throws Exception {
		requestProperties = props;
		
		String data = null;
		Document doc = null;
		
		if(requestProperties.get(FETCH).equalsIgnoreCase(GROUPS)) {
			data = fetchFromEndpoint(connectionProperties.get(GROUP_ENDPOINT));
		} else if(requestProperties.get(FETCH).equalsIgnoreCase(USERS)) {
			data = fetchFromEndpoint(connectionProperties.get(USER_ENDPOINT));
		} else {
			System.out.println("Unknown Fetch Type: " + requestProperties.get(FETCH));
			return null;
		}
		
		if(requestProperties.get(FORMAT).equalsIgnoreCase(AS_JSON)) return data;
		
		// create w3c DOM document from the JSON data so we can pretty-print it.
		J2X parser = new J2X();
		doc = parser.X(data);
		
		if(requestProperties.get(FORMAT).equalsIgnoreCase(AS_XML_STRING)) return J2X.prettyPrint(doc);
		
		return null;
	}
	
	/**
	 * Calls the end-point and returns the raw JSON data.
	 */
	private String fetchFromEndpoint(String endpoint) throws Exception {
		StringBuffer sb_url = new StringBuffer();
		String jsonRaw = new String();
		
		sb_url.append(endpoint);
		sb_url.append(buildURL());
		
		URL url = new URL(sb_url.toString());
		
	    URLConnection con = url.openConnection();
	    con.setRequestProperty("Authorization", auth());
	    con.setRequestProperty("Accept", "*/*");
	    con.setDoInput(true);
	    con.setReadTimeout(0);
	    con.connect();
	    
		String inputLine;
		BufferedReader in;
	    in = new BufferedReader(new InputStreamReader(con.getInputStream()));
	    while ((inputLine = in.readLine()) != null) {
	        jsonRaw += inputLine;
	    }
	    in.close();
		return jsonRaw;
		
	}
	
	/**
	 * Adds additional ID and filter parameters to the base URL.
	 * @return
	 */
	private String buildURL() {
		StringBuffer sb = new StringBuffer();
		
		// if ID was supplied, then add this to the URL.
		if(requestProperties.get(ID)!=null && requestProperties.get(ID).length()>0) {
			sb.append("/");
			sb.append(requestProperties.get(ID));
		}
		
		// if a FILTER was supplied, then add this to the URL too.
		if(requestProperties.get(FILTER)!=null && requestProperties.get(FILTER).length()>0) {
			sb.append("?filter=");
			sb.append(requestProperties.get(FILTER));
		}
		
		return sb.toString();
	}
	
	/**
	 * Creates the Basic Authorization string for the HTTP header.
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private String auth() throws UnsupportedEncodingException {
		StringBuffer sb = new StringBuffer();
		sb.append(connectionProperties.get(USERNAME));
		sb.append(":");
		sb.append(connectionProperties.get(PASSWORD));
		
		return "Basic " + Base64.encodeBase64URLSafeString(sb.toString().getBytes("UTF-8")) + "=";
	}
}
