package com.hevelian.exonite.utils;

import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.regex.Pattern;

public class J2X {

	private static String ROOT_NODE_NAME 	= "root";
	private static String OBJECT_NODE_NAME 	= "o";
	private static String ELEMENT_NODE_SET 	= "e";
	
	final private static int STATE_UNKNOWN		= 0;
	final private static int STATE_IN_NAME		= 1;
	final private static int STATE_IN_VALUE		= 2;
	
	final private static int TYPE_STRING		= 1;
	final private static int TYPE_NUMBER		= 2;
	
	private Document doc;
	private String Name = new String();
	private String Value = new String();
	private int State = STATE_UNKNOWN;
	private int Type = TYPE_STRING;
	
	public boolean IncludeType = false;
	public boolean IncludeSubType = false;
	
	// STRING sub-types
	private static final String EMAIL_PATTERN 		= "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
	private static final String URL_PATTERN 		= "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
	private static final String IP_PATTERN			= "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
	private static final String COLOR_PATTERN		= "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$";
	private static final String DATE_PATTERN		= "^\\d{4}-[0-1][0-9]-[0-3]\\d{1}T[0-2]\\d{1}:[0-5]\\d{1}:[0-5]\\d{1}Z$";
	private static final String DATE_TZ_PATTERN		= "^\\d{4}-[0-1][0-9]-[0-3]\\d{1}T[0-2]\\d{1}:[0-5]\\d{1}:[0-5]\\d{1}\\+\\d\\d:\\d\\dZ$";
	private static final String MILLI_PATTERN		= "^\\d{4}-[0-1][0-9]-[0-3]\\d{1}T[0-2]\\d{1}:[0-5]\\d{1}:[0-5]\\d{1}\\.\\d{3}Z$";
	private static final String MILLI_TZ_PATTERN	= "^\\d{4}-[0-1][0-9]-[0-3]\\d{1}T[0-2]\\d{1}:[0-5]\\d{1}:[0-5]\\d{1}\\.\\d{3}\\+\\d\\d:\\d\\dZ$";
	private static final String UUID_PATTERN		= "[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}";
	
	private Pattern email_pattern 				= Pattern.compile(EMAIL_PATTERN);
	private Pattern url_pattern 				= Pattern.compile(URL_PATTERN);
	private Pattern ip_pattern 					= Pattern.compile(IP_PATTERN);
	private Pattern color_pattern 				= Pattern.compile(COLOR_PATTERN);
	private Pattern date_pattern 				= Pattern.compile(DATE_PATTERN);	
	private Pattern date_tz_pattern 			= Pattern.compile(DATE_TZ_PATTERN);
	private Pattern milli_pattern 				= Pattern.compile(MILLI_PATTERN);
	private Pattern milli_tz_pattern 			= Pattern.compile(MILLI_TZ_PATTERN);
	private Pattern uuid_pattern 				= Pattern.compile(UUID_PATTERN);
	
	// NUMBER sub-types
	private static final String FLOAT_PATTERN 		= "[+-]?\\d*(\\.\\d+)?";
	private static final String INTEGER_PATTERN 	= "[+-]?\\d+";

	private Pattern float_pattern 		= Pattern.compile(FLOAT_PATTERN);
	private Pattern integer_pattern 	= Pattern.compile(INTEGER_PATTERN);
	
	public J2X() {
		return;
	}
	
