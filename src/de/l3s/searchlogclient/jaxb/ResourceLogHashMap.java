package de.l3s.searchlogclient.jaxb;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="ResourceLogHashMap")
public class ResourceLogHashMap {

	HashMap<String, ResourceLog> resourceLogHashMap;
	
	public ResourceLogHashMap(){
		resourceLogHashMap = new HashMap<String, ResourceLog>();
	}

	public HashMap<String, ResourceLog> getResourceLogHashMap() {
		return resourceLogHashMap;
	}

	public void setResourceLogHashMap(HashMap<String, ResourceLog> resourceLogHashMap) {
		this.resourceLogHashMap = resourceLogHashMap;
	}

}
