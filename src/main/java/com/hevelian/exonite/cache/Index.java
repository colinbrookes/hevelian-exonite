package com.hevelian.exonite.cache;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.zip.CRC32;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hevelian.exonite.core.CollectionItem;
import com.hevelian.exonite.core.Configuration;
import com.hevelian.exonite.interfaces.Action;
import com.hevelian.exonite.core.Evaluator;

/**
 * The index maintains pointers to the documents based on the given criteria.
 * The pointers file contains the correct number of pointers for the specified 'on' criteria. It is limited by the 'max' value.
 * 
 * @author cb
 *
 */
public class Index {

	private String keyFolder					= "keys/";
//	private String keyIndexFilename				= "index.dat";
	private String keyDefFilename				= "index.def";
	
	private Configuration config 				= new Configuration();
	private Evaluator evaluator					= null;
	private Element doc 						= null;
	
	private String orderBy 						= null;
	private String orderByField					= null;
	private String orderByQualifier				= null;
	private String max 							= null;
	private String on 							= null;
	private String id 							= null;
	private String root 						= null;
	private String storePath 					= null;
	
	private boolean hasWhereClause				= false;
	private String whereType					= "simple";
	private String fWhere						= null;
	private String wLHS							= null;
	private String wCON							= null;
	private String wRHS							= null;

	private DocumentBuilderFactory dbFactory 			= null;
	private DocumentBuilder dBuilder 					= null;
	private ScriptEngineManager factory 				= new ScriptEngineManager();
	private ScriptEngine engine 						= factory.getEngineByName("JavaScript");

	public Index(Element xml, HashMap<String, Action> map) {
		doc 		= xml;
		
		orderBy 	= doc.getAttribute("orderBy");
		max 		= doc.getAttribute("max");
		on 			= doc.getAttribute("on");
		id 			= doc.getAttribute("id");
		root 		= doc.getAttribute("root");
		
		evaluator	= new Evaluator(map);
		
		try {
			// check if we have a 'where' clause
			NodeList whereNodes = xml.getElementsByTagName("where");
			if(whereNodes!=null && whereNodes.getLength()>0) {
				Element whereNode = (Element) whereNodes.item(0); 
				if(whereNode.getAttribute("type")==null || !whereNode.getAttribute("type").equalsIgnoreCase("script")) {
					// simple style clause
					fWhere = whereNode.getTextContent();
					String[] whereParts = fWhere.split(" ");
					if(whereParts.length==3) {
						hasWhereClause = true;
						wLHS = whereParts[0];
						wCON = whereParts[1];
						wRHS = whereParts[2];
					}

				} else {
					// script style clause
					hasWhereClause = true;
					whereType = "script";
					fWhere = whereNode.getTextContent();
				}
			}
		} catch(Exception e) {}
		
		try {
			initialiseIndexStore();
		} catch(Exception e) {}
		return;
	}
	
	/**
	 * if we have a WHERE clause then we need to evaluate the record against it.
	 * @param record
	 * @return
	 */
	public boolean matchesWhereClause(Element record) {
		if(whereType.equalsIgnoreCase("simple")) return matchesSimpleWhereClause(record);
		if(whereType.equalsIgnoreCase("script")) return matchesScriptWhereClause(record);
		return false;
	}

	/**
	 * A where clause can contain a script instead of a simple logic evaluation.
	 * @param record
	 * @return
	 */
	public boolean matchesScriptWhereClause(Element record) {
		try {
			engine.put("item", new CollectionItem(record));
			engine.eval(fWhere);
			String result = (String) engine.get("result");
			return result.equalsIgnoreCase("true");
		} catch(Exception e) {}
		return false;
	}
	
	/**
	 * check where a record matches the where clause. The LHS and the RHS can be either fixed values
	 * or elements in the specified record. If we dont find an element with the given name then we
	 * assume it is a fixed value.
	 * @param record
	 * @return
	 */
	public boolean matchesSimpleWhereClause(Element record) {
		try {
			NodeList nlLHS = record.getElementsByTagName(wLHS);
			NodeList nlRHS = record.getElementsByTagName(wRHS);
			
			String vLHS = wLHS;
			String vRHS = wRHS;
		
			if(nlLHS!=null && nlLHS.getLength()>0) {
				vLHS = nlLHS.item(0).getTextContent();
			}
			
			if(nlRHS!=null && nlRHS.getLength()>0) {
				vRHS = nlRHS.item(0).getTextContent();
			}
			
			long lLHS = Long.parseLong(vLHS);
			long lRHS = Long.parseLong(vRHS);
			
			switch(wCON) {
			case "lessThan":
				if(lLHS < lRHS) return true;
				break;
				
			case "greaterThan":
				if(lLHS > lRHS) return true;
				break;
				
			case "equals":
				if(lLHS == lRHS) return true;
				break;
				
			case "notEqualTo":
				if(lLHS != lRHS) return true;
				break;
				
			default:
				break;
			}
			
		} catch(Exception e) {
			return true;
		}

		
		return false;
	}
	
