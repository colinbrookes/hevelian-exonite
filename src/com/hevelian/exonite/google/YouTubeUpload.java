package com.hevelian.exonite.google;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
//import java.nio.file.Files;
//import java.nio.file.Paths;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class YouTubeUpload {

	private String token 				= null;
	private String title				= null;
	private String description			= null;
	private String privacy				= null;
	private String channelId			= null;
	private ArrayList<String> tags		= null;

	private String location				= null;
	
	private static final String MIME_TYPE		= "video/*";
	private static final String URL_UPLOAD		= "https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet%2Cstatus";
	
	public YouTubeUpload(String auth_token) {
		token = auth_token;
		return;
	}
	
	/**
	 * performs the upload of the file with the supplied parameters
	 * @param movie
	 * @throws UnsupportedEncodingException 
	 */
	public void executeWith(String movie) throws UnsupportedEncodingException {
		File file = new File(movie);
		String snippet = buildSnippet();
        String buffer = "";
        
		System.out.println("SNIPPET: " + snippet);
		
        try {
    		OutputStream outStream = null;
            InputStream inStream = null;
            
            URL url = new URL(URL_UPLOAD);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setRequestProperty("Content-Length", String.valueOf(snippet.length()));
            urlConnection.addRequestProperty("Authorization", "Bearer " + token);
            urlConnection.addRequestProperty("X-Upload-Content-Type", MIME_TYPE);
            urlConnection.addRequestProperty("X-Upload-Content-Length", String.valueOf(file.length()));
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);

            urlConnection.connect();
            
            outStream = urlConnection.getOutputStream();
            outStream.write(snippet.getBytes("UTF-8"));
            outStream.flush();
            outStream.close();
     
    		location = urlConnection.getHeaderField("Location");
    		System.out.println("UPLOAD LOCATION: " + location);
    		
            inStream = urlConnection.getInputStream();
            String buf;
            BufferedReader b = new BufferedReader(new InputStreamReader(inStream));
            while((buf = b.readLine()) != null) {
            	buffer += buf;
            }
     
            inStream.close();
        	
        } catch(Exception e) {
        	e.printStackTrace();
        	return;
        }

        System.out.println("UPLOAD RESPONSE: " + buffer);
        
        if(location==null) {
        	System.out.println("UPLOAD: failed to get a Location");
        	return;
        }
        
        // now do the actual upload - we will do it in one chunk for now ...
        try {
    		OutputStream outStream = null;
            InputStream inStream = null;

            URL url = new URL(location);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestMethod("PUT");
            
            urlConnection.addRequestProperty("Authorization", "Bearer " + token);
            urlConnection.setRequestProperty("Content-Type", MIME_TYPE);
            urlConnection.setRequestProperty("Content-Length", String.valueOf(file.length()));
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);

            urlConnection.connect();
            
            outStream = urlConnection.getOutputStream();
//            outStream.write(Files.readAllBytes(Paths.get(movie)));
            outStream.flush();
            outStream.close();

            inStream = urlConnection.getInputStream();
            buffer = "";
            String buf;
            BufferedReader b = new BufferedReader(new InputStreamReader(inStream));
            while((buf = b.readLine()) != null) {
            	buffer += buf;
            }
     
            inStream.close();
        	
        } catch(Exception e) {
        	e.printStackTrace();
        }

        System.out.println("UPLOAD FINAL RESPONSE: " + buffer);
	}
	
	/**
	 * builds the JSON format snippet and status blocks
	 * @return
	 */
	private String buildSnippet() {
		StringBuilder snippet = new StringBuilder();
		
		snippet.append("{");
			snippet.append("\"snippet\":{");
			snippet.append("\"title\":\"").append(title).append("\",");
			snippet.append("\"description\":\"").append(description).append("\",");
			if(channelId!=null) snippet.append("\"channelId\":\"").append(channelId).append("\",");
		
			if(tags!=null && tags.size()>0) {
				snippet.append("\"tags\":[");
				for(int i=0; i<tags.size(); i++) {
					if(i>0) snippet.append(",");
					snippet.append("\"").append(tags.get(i)).append("\"");
				}
				snippet.append("],");
			}
			snippet.append("\"categoryId\":").append("22");
			snippet.append("},");
			snippet.append("\"status\":{");
			snippet.append("\"privacyStatus\":\"").append(privacy).append("\",");
//			snippet.append("\"embeddable\":").append("True").append(",");
			snippet.append("\"license\":\"").append("youtube").append("\"");
			snippet.append("}");
		snippet.append("}");
		
		return snippet.toString();
	}
	
	/**
	 * Sets the basic snippet and status parameters
	 * @param _title
	 * @param _description
	 * @param _privacy
	 * @param _tags
	 */
	public void setParameters(String _title, String _description, String _privacy, ArrayList<String> _tags, String _channelId) {
		title		 	= _title;
		description 	= _description;
		privacy 		= _privacy;
		tags 			= _tags;
		channelId		= _channelId;
	}
}
