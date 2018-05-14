package com.hevelian.exonite.actions;

import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hevelian.exonite.core.Configuration;
import com.hevelian.exonite.interfaces.Action;

public class ForEach implements com.hevelian.exonite.interfaces.Action {

	Configuration config 				= new Configuration();
	HashMap<String, Action> objects		= new HashMap<String, Action>();
	
	public ForEach() {
	}
	
	@Override
	public void run(Element child, HashMap<String, Action> map) {
		
		// copy the parent objects to this local object list
		objects.putAll(map);
		
		String collectionName				= child.getAttribute("ItemInCollection");
		Collection collection				= (Collection) objects.get(collectionName);
		
		for(int n=0; n<collection.size(); n++) {
			// this is the real FOR EACH loop
			collection.next();
			objects.put(collectionName,  collection);
			
			NodeList children = child.getChildNodes();
			for(int i=0; i<children.getLength(); i++) {
				if(children.item(i).getNodeType()!=Node.ELEMENT_NODE) continue;
				
				Element mychild = (Element) children.item(i);
				
				// we need to create a new instance of the class as specified by the node name and then call its run() method
				com.hevelian.exonite.interfaces.Action action = null;
				try {
					Class<?> c = Class.forName(config.getActionByName(mychild.getNodeName()));
					action = (com.hevelian.exonite.interfaces.Action) c.newInstance();
					
				} catch(Exception e) {
					e.printStackTrace();
				}
				
				String _id = mychild.getAttribute("id");
				objects.put(_id, action);
				action.run(mychild, objects);
				
			}

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
