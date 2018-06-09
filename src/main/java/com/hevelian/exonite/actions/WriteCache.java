package com.hevelian.exonite.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.hevelian.exonite.core.Configuration;
import com.hevelian.exonite.core.Evaluator;
import com.hevelian.exonite.interfaces.Action;
import com.hevelian.exonite.cache.FullTextIndex;
import com.hevelian.exonite.cache.Index;

public class WriteCache implements com.hevelian.exonite.interfaces.Action {

	Configuration config 					= new Configuration();
	ArrayList<Index> indexes				= new ArrayList<Index>();
	ArrayList<FullTextIndex> ftindexes		= new ArrayList<FullTextIndex>();
	
	public WriteCache() {
	}
	
	@Override
	public void run(Element child, HashMap<String, Action> map) {
		
		Evaluator evaluator 		= new Evaluator(map);
		
		String root 				= evaluator.evaluate(child.getAttribute("root"));
		String key 					= child.getAttribute("key");
		String row 					= child.getAttribute("row");
		
		// we need to create the IndexStore objects, if indexes are defined.
		if(child.hasChildNodes()) {
			NodeList children = child.getChildNodes();
			for(int i=0; i<children.getLength(); i++) {
				if(children.item(i).getNodeType()!=Element.ELEMENT_NODE) continue;
				Element el = (Element) children.item(i);
				
				if(el.getNodeName()=="index") {
					// we have an index, so lets create an index store object
					Index index = new Index(el, map);
					indexes.add(index);
				}
				
				if(el.getNodeName()=="fullTextIndex") {
					// we have a full-text index, so lets create an index store object
					FullTextIndex index = new FullTextIndex(el, map);
					ftindexes.add(index);
				}
			}
		}
		
		Collection collection		= (Collection) map.get(row);
		Element record = collection.getRecord();
		
		String folder = config.getProperty("folder_home") + "data/" + root + "/";
		
		try {
			File fileFolder = new File(folder);
			fileFolder.mkdirs();
			
			String filename = folder + evaluator.evaluate(key);
			File outFilePtr = new File(filename);
			FileOutputStream outFile = new FileOutputStream(outFilePtr);
			
			Transformer tf = TransformerFactory.newInstance().newTransformer();
			tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			tf.setOutputProperty(OutputKeys.INDENT, "yes");
			
			Writer out = new StringWriter();
			tf.transform(new DOMSource(record), new StreamResult(out));
			
			outFile.write(out.toString().getBytes("UTF-8"));
			outFile.close();
			
			// check if we have any indexes to update
			if(indexes.size()>0) {
				// we have indexes, so we need to update them
				for(int i=0; i<indexes.size(); i++) {
					Index index = indexes.get(i);
					index.updateIndex(record, filename);
				}
			}
			
			// and then the fulltext indexes
			if(ftindexes.size()>0) {
				// we have indexes, so we need to update them
				for(int i=0; i<ftindexes.size(); i++) {
					FullTextIndex index = ftindexes.get(i);
					index.updateIndex(record, filename);
				}
			}
			
		} catch(Exception e) {
		}

		return;
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