	/**
	 * returns an XML Document object version of the specified JSON data.
	 * @param jsonData
	 * @return
	 */
	public Document X(String jsonData) throws ParserConfigurationException {
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		doc = dBuilder.newDocument();
		doc.setXmlVersion("1.0");
		doc.setXmlStandalone(true);
		
		Element root = doc.createElement(ROOT_NODE_NAME);
		root = (Element) doc.appendChild(root);
		
		Element node = null;
		Name = "";
		Value = "";
		
		for(int i=0; i<jsonData.length(); i++) {
			char c = jsonData.charAt(i);
			
			switch(c) {
			
			case '"':
				if(State==STATE_UNKNOWN && Name.length()==0) {
					State = STATE_IN_NAME;
					break;
				}
				
				if(State==STATE_UNKNOWN && Name.length()>0) {
					State = STATE_IN_VALUE;
					Type = TYPE_STRING;
					break;
				}
				
				if(State==STATE_IN_NAME && root.getNodeName().equalsIgnoreCase(ELEMENT_NODE_SET)) {
					node = doc.createElement("array");
					root.appendChild(node);
					CDATASection cdata = doc.createCDATASection(Name.trim());
					node.appendChild(cdata);
					setTypeAttribute(node);
					
					Value = "";
					Name = "";
					State = STATE_UNKNOWN;
					break;
				}
				
				if(State==STATE_IN_NAME) {
					node = doc.createElement(Name);
					root.appendChild(node);
					State = STATE_UNKNOWN;
					break;
				}
				
				if(State==STATE_IN_VALUE) {
					CDATASection cdata = doc.createCDATASection(Value.trim());
					node.appendChild(cdata);
					setTypeAttribute(node);
					Value = "";
					Name = "";
					State = STATE_UNKNOWN;
					node = null;
					break;
				}
				break;
				
			case '{':
				if(State==STATE_IN_VALUE) {
					Value += c;
					break;
				}

				if(node!=null) {
					root.appendChild(node);
					root = node;
					node = null;
				}
				Element o_node = doc.createElement(OBJECT_NODE_NAME);
				root.appendChild(o_node);
				root = o_node;
				
				State = STATE_UNKNOWN;
				Name = "";
				Value = "";
				node = null;
				break;
				
			case '[':
				if(State==STATE_IN_VALUE) {
					Value += c;
					break;
				}
				
				if(node!=null) {
					root.appendChild(node);
					root = node;
					node = null;
				}
				Element e_node = doc.createElement(ELEMENT_NODE_SET);
				root.appendChild(e_node);
				root = e_node;

				State = STATE_UNKNOWN;
				Name = "";
				Value = "";
				break;
				
			case ']':
			case '}':
				if(State==STATE_IN_VALUE && Type==TYPE_STRING) {
					Value += c;
					break;
				}

				if(State==STATE_IN_VALUE && Type==TYPE_NUMBER) {
					if(Name.length()==0) {
						// could be an 'array' value
						node = doc.createElement("array");
						root.appendChild(node);
					}
					CDATASection cdata = doc.createCDATASection(Value.trim());
					node.appendChild(cdata);
					setTypeAttribute(node);
					Value = "";
					Name = "";
					State = STATE_UNKNOWN;
				}
				
				try {
					if(!root.getNodeName().equalsIgnoreCase(OBJECT_NODE_NAME)) root = (Element) root.getParentNode();
					if(!root.getNodeName().equalsIgnoreCase(ELEMENT_NODE_SET)) root = (Element) root.getParentNode();
				} catch(Exception e) { }

				State = STATE_UNKNOWN;
				Name = "";
				Value = "";
				break;
			
			case ',':
				System.out.println("J2X: Got Comma: State: " + State + ", Type: " + Type + ", Name: " + Name + ", Value: " + Value);
			case ':':
				if(State==STATE_IN_VALUE && Type==TYPE_NUMBER && node!=null) {
					if(Name.length()==0) {
						// could be an 'array' value
						node = doc.createElement("array");
						root.appendChild(node);
					}
					CDATASection cdata = doc.createCDATASection(Value.trim());
					node.appendChild(cdata);
					setTypeAttribute(node);
					Value = "";
					Name = "";
					State = STATE_UNKNOWN;
					Type = TYPE_STRING;
					break;
				}
				if(State==STATE_UNKNOWN) break;
				if(State==STATE_IN_NAME) Name += c;
				if(State==STATE_IN_VALUE) Value += c;
				break;
				
			case '\\':
				i++;
				c = jsonData.charAt(i);
				switch(c) {
				case 'n':
					c= '\n'; break;
				case 'r':
					c= '\r'; break;
				case 't':
					c= '\t'; break;
				case 'b':
					c= '\b'; break;
				case 'f':
					c= '\f'; break;
				case 'u':
					String unicode = "\\u" + jsonData.charAt(i+1) + jsonData.charAt(i+2) + jsonData.charAt(i+3) + jsonData.charAt(i+4);
					c = (char) Integer.parseInt(unicode.substring(2), 16);
					i += 4;
				default:
					// do nothing, just a regular escaped char	
				}
				// allow fall-through on purpose ...
				
			default:
				if(State==STATE_UNKNOWN && !Character.isWhitespace(c)) {
					State = STATE_IN_VALUE;
					Type = TYPE_NUMBER;
				}
				if(State==STATE_IN_NAME) Name += c;
				if(State==STATE_IN_VALUE) Value += c;
				break;
			}
		}
		
		return doc;
	}
	
