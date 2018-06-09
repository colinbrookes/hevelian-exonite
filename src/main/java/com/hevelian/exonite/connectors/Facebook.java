package com.hevelian.exonite.connectors;

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;

import com.hevelian.exonite.core.CollectionItem;
import com.hevelian.exonite.core.Connector;
import com.hevelian.exonite.core.Evaluator;
import com.hevelian.exonite.interfaces.Action;
import com.hevelian.exonite.interfaces.ConnectorImpl;
import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.FacebookClient.AccessToken;
import com.restfb.Version;
import com.restfb.types.Page;
import com.restfb.types.User;

/**
 * Facebook Connector
 * You can retrieve:
 * 		account, timeline, albums, friends, links, videos
 * 
 * Assuming the user has allowed this, and your app token is enabled for this.
 * 
 * @author cb
 *
 */
public class Facebook implements ConnectorImpl {

	private ArrayList<String> columns 			= new ArrayList<String>();
	private ArrayList<CollectionItem> items 	= null;
	private HashMap<String, Action> objects 	= null;
	private Evaluator evaluator 				= null;

	private String facebook_api_version 		= null;
	private String facebook_app_id 				= null;
	private String facebook_app_secret 			= null;
	
	@Override
	public ArrayList<CollectionItem> select() {
		return items;
	}

	@Override
	public ArrayList<CollectionItem> select(Document _doc, Connector connector, HttpServletRequest request) {
		items = new ArrayList<CollectionItem>();
		evaluator = new Evaluator(request, objects);
		
		facebook_api_version		= connector.getProperty("api_version");
		facebook_app_id				= connector.getProperty("app_id");
		facebook_app_secret			= connector.getProperty("app_secret");
		
		AccessToken accessToken = new DefaultFacebookClient(Version.VERSION_2_6).obtainAppAccessToken(facebook_app_id, facebook_app_secret);
		FacebookClient facebookClient = new DefaultFacebookClient(accessToken.getAccessToken(), Version.VERSION_2_6);
//		User user = facebookClient.fetchObject("me", User.class);
//		Page page = facebookClient.fetchObject("me", Page.class);
	
		System.out.println("FACEBOOK ACCESS TOKEN: " + accessToken.getAccessToken() + ", TYPE: " + accessToken.getTokenType());
		
		Connection<Page> fetchConnection = facebookClient.fetchConnection("colin.hevelian/likes", Page.class);

	    for ( Page page : fetchConnection.getData() )
	    {
	        System.out.println( "LIKES NAME: " + page.getName() );
	    }

	    return items;
	}

	@Override
	public ArrayList<CollectionItem> select(Document _doc, Connector connector, HttpServletRequest request,
			HashMap<String, Action> map) {
		objects = map;
		return select(_doc, connector, request);
	}

	@Override
	public String update() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String delete() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String insert() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<String> getRawColumnNames() {
		return columns;
	}

}
