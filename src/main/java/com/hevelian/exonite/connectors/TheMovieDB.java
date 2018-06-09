package com.hevelian.exonite.connectors;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.hevelian.exonite.core.CollectionItem;
import com.hevelian.exonite.core.Connector;
import com.hevelian.exonite.core.Evaluator;
import com.hevelian.exonite.interfaces.Action;
import com.hevelian.exonite.interfaces.ConnectorImpl;
import com.hevelian.exonite.utils.J2X;

// example:
// https://api.themoviedb.org/3/discover/movie?with_genres=18&primary_release_year=2014&api_key=8c4774392ba9fe13a83c9f1438a77b2e	- movie query
// https://api.themoviedb.org/3/person/1181313-maisie-williams?api_key=8c4774392ba9fe13a83c9f1438a77b2e								- person
// https://api.themoviedb.org/3/person/1181313-maisie-williams/movie_credits?api_key=8c4774392ba9fe13a83c9f1438a77b2e				- movie credits
// https://api.themoviedb.org/3/person/1181313-maisie-williams/tv_credits?api_key=8c4774392ba9fe13a83c9f1438a77b2e					- tv credits
// https://api.themoviedb.org/3/person/1181313-maisie-williams/images?api_key=8c4774392ba9fe13a83c9f1438a77b2e						- images

public class TheMovieDB implements ConnectorImpl {

	private final static String API_URL		= "https://api.themoviedb.org/";
	
	private ArrayList<String> columns 		= new ArrayList<String>();
	private ArrayList<CollectionItem> items = null;
	private Evaluator evaluator 			= null;
	private HashMap<String, Action> objects = null;
	private String jsonRaw 					= new String();
	
	DocumentBuilderFactory dbFactory 		= null;
	DocumentBuilder dBuilder 				= null;

	public TheMovieDB() {
		try {
			dbFactory = DocumentBuilderFactory.newInstance();
			dBuilder = dbFactory.newDocumentBuilder();
		} catch(Exception e) {
			return;
		}

	}
	@Override
	public ArrayList<CollectionItem> select() {
		if(items==null) items = new ArrayList<CollectionItem>();
		return items;
	}

	@Override
	public ArrayList<CollectionItem> select(Document _doc, Connector connector, HttpServletRequest request) {

		items = new ArrayList<CollectionItem>();
		evaluator = new Evaluator(request, objects);

		String api_key 			= connector.getProperty("api_key");
		String api_version 		= connector.getProperty("api_version");
		
		String queryType 		= _doc.getElementsByTagName("type").item(0).getTextContent();			// i.e. find
		String queryTopic 		= _doc.getElementsByTagName("topic").item(0).getTextContent();			// i.e. movies
		String queryFilter 		= _doc.getElementsByTagName("q").item(0).getTextContent();				// i.e. with_genres=18&primary_release_year=2014
		
		String BaseURL = API_URL + api_version + "/" + queryType + "/" + evaluator.evaluate(queryTopic);
		String BaseURLParameters = "?api_key=" + api_key;
		if(queryFilter.length()>0) BaseURLParameters += "&" + evaluator.evaluate(queryFilter);
		
		try {
			// try to fetch data and process the resulting JSON into XML
			URL url = new URL(BaseURL + BaseURLParameters);
			String inputLine;
			
			BufferedReader in;
		    URLConnection con = url.openConnection();
		    in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		    while ((inputLine = in.readLine()) != null) {
		        jsonRaw += inputLine;
		    }
		    in.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		try {
			J2X converter = new J2X();
			Document xml = converter.X(jsonRaw);
			
			System.out.println("JSON RAW: " + jsonRaw);
			
			if(xml==null) {
				System.out.println("EXONITE: XML DOCUMENT IS NULL AFTER JSON CONVERSION!");
			}
			
			// DEBUG: send XML to console
			
			try {
				String buf = J2X.prettyPrint(xml);
				System.out.println("EXONITE: MOVIEDB XML: " + buf);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
//			processResults(xml);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return items;
	}

	private void processResults(Document doc) {
		Element results = (Element) doc.getElementsByTagName("results").item(0);
		Element e = (Element) results.getElementsByTagName("e").item(0);
	}
	
	@Override
	public ArrayList<CollectionItem> select(Document _doc, Connector connector, HttpServletRequest request, HashMap<String, Action> map) {
		objects = map;
		return select(_doc, connector, request);
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
