package com.hevelian.exonite.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hevelian.exonite.cache.Index;
import com.hevelian.exonite.core.CollectionItem;
import com.hevelian.exonite.core.Configuration;
import com.hevelian.exonite.interfaces.Action;

/**
 * IndexCleaner removes records from an index based on specific criteria.
 * The criteria can be expressed as a simple logic clause or as a script.
 * @author cb
 *
 */
public class IndexCleaner implements Action {
	
	private String keyFolder					= "keys/";
	private Configuration config 				= new Configuration();
	
	private String whereType					= "simple";
	private String fWhere						= null;
	private String wLHS							= null;
	private String wCON							= null;
	private String wRHS							= null;

	DocumentBuilderFactory dbFactory 			= null;
	DocumentBuilder dBuilder 					= null;
	ScriptEngineManager factory 				= new ScriptEngineManager();
	ScriptEngine engine 						= factory.getEngineByName("JavaScript");
	Index index									= null;

	@Override
	public void run(Element child, HashMap<String, Action> map) {
		String id 				= child.getAttribute("id");
		String index 			= child.getAttribute("index");
		String root 			= child.getAttribute("root");

		int cntTotal			= 0;
		int cntCleaned			= 0;
		
		System.out.println("INDEXCLEANER: STARTED: " + id);
		
		String folder_home = config.getProperty("folder_home");
		String storePath = folder_home + "data/" + root + "/" + index + "/";
		File store = new File(storePath + keyFolder);
		if(!store.exists()) {
			System.out.println("INDEXCLEANER: index store does not exist: " + storePath);
			return;
		}
		
		try {
			// check if we have a 'where' clause
			NodeList whereNodes = child.getElementsByTagName("where");
			if(whereNodes!=null && whereNodes.getLength()>0) {
				Element whereNode = (Element) whereNodes.item(0); 
				if(whereNode.getAttribute("type")==null || !whereNode.getAttribute("type").equalsIgnoreCase("script")) {
					// simple style clause
					fWhere = whereNode.getTextContent();
					String[] whereParts = fWhere.split(" ");
					if(whereParts.length==3) {
						wLHS = whereParts[0];
						wCON = whereParts[1];
						wRHS = whereParts[2];
					}

				} else {
					// script style clause
					whereType = "script";
					fWhere = whereNode.getTextContent();
				}
			}
		} catch(Exception e) {}
		
		/**
		 * we now iterate through all the files and apply the 'where' logic
		 */
		try {
			File[] files = store.listFiles();
			for(int i=0; i<files.length; i++) {
				File file = files[i];
				if(file.isDirectory()) continue;
				
				try(BufferedReader br = new BufferedReader(new FileReader(file))) {

					// setup the output tmp file
					File outFile = new File(file.getAbsoluteFile() + ".tmp");
					FileOutputStream fos = new FileOutputStream(outFile);
					PrintWriter out = new PrintWriter(fos);
					
					for(String line; (line = br.readLine()) != null; cntTotal++) {
						Element e = loadIndexedRecord(line);
						if(!matchesWhereClause(e)) {
							out.println(line);
						} else {
							cntCleaned++;
						}
					}
					
					br.close();
					out.flush();
					out.close();
					
					file.delete();
					if(outFile.length()==0) {
						outFile.delete();
					} else {
						outFile.renameTo(file);
					}
				}
			}
		} catch(Exception e) { }
		
		System.out.println("INDEX CLEANER: TOTAL: " + cntTotal + ", CLEANED: " + cntCleaned);
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
			if(result.equalsIgnoreCase("true")) return true;
			return false;
			
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
			}
			
		} catch(Exception e) {
			return true;
		}

		
		return false;
	}

	@Override
	public Element getRecord() {
		return null;
	}

	@Override
	public String getProperty(String name) {
		return null;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public void next() {

	}

}
