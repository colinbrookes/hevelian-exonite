package com.hevelian.exonite.connectors;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.hevelian.exonite.core.CollectionItem;
import com.hevelian.exonite.core.Connector;
import com.hevelian.exonite.core.Evaluator;
import com.hevelian.exonite.interfaces.Action;
import com.hevelian.exonite.interfaces.ConnectorImpl;
import com.hevelian.exonite.utils.NameValuePair;

public class URI implements ConnectorImpl {

	private ArrayList<String> columns = new ArrayList<String>();
	private ArrayList<CollectionItem> items = null;
	private HashMap<String, Action> objects = null;
	private Evaluator evaluator = null;
	private Document doc = null;

	private ArrayList<NameValuePair> urlParams	= new ArrayList<NameValuePair>();
	
	public URI() {
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
	public ArrayList<CollectionItem> select(Document _doc, Connector connector, HttpServletRequest request) {
		
		items = new ArrayList<CollectionItem>();
		evaluator = new Evaluator(request, objects, connector);
		String isType = "xml";
		String useMethod = "GET";
		String useContentType = "application/x-www-form-urlencoded";
		String body = "";
		String startFrom = null;
		String extractNode = null;
		String root = null;

		String uri = evaluator.evaluate(_doc.getElementsByTagName("uri").item(0).getTextContent());
		
		if(_doc.getElementsByTagName("root").getLength()>0) {
			root = evaluator.evaluate(_doc.getElementsByTagName("root").item(0).getTextContent());			
		}

		if(_doc.getElementsByTagName("startFrom").getLength()>0) {
			startFrom = evaluator.evaluate(_doc.getElementsByTagName("startFrom").item(0).getTextContent());			
		}

		if(_doc.getElementsByTagName("extractNodes").getLength()>0) {
			extractNode = evaluator.evaluate(_doc.getElementsByTagName("extractNodes").item(0).getTextContent());			
		}

		if(_doc.getElementsByTagName("isType").getLength()>0) {
			isType = evaluator.evaluate(_doc.getElementsByTagName("isType").item(0).getTextContent());			
		}
		
		if(_doc.getElementsByTagName("useMethod").getLength()>0) {
			useMethod = evaluator.evaluate(_doc.getElementsByTagName("useMethod").item(0).getTextContent());			
		}
		
		if(_doc.getElementsByTagName("useContentType").getLength()>0) {
			useContentType = evaluator.evaluate(_doc.getElementsByTagName("useContentType").item(0).getTextContent());			
		}
		
		if(useMethod.equalsIgnoreCase("POST")) {
			
			try {
				Element nodeProperties = (Element) _doc.getElementsByTagName("properties").item(0);
				for(int i=0; i<nodeProperties.getChildNodes().getLength(); i++) {
					Node node = nodeProperties.getChildNodes().item(i);
					
					 if(node.getNodeType()!=Element.ELEMENT_NODE) continue;
					 
					 urlParams.add(new NameValuePair(node.getNodeName(), node.getTextContent()));
					 
					 if(body.length()==0) {
						 body = node.getAttributes().getNamedItem("name").getTextContent() + "=" + URLEncoder.encode(evaluator.evaluate(node.getTextContent()), "UTF-8");
					 } else {
						 body = body + "&" + node.getAttributes().getNamedItem("name").getTextContent() + "=" + URLEncoder.encode(evaluator.evaluate(node.getTextContent()), "UTF-8");
					 }
				}
			} catch(Exception e) {
				System.out.println(e.getMessage());
			}
		}
		
		try {
			if(isType.equalsIgnoreCase("json")) {
				java.net.URI _uri = new java.net.URI(uri + "?" + body);
				URL url = _uri.toURL();
				
				String inputLine;
				String jsonRaw = "";
				
				URLConnection con = null;
				BufferedReader in;
				
				if(useMethod.equalsIgnoreCase("POST")) {
					HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
					con = (URLConnection) url.openConnection();
					httpCon.setDoInput(true);
					httpCon.setUseCaches(false);
					httpCon.setDoOutput(true);
					httpCon.setRequestMethod("POST");
					
					byte[] postData = body.getBytes("UTF-8");
					int postDataLength = postData.length;
					
					httpCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
					httpCon.setRequestProperty("Content-Length", Integer.toString(postDataLength));
					
					DataOutputStream out = new DataOutputStream(httpCon.getOutputStream());
					out.write(postData);
					out.flush();
					out.close();
					
				    in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				    while ((inputLine = in.readLine()) != null) {
				        jsonRaw += inputLine;
				    }
				    in.close();
				} else {
					con = (URLConnection) url.openConnection();
				    con.setDoInput(true);
		            con.setUseCaches(false);
				    con.connect();

				    in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				    while ((inputLine = in.readLine()) != null) {
				        jsonRaw += inputLine;
				    }
				    in.close();
				}
			    
			    JSONObject jsonObject = new JSONObject(jsonRaw);
			    fixJsonKey(jsonObject);

			    String xml = "<x>" + XML.toString(jsonObject) + "</x>";

				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				doc = dBuilder.parse(new InputSource( new StringReader(xml)));
			    
			} else {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				doc = dBuilder.parse(uri);
			}
			
			
			processResult(doc, root, startFrom, extractNode);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
        
		return items;
	}

	private void processResult(Document xml, String response_node, String start_node, String extract_node) {
		try {
			
			// legacy function
			if(response_node!=null) {
				processNodeSet(xml.getElementsByTagName(response_node));
			}
			
			if(start_node!=null&&extract_node!=null) {
				Element e = (Element) xml.getElementsByTagName(start_node).item(0);
				for(int i=0; i<e.getChildNodes().getLength(); i++) {
					Node node = e.getChildNodes().item(i);
					
					if(node.getNodeType()!=Element.ELEMENT_NODE) continue;
					if(node.getNodeName().equals(extract_node)) {
						CollectionItem item = new CollectionItem();
						ProcessChildren(item, node, node.getNodeName(), i);
						items.add(item);
					}
				}
			}
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
	
	private static void fixJsonKey(Object json) {

        if (json instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) json;
            List<String> keyList = new LinkedList<String>(jsonObject.keySet());
            for (String key : keyList) {
                if (!key.matches(".*[\\s\t\n\"]+.*")) {
                    Object value = jsonObject.get(key);
                    fixJsonKey(value);
                    continue;
                }

                Object value = jsonObject.remove(key);
                String newKey = key.replaceAll("[\\s\t\n\"]", "");

                fixJsonKey(value);

                jsonObject.accumulate(newKey, value);
            }
        } else if (json instanceof JSONArray) {
            for (Object aJsonArray : (JSONArray) json) {
                fixJsonKey(aJsonArray);
            }
        }
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
