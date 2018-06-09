package com.hevelian.exonite.google;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;

import com.hevelian.exonite.utils.J2X;

public class GoogleAuthenticator {

	public static String google_iss 			= null;
	public static String google_scope 			= null;
	public static String google_private_key 	= null;
	public static String google_key_password 	= null;
	
	private static final String JWT_HEADER 		= "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
	private static final String GOOGLE_AUD 		= "https://accounts.google.com/o/oauth2/token";
	private static final String GRANT_TYPE 		= "urn:ietf:params:oauth:grant-type:jwt-bearer";
	private static final String TOKEN_URL 		= "https://accounts.google.com/o/oauth2/token";
	
	
	/*
	 * for Application authentication ...
	 * we need to capture the auth token and then the refresh token - use this (change the  client id)
	 * https://accounts.google.com/o/oauth2/auth?scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fyoutube.upload&redirect_uri=http://localhost&response_type=code&client_id=131114705420-a5can9gno2sbejo1fptl0eagigsia88s.apps.googleusercontent.com&access_type=offline&pageId=none
	 * 
	 * then we need to get the refresh_token: we do this:
	 * curl --request POST --header 'Content-Type: application/x-www-form-urlencoded' --data 'code=4/bDt2oWuqUe0rvFAhjg_XNq6A8aZM.8lA04TNGPKIdOl05ti8ZT3av4UmTiAI&client_id=131114705420-a5can9gno2sbejo1fptl0eagigsia88s.apps.googleusercontent.com&client_secret=5RNppM1blEnSeQ7Mo63YhAcd&redirect_uri=http://localhost&grant_type=authorization_code' https://accounts.google.com/o/oauth2/token --verbose
	 */
	public GoogleAuthenticator() {
		return;
	}
	
	public String authenticateApplication(String client_id, String client_secret, String refresh_token) throws IOException {
		StringBuffer postData = new StringBuffer();
		
		postData.append("client_id=").append(client_id).append("&");
		postData.append("client_secret=").append(client_secret).append("&");
		postData.append("refresh_token=").append(refresh_token).append("&");
		postData.append("grant_type=refresh_token");
		
        String buffer = "";
		
		OutputStream outStream = null;
        InputStream inStream = null;
        try {
            
            URL url = new URL(TOKEN_URL);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setFixedLengthStreamingMode(postData.length());
            
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);

            urlConnection.connect();
            
            outStream = urlConnection.getOutputStream();
            outStream.write(postData.toString().getBytes("UTF-8"));
            outStream.flush();
     
            // Get Response
            inStream = urlConnection.getInputStream();
            String buf;
            BufferedReader b = new BufferedReader(new InputStreamReader(inStream));
            while((buf = b.readLine()) != null) {
            	buffer += buf;
            }
     
            outStream.close();
            inStream.close();
        	
        } catch(Exception e) {
        	e.printStackTrace();
            outStream.close();
        	return null;
        }
		
        System.out.println("APP AUTH RESPONSE: " + buffer);
        
        try {
    		J2X converter = new J2X();
    		Document xml = converter.X(buffer);
    		String auth_token_type = xml.getElementsByTagName("token_type").item(0).getTextContent();
    		String auth_token_value = xml.getElementsByTagName("access_token").item(0).getTextContent();
    		
    		if(auth_token_type.equalsIgnoreCase("bearer")) return auth_token_value;
    		return null;
        	
        } catch(Exception e) {
        	e.printStackTrace();
        	return null;
        }
		
	}
	
	public String authenticateService(String iss, String scope, String private_key, String password) throws IOException {
	
		google_iss 				= iss;
		google_scope 			= scope;
		google_private_key 		= private_key;
		google_key_password 	= password;
		
		String auth_token = createServiceJWT();
		String postData = "grant_type=" + URLEncoder.encode(GRANT_TYPE, "UTF-8") + "&assertion=" + auth_token;
        String buffer = "";
		
		OutputStream outStream = null;
        InputStream inStream = null;
        try {
            
            URL url = new URL(TOKEN_URL);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setFixedLengthStreamingMode(postData.length());
            
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);

            urlConnection.connect();
            
            outStream = urlConnection.getOutputStream();
            outStream.write(postData.getBytes("UTF-8"));
            outStream.flush();
     
            // Get Response
            inStream = urlConnection.getInputStream();
            String buf;
            BufferedReader b = new BufferedReader(new InputStreamReader(inStream));
            while((buf = b.readLine()) != null) {
            	buffer += buf;
            }
     
            outStream.close();
            inStream.close();
        	
        } catch(Exception e) {
        	e.printStackTrace();
            outStream.close();
        	return null;
        }
        
        System.out.println("SERVICE AUTH RESPONSE: " + buffer);
        
        try {
    		J2X converter = new J2X();
    		Document xml = converter.X(buffer);
    		String auth_token_type = xml.getElementsByTagName("token_type").item(0).getTextContent();
    		String auth_token_value = xml.getElementsByTagName("access_token").item(0).getTextContent();
    		
    		if(auth_token_type.equalsIgnoreCase("bearer")) return auth_token_value;
    		return null;
        	
        } catch(Exception e) {
        	e.printStackTrace();
        	return null;
        }
	}
	
	/**
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	private String createServiceJWT() throws UnsupportedEncodingException {
		
		StringBuffer token = new StringBuffer();
		
		token.append(Base64.encodeBase64URLSafeString(JWT_HEADER.getBytes("UTF-8")));
		token.append(".");
		
		Calendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		long JWT_iat = c.getTimeInMillis() / 1000;
		long JWT_exp = JWT_iat + (60 * 60);
		
		String JWT_claim = new String("{");
		JWT_claim += "\"iss\":\"" + google_iss + "\","; 
		JWT_claim += "\"scope\":\"" + google_scope + "\","; 
		JWT_claim += "\"aud\":\"" + GOOGLE_AUD + "\","; 
		JWT_claim += "\"exp\":" + JWT_exp + ","; 
		JWT_claim += "\"iat\":" + JWT_iat; 
		JWT_claim += "}";
				
		token.append(Base64.encodeBase64URLSafeString(JWT_claim.getBytes("UTF-8")));
		byte[] sig = signServiceJWT(token.toString().getBytes("UTF-8"));
		
		token.append(".");
		token.append(Base64.encodeBase64URLSafeString(sig));
		
		return token.toString();
	}
	
	/**
	 * Sign the JTW header data with the specified certificate and return the signature
	 * @param bytes
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	private byte[] signServiceJWT(byte[] bytes) throws UnsupportedEncodingException {
	    byte[] signature = null;

	    try {
	        java.security.KeyStore keyStoreFile = java.security.KeyStore.getInstance("PKCS12");
	        keyStoreFile.load(new FileInputStream(google_private_key), google_key_password.toCharArray());
	        PrivateKey privateKey = (PrivateKey) keyStoreFile.getKey("privatekey", google_key_password.toCharArray());
	        Signature rsa = Signature.getInstance("SHA256withRSA");
	        
	        rsa.initSign(privateKey);
	        rsa.update(bytes);
	        signature = rsa.sign();

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    
	    return signature;
	}
	
}