	private void setTypeAttribute(Element node) {
		if(IncludeType==false && IncludeSubType==false) return;
		
		if(IncludeType==true) {
			switch(Type) {
			case TYPE_STRING:
				node.setAttribute("type", "string");
				break;
				
			case TYPE_NUMBER:
				node.setAttribute("type", "number");
				break;
			}
		}
		
		// advanced data type detection
		if(IncludeSubType==true) {
			switch(Type) {
			case TYPE_STRING:
				if(email_pattern.matcher(node.getFirstChild().getNodeValue()).matches()) {
					node.setAttribute("subtype", "email");
					break;
				}
				if(url_pattern.matcher(node.getFirstChild().getNodeValue()).matches()) {
					node.setAttribute("subtype", "url");
					break;
				}
				if(ip_pattern.matcher(node.getFirstChild().getNodeValue()).matches()) {
					node.setAttribute("subtype", "ip_address");
					break;
				}
				if(color_pattern.matcher(node.getFirstChild().getNodeValue()).matches()) {
					node.setAttribute("subtype", "colour");
					break;
				}
				if(uuid_pattern.matcher(node.getFirstChild().getNodeValue().toLowerCase()).matches()) {
					node.setAttribute("subtype", "uuid");
					break;
				}
				if(date_pattern.matcher(node.getFirstChild().getNodeValue()).matches()) {
					node.setAttribute("subtype", "datetime");
					node.setAttribute("withMillis", "false");
					node.setAttribute("withTimezone", "false");
					break;
				}
				if(date_tz_pattern.matcher(node.getFirstChild().getNodeValue()).matches()) {
					node.setAttribute("subtype", "datetime");
					node.setAttribute("withMillis", "false");
					node.setAttribute("withTimezone", "true");
					break;
				}
				if(milli_pattern.matcher(node.getFirstChild().getNodeValue()).matches()) {
					node.setAttribute("subtype", "datetime");
					node.setAttribute("withMillis", "true");
					node.setAttribute("withTimezone", "false");
					break;
				}
				if(milli_tz_pattern.matcher(node.getFirstChild().getNodeValue()).matches()) {
					node.setAttribute("subtype", "datetime");
					node.setAttribute("withMillis", "true");
					node.setAttribute("withTimezone", "true");
					break;
				}
				break;
				
			case TYPE_NUMBER:
				if(integer_pattern.matcher(node.getFirstChild().getNodeValue()).matches()) {
					node.setAttribute("subtype", "integer");
					break;
				}
				if(float_pattern.matcher(node.getFirstChild().getNodeValue()).matches()) {
					node.setAttribute("subtype", "float");
					break;
				}
				break;
			}
		}
	}
	
	/**
	 * Creates a pretty-formatted string containing the entire XML tree
	 * @param xml
	 * @return
	 * @throws Exception
	 */
	public static final String prettyPrint(Document xml) throws Exception {
		String buf = new String();
		
		Transformer tf = TransformerFactory.newInstance().newTransformer();
		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		
		Writer out = new StringWriter();
		tf.transform(new DOMSource(xml), new StreamResult(out));
		
		buf += out.toString();
		
		return buf;
	}	
	
}
