package de.l3s.sparqlclient;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="ResourceLogHashMap")
public class ResourceLogHashMap {

	HashMap<String, String> resourceLogHashMap;
	
	public ResourceLogHashMap(){
		resourceLogHashMap = new HashMap<String, String>();
	}

	public HashMap<String, String> getResourceLogHashMap() {
		return resourceLogHashMap;
	}

	public void setResourceLogHashMap(HashMap<String, String> resourceLogHashMap) {
		this.resourceLogHashMap = resourceLogHashMap;
	}

}
