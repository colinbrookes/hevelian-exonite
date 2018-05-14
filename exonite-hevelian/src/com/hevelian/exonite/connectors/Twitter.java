package com.hevelian.exonite.connectors;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.commons.codec.binary.Base64;

import com.hevelian.exonite.core.CollectionItem;
import com.hevelian.exonite.core.Connector;
import com.hevelian.exonite.core.Evaluator;
import com.hevelian.exonite.utils.J2X;
import com.hevelian.exonite.utils.NameValuePair;
import com.hevelian.exonite.utils.NvpComparator;
import com.hevelian.exonite.interfaces.Action;
import com.hevelian.exonite.interfaces.ConnectorImpl;

public class Twitter implements ConnectorImpl {

	private static final String URL_OAUTH_TOKEN 		= "https://api.twitter.com/oauth2/token";
	private static final String URL_DEFAULT_ENDPOINT	= "/search/tweets.json";
	private static final String DEFAULT_RESPONSE_NODE	= "statuses";
	
	private ArrayList<String> columns 			= new ArrayList<String>();
	private ArrayList<CollectionItem> items 	= null;
	private HashMap<String, Action> objects 	= null;
	private Evaluator evaluator 				= null;
	private String jsonRaw 						= new String();

	private String twitter_api_version 			= null;
	private String twitter_consumer_key 		= null;
	private String twitter_consumer_secret 		= null;
	private String twitter_access_token 		= null;
	private String twitter_token_secret 		= null;
	private String twitter_auth_type			= null;
	
	private String auth_token_type 				= null;
	private String auth_token_value 			= null;
	
	private String response_node				= DEFAULT_RESPONSE_NODE;
	private String url_endpoint					= URL_DEFAULT_ENDPOINT;
	
	private String BaseURLParameters			= "";
	private String BaseURL						= "";
	
	private ArrayList<NameValuePair> urlParams	= new ArrayList<NameValuePair>();
	
