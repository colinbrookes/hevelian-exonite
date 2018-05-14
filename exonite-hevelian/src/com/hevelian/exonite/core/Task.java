package com.hevelian.exonite.core;

import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hevelian.exonite.interfaces.Action;

public class Task {

	Document task 					= null;
	Configuration config 			= new Configuration();
	HashMap<String, Action> objects = new HashMap<String,Action>();
	
	public Task() {
		System.out.println("TASK: new Task object created");
	}
	
	public void run(Document task) throws ParserConfigurationException, SAXException, IOException {
		
		this.task = task;
		Node root = this.task.getElementsByTagName("task").item(0);
		
		if(!root.hasChildNodes()) {
			System.out.println("TASK: Task has nothing to run.");
			return;
		}
		
		System.out.println("TASK: runner has started");
		
		NodeList children = root.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			if(children.item(i).getNodeType()!=Node.ELEMENT_NODE) continue;
			
			Element child = (Element) children.item(i);
			
			System.out.println("TASK: action: " + child.getNodeName() + " with id " + child.getAttribute("id"));
			
			// we need to create a new instance of the class as specified by the node name and the call its run() method
			com.hevelian.exonite.interfaces.Action action = null;
			try {
				Class<?> c = Class.forName(config.getActionByName(child.getNodeName()));
				action = (com.hevelian.exonite.interfaces.Action) c.newInstance();
				
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			String _id = child.getAttribute("id");
			objects.put(_id, action);
			action.run(child, objects);
			
		}
	}
}
