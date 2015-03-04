package de.l3s.searchlogclient.jaxb;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ServiceResponse {

	private String message;
	private int returnid;
	
	//Default Constructor
	public ServiceResponse(){}
	
	//Parameterized Constructor
	public ServiceResponse(String message, int returnid){
		
		this.message = message;
		this.returnid = returnid;
	}
	
	@XmlElement
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

	@XmlElement
	public int getReturnid() {
		return returnid;
	}

	public void setReturnid(int returnid) {
		this.returnid = returnid;
	}
	
	
}