	public Twitter() {
		return;
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
	public ArrayList<CollectionItem> select(Document _doc, com.hevelian.exonite.core.Connector connector, HttpServletRequest request) {
		items = new ArrayList<CollectionItem>();
		evaluator = new Evaluator(request, objects);
		
		twitter_auth_type			= connector.getProperty("twitter_auth_type");
		twitter_api_version 		= connector.getProperty("twitter_api_version");

		twitter_consumer_key 		= connector.getProperty("twitter_consumer_key");
		twitter_consumer_secret 	= connector.getProperty("twitter_consumer_secret");
		twitter_access_token 		= connector.getProperty("twitter_access_token");
		twitter_token_secret 		= connector.getProperty("twitter_token_secret");
		
		if(connector.getProperty("endpoint")!=null) 		url_endpoint 	= connector.getProperty("endpoint");
		if(connector.getProperty("response_node")!=null) 	response_node 	= connector.getProperty("response_node");
		
		// we have authenticated so we need to build the query and fetch the data
		BaseURL = "https://api.twitter.com/" + twitter_api_version + url_endpoint;
		BaseURLParameters = "";

		// we now need to build the full URL and fetch the data.
		try {
			Element nodeProperties = (Element) _doc.getElementsByTagName("properties").item(0);
			for(int i=0; i<nodeProperties.getChildNodes().getLength(); i++) {
				Node node = nodeProperties.getChildNodes().item(i);
				
				 if(node.getNodeType()!=Element.ELEMENT_NODE) continue;
				 
				 urlParams.add(new NameValuePair(node.getNodeName(), node.getTextContent()));
				 
				 if(BaseURLParameters.length()==0) {
					 BaseURLParameters = node.getNodeName() + "=" + URLEncoder.encode(evaluator.evaluate(node.getTextContent()), "UTF-8");
				 } else {
					 BaseURLParameters = BaseURLParameters + "&" + node.getNodeName() + "=" + URLEncoder.encode(evaluator.evaluate(node.getTextContent()), "UTF-8");
				 }
				 

			}
		} catch(Exception e) {
		}
		
		try {
			boolean suc = authorise();
			if(suc==false) {
				System.out.println("EXONITE: twitter authorisation failed.");
				return items;
			}
		} catch (Exception e) {
			return items;
		}

		try {
			
			URL url = null;
			HttpsURLConnection con = null;
			
//			System.out.println("TWITTER: URL: " + BaseURL + "?" + BaseURLParameters);
//			System.out.println("TWITTER: AUTH: " + auth_token_value);
			
            if(twitter_auth_type!=null && twitter_auth_type.equalsIgnoreCase("OAuth")) {
            	// we need to do Output too, and send the params as post data
    			url = new URL(BaseURL + "?" + BaseURLParameters);
    			con = (HttpsURLConnection) url.openConnection();
    			con.setRequestMethod("GET");
            } else {
    			url = new URL(BaseURL + "?" + BaseURLParameters);
    			con = (HttpsURLConnection) url.openConnection();
            }

            // DEBUG
            /*
            StringBuffer curl = new StringBuffer();
            curl.append("curl --get '").append(BaseURL).append("'");
            curl.append(" --data '").append(BaseURLParameters).append("'");
            curl.append(" --header 'Authorization: ").append(auth_token_value).append("'");
            curl.append(" --verbose");
            System.out.println("TWITTER: CURL: " + curl.toString());
            */
            
			String inputLine;
			
			BufferedReader in;
			con = (HttpsURLConnection) url.openConnection();
		    con.addRequestProperty("Authorization", auth_token_value);
		    con.setDoInput(true);
			con.setDoOutput(false);
            con.setUseCaches(false);
		    
		    con.connect();
		    
		    in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		    while ((inputLine = in.readLine()) != null) {
		        jsonRaw += inputLine;
		    }
		    in.close();
		    
		} catch(Exception e) {
			return items;
		}
		
		try {
			J2X converter = new J2X();
			Document xml = converter.X(jsonRaw);
			
			if(xml==null) {
				System.out.println("EXONITE: XML DOCUMENT IS NULL AFTER JSON CONVERSION!");
			}
			
			processResult(xml);
			
		} catch(Exception e) {
			return items;
		}
		
		return items;
	}

	private void processResult(Document xml) {
		try {
			Node root = xml.getElementsByTagName(response_node).item(0).getFirstChild();
			NodeList nodes = root.getChildNodes();
			processNodeSet(nodes);
		} catch(Exception e) {
			return;
		}
	}
	
	private void processNodeSet(NodeList nodes) {
		
        for(int i=0; i<nodes.getLength(); i++) {
        	Element e = (Element) nodes.item(i);
			CollectionItem item = new CollectionItem();
			
        	for(int c=0; c<e.getChildNodes().getLength(); c++) {
        		Node node = e.getChildNodes().item(c);
        		
        		if(node.getNodeType()!=Element.ELEMENT_NODE) continue;
        		
        		if(i==0) {
        			columns.add(node.getNodeName());
        		}
        		
        		item.setValue(node.getNodeName(), node.getChildNodes().item(0).getNodeValue());
        		
        		if(countChildElements(node)>0) ProcessChildren(item, node, node.getNodeName(), i);

        	}
        	
			items.add(item);
        }
		
	}
	
	private void ProcessChildren(CollectionItem item, Node node, String prefix, int addColumns) {

		if(node.getChildNodes().getLength()==0) return;
		
		String _prefix = prefix;
		
		for(int i=0; i<node.getChildNodes().getLength(); i++) {
			Node n = node.getChildNodes().item(i);
			
			if(n.getNodeType()!=Element.ELEMENT_NODE) continue;
			
			if(addColumns==0) {
				columns.add(_prefix + "_" + n.getNodeName());
			}
			
			if(n.getChildNodes().item(0)!=null) item.setValue(_prefix + "_" + n.getNodeName(), n.getChildNodes().item(0).getNodeValue());
			if(countChildElements(n)>0) ProcessChildren(item, n, _prefix + "_" + n.getNodeName(), addColumns);
		}
	}
	
	private int countChildElements(Node node) {

		int cnt=0;
		for(int i=0; i<node.getChildNodes().getLength(); i++) {
			if(node.getChildNodes().item(i).getNodeType()==Element.ELEMENT_NODE) cnt++;
		}
		
		return cnt;
	}
	
	/**
	 * Performs appropriate authorisation method. No method specified assumes Basic authentication.
	 * @return
	 * @throws ParserConfigurationException
	 * @throws UnsupportedEncodingException
	 */
	private boolean authorise() throws ParserConfigurationException, UnsupportedEncodingException {
		
		if(twitter_auth_type!=null && twitter_auth_type.equalsIgnoreCase("OAuth")) {
			try {
				return authoriseOAuth();
			} catch(Exception e) {
				return false;
			}
		}
		
		return authoriseBasic();
	}
	
	/**
	 * OAuth is a pain in the but, but hey-ho. This doesnt do an auth call first, it generates the OAuth header
	 * for the main URL call. This is different from Basic auth, that first fetches an auth token and then does the call.
	 * @return
	 * @throws ParserConfigurationException
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	private boolean authoriseOAuth() throws ParserConfigurationException, UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
		
		String oauth_consumer_key			= twitter_consumer_key;
		String oauth_nonce					= generateNonce();
		String oauth_signature				= "";
		String oauth_signature_method		= "HMAC-SHA1";
		String oauth_timestamp				= String.valueOf(System.currentTimeMillis()/1000);
		String oauth_token					= twitter_access_token;
		String oauth_version				= "1.0";
		
		ArrayList<NameValuePair> oauth		= new ArrayList<NameValuePair>();
		
		oauth.add(new NameValuePair("oauth_consumer_key", oauth_consumer_key));
		oauth.add(new NameValuePair("oauth_nonce", oauth_nonce));
		oauth.add(new NameValuePair("oauth_signature_method", oauth_signature_method));
		oauth.add(new NameValuePair("oauth_timestamp", oauth_timestamp));
		oauth.add(new NameValuePair("oauth_token", oauth_token));
		oauth.add(new NameValuePair("oauth_version", oauth_version));
		
		oauth.addAll(urlParams);		// finally, add the url params too.
		Collections.sort(oauth, new NvpComparator());		// and sort the damn thing.
		
		StringBuffer signatureBaseString3 = new StringBuffer();
	    for(int i=0;i<oauth.size();i++)
	    {
	        NameValuePair nvp = oauth.get(i);
	        if (i>0) {
	            signatureBaseString3.append("&");
	        }
	        signatureBaseString3.append(nvp.getName() + "=" + nvp.getValue());
	    }
		
		// encoded:CONSUMER_SECRET & encoded:TOKEN_SECRET
		String key							= URLEncoder.encode(twitter_consumer_secret, "UTF-8") + "&" + URLEncoder.encode(twitter_token_secret, "UTF-8");
		// encoded:GET & encoded:URL & encoded:PARAMS
		String sigString					= URLEncoder.encode("GET", "UTF-8") + "&" + URLEncoder.encode(BaseURL, "UTF-8") + "&" + URLEncoder.encode(signatureBaseString3.toString(), "UTF-8");
		
//		System.out.println("TWITTER: key: " + key);
//		System.out.println("TWITTER: sig: " + sigString);
		
		SecretKey secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA1");
		Mac mac = Mac.getInstance("HmacSHA1");
		
		mac.init(secretKey);
		
		oauth_signature = new String(Base64.encodeBase64(mac.doFinal(sigString.getBytes("UTF-8")))).trim();
				
		String authHdrT = "OAuth oauth_consumer_key=\"%s\", oauth_nonce=\"%s\", oauth_signature=\"%s\", oauth_signature_method=\"%s\", oauth_timestamp=\"%s\", oauth_token=\"%s\", oauth_version=\"%s\"";
	    String authHrd  = String.format(authHdrT, oauth_consumer_key, oauth_nonce, URLEncoder.encode(oauth_signature, "UTF-8"), oauth_signature_method, oauth_timestamp, oauth_token, oauth_version);
	    
//	    System.out.println("TWITTER: authHrd: " + authHrd);
	    
	    auth_token_value = authHrd;
	    
		return true;
	}
	
	/**
	 * Basic authentication is fine for things like search tweets, but not fine for other endpoints.
	 * Probably best to assume you always need to oauth, then you shouldnt have problems.
	 * 
	 * @return
	 * @throws ParserConfigurationException
	 * @throws UnsupportedEncodingException
	 */
	private boolean authoriseBasic() throws ParserConfigurationException, UnsupportedEncodingException {
		String auth_token = twitter_access_token + ":" + twitter_token_secret;
		String auth_token_encoded = new String(Base64.encodeBase64(auth_token.getBytes("UTF-8")));
		String body = "grant_type=client_credentials";
        String buffer = "";

//        System.out.println("TWITTER: auth token: " + auth_token);
//       System.out.println("TWITTER: encoded: " + auth_token_encoded);
        
        try {
    		OutputStream outStream = null;
            InputStream inStream = null;
            
            URL url = new URL(URL_OAUTH_TOKEN);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            
            urlConnection.addRequestProperty("Authorization", "Basic " + auth_token_encoded);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            urlConnection.setFixedLengthStreamingMode(body.length());
            
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);

            urlConnection.connect();
            
            outStream = urlConnection.getOutputStream();
            outStream.write(body.getBytes("UTF-8"));
            outStream.flush();
            outStream.close();
     
            // Get Response
            inStream = urlConnection.getInputStream();
            String buf;
            BufferedReader b = new BufferedReader(new InputStreamReader(inStream));
            while((buf = b.readLine()) != null) {
            	buffer += buf;
            }
     
            inStream.close();
        	
        } catch(Exception e) {
        	return false;
        }
        
//        System.out.println("TWITTER: we got: " + buffer);
        
        try {
    		J2X converter = new J2X();
    		Document xml = converter.X(buffer);
    		auth_token_type = xml.getElementsByTagName("token_type").item(0).getTextContent();
    		auth_token_value = "Bearer " + xml.getElementsByTagName("access_token").item(0).getTextContent();
    		
//    		System.out.println("TWITTER: auth_token_type: " + auth_token_type);
//    		System.out.println("TWITTER: auth_token_value: " + auth_token_value);

    		if(auth_token_type.equalsIgnoreCase("bearer")) return true;
    		return false;
        	
        } catch(Exception e) {
        	return false;
        }
	}
	
	private String generateNonce() {
	    Random gen = new Random(System.currentTimeMillis());
	    StringBuilder nonceBuilder = new StringBuilder("");
	    String base = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	    int baseLength = base.length();

	    // Taking random word characters
	    for (int i = 0; i < 32; ++i) {
	        int position = gen.nextInt(baseLength);
	        nonceBuilder.append(base.charAt(position));
	    }

	    String nonce = null;
		try {
//			nonce = new String(Base64.encodeBase64((nonceBuilder.toString().getBytes("UTF-8"))));
			nonce = nonceBuilder.toString();
		} catch (Exception e) {
		}

	    return nonce;
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