	public void updateIndex(Element record, String filename) {
		String dOn		= createSafeString(record.getElementsByTagName(on).item(0).getTextContent());
		File index		= getIndexFile(dOn);
		String key		= getIndexKey(record, filename);
		
		try {
			// check if this record meets the 'where' clause criteria
			if(fWhere!=null && !matchesWhereClause(record)) {
				return;
			}
			
			// if the index doesnt exist then it is new, so we just write out the key and return
			if(!index.exists()) {
				FileOutputStream out = new FileOutputStream(index);
				out.write(key.getBytes("UTF-8"));
				out.write("\n".getBytes("UTF-8"));	// append a newline	
				out.flush();
				out.close();
				return;
				
			}
			
			// if the key is already in the index, then we don't need to check anything, just return
			if(indexHasKey(index.getAbsolutePath(), key)) {
				return;
			}
			
			// if we have no orderBy or max or where-clause then we just append the key to the index file
			if(orderBy==null && max==null) {
				FileWriter fw = new FileWriter(index, true);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.append(key + "\n");
				bw.close();
				return;
			}
			
			// okay, we now need to do the heavy lifting, and check if the key needs adding to the index
			// ...
			applyKeyLogicToRecord(record, index, key);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Where the simple test cases do not apply, then we need to apply the logic defined in the index
	 * to see if (and where) this record should fit in the index. It is assumed that the key doesnt exist
	 * already in the file.
	 * @param record
	 * @param index
	 * @param key
	 */
	private void applyKeyLogicToRecord(Element record, File index, String key) {
		long cnt 			= 0;
		long lMax 			= -1;
		boolean inserted 	= false;
		
		if(max!=null && !max.equalsIgnoreCase("")) {
			lMax = Long.parseLong(max);
		}
		
		// we read each line of the index file, and run the compare logic to determine where this new key should be inserted
		try(BufferedReader br = new BufferedReader(new FileReader(index))) {

			// setup the output tmp file
			File outFile = new File(index.getAbsoluteFile() + ".tmp");
			FileOutputStream fos = new FileOutputStream(outFile);
			PrintWriter out = new PrintWriter(fos);
			
			for(String line; (line = br.readLine()) != null; cnt++) {

				// check for file limit based on MAX records allowed in index
				if(cnt==lMax) {
		    		out.flush();
		    		out.close();
		    		br.close();
		    		
		    		index.delete();
		    		outFile.renameTo(index);
		    		return;
		    	}
		    	
		    	// here we test for adding the record
		    	if(inserted==false && recordGoesBefore(record, line)) {
		    		out.println(key);
		    		cnt++;
		    		
		    		if(cnt==lMax) {
			    		out.flush();
			    		out.close();
			    		br.close();
			    		
			    		index.delete();
			    		outFile.renameTo(index);
			    		return;
		    		}
		    		inserted = true;
		    	}

				// check if the old record needs to be removed from the index
				if(hasWhereClause==true && fWhere!=null) {
					Element iRecord = loadIndexedRecord(line);
					if(iRecord!=null && matchesWhereClause(iRecord)) {
						out.println(line);
					}
				} else {
					out.println(line);
				}
		    }
			
			br.close();
			out.flush();
			out.close();
			index.delete();
			outFile.renameTo(index);
			return;
			
		} catch(Exception e) {
			return;
		}
	}
	
	private Element loadIndexedRecord(String line) throws ParserConfigurationException, SAXException, IOException {
		String[] lineParts = line.split("@@");
		String recFilename = lineParts[lineParts.length - 1];
		
		if(dbFactory==null) {
			dbFactory = DocumentBuilderFactory.newInstance();
			dBuilder = dbFactory.newDocumentBuilder();
		}

		Document doc = dBuilder.parse(new File(recFilename));
		if(doc==null) {
			return null;
		}
		return doc.getDocumentElement();
	}
	
	/**
	 * Check for insertion point in the index file for the new record
	 * @param record
	 * @param line
	 * @return
	 */
	private boolean recordGoesBefore(Element record, String line) {
		
		if(orderBy!=null && orderByField==null && orderByQualifier==null) {
			// initialise the qualifier and field
			String[] parts = orderBy.split(" ");
			orderByField = parts[0];
			if(parts.length>1) orderByQualifier = parts[1];
		}
		
		if(orderByQualifier==null) orderByQualifier = "stringAscending";
		
		// extract the key from the input line, ready for comparison
		String[] lineParts = line.split("@@");
		String lineKey = lineParts[0];
		
		String recValue = record.getElementsByTagName(orderByField).item(0).getTextContent();
		
		switch(orderByQualifier) {
		case "stringDescending":
			if(recValue.compareTo(lineKey) >= 0) return true;
			return false;
			
		case "stringAscending":
			if(recValue.compareTo(lineKey) <= 0) return true;
			return false;
			
		case "numericDescending":
			long val = Long.parseLong(recValue);
			long valLine = Long.parseLong(lineKey);
			if(val >= valLine) return true;
			return false;
			
		case "numericAscending":
			long aval = Long.parseLong(recValue);
			long avalLine = Long.parseLong(lineKey);
			if(aval <= avalLine) return true;
			return false;
			
		default:
			// unknown qualifier
			break;
		}
		
		return false;
	}
	
	/**
	 * check to see if the specified key already exists in the index file.
	 * @param file
	 * @param key
	 * @return
	 */
	private boolean indexHasKey(String file, String key) {
		
		try(BufferedReader br = new BufferedReader(new FileReader(file))) {
		    for(String line; (line = br.readLine()) != null; ) {
		    	if(line.equalsIgnoreCase(key)) {
		    		br.close();
		    		return true;
		    	}
		    }
		    br.close();
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return false;
	}
	
	/**
	 * based on the orderBy and the pointer filename we need to build the actual key stored in the index file
	 * for this given 'on' criteria.
	 * @param record
	 * @param filename
	 * @return
	 */
	private String getIndexKey(Element record, String filename) {
		String[] orderByParts = orderBy.split(",");
		StringBuffer indexKey = new StringBuffer();
		
		for(int i=0; i<orderByParts.length; i++) {
			String[] orderByItem = orderByParts[i].split(" ");
			
			indexKey.append(record.getElementsByTagName(orderByItem[0]).item(0).getTextContent());
			indexKey.append("@@");
		}
		
		indexKey.append(filename);
		return indexKey.toString();
	}
	
	/**
	 * Based on the hash key, we need to see get a handle on the key store.
	 * @param key
	 * @return
	 */
	private File getIndexFile(String key) {
		String folder_home = config.getProperty("folder_home");
		storePath = folder_home + "data/" + evaluator.evaluate(root) + "/" + id + "/";
		File store = new File(storePath + keyFolder);
		store.mkdirs();

		File index = new File(storePath + keyFolder + key);
		return index;
	}
	
	/**
	 * The safe string is used for creating the indexes file, so we need to make sure it doesnt have weirdness in it and that it isnt too long. 
	 * @param from
	 * @return
	 */
	private String createSafeString(String from) {
		CRC32 crc = new CRC32();
		try {
			crc.update(from.getBytes("UTF-8"));
			return Long.toHexString(crc.getValue());
		} catch (Exception e) {
			e.printStackTrace();
			return from;
		}
	}
	
	private void initialiseIndexStore() throws IOException {
		
		if(id==null) {
			System.out.println("EXONITE: INDEX: id not supplied.");
			return;
		}
		
		String folder_home = config.getProperty("folder_home");
		storePath = folder_home + "data/" + evaluator.evaluate(root) + "/" + id + "/";
		File store = new File(storePath + keyFolder);
		store.mkdirs();

		String index_def = storePath + keyDefFilename;
		File fileIndexDef = new File(index_def);
		if(!fileIndexDef.exists()) {
			// we need to write this file
			StringBuffer sb = new StringBuffer();
			sb.append("<?xml encoding=\"utf-8\" version=\"1.0\"?>\n");
			sb.append("<index>\n");
			sb.append("\t<type>index</type>\n");
			
			sb.append("\t\t<definition>\n");
			sb.append("\t\t\t<id>").append(id).append("</id>\n");
			sb.append("\t\t\t<orderBy>").append(orderBy).append("</orderBy>\n");
			sb.append("\t\t\t<max>").append(max).append("</max>\n");
			sb.append("\t\t\t<on>").append(on).append("</on>\n");
			sb.append("\t\t\t<where><![CDATA[").append(fWhere).append("]]></where>\n");
			sb.append("\t\t\t<root>").append(root).append("</root>\n");
			sb.append("\t\t</definition>\n");

			sb.append("</index>\n");
			
			FileWriter fw = new FileWriter(fileIndexDef);
			fw.write(sb.toString());
			fw.flush();
			fw.close();
			
		}
		
		/*
		String index_key = storePath + keyIndexFilename;
		File fileIndexKey = new File(index_key);
		if(!fileIndexKey.exists()) {
		}
		*/
		
		return;
	}
}
