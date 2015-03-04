package de.l3s.searchlogclient.jaxb;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="ResourceLogList")
public class ResourceLogList {

	ArrayList<ResourceLog> resourceLog;

	
	public ResourceLogList(){
		resourceLog = new ArrayList<ResourceLog>();
	}
	
	public ArrayList<ResourceLog> getResourceLog() {
		return resourceLog;
	}

	public void setResourceLog(ArrayList<ResourceLog> resourceLog) {
		this.resourceLog = resourceLog;
	}

}





