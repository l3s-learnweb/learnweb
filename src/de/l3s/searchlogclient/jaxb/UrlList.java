package de.l3s.searchlogclient.jaxb;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "URLlist")
public class UrlList {

	private ArrayList<String> resources_urls;

	public UrlList() {
		resources_urls = new ArrayList<String>();
	}
	
	@XmlElement(name="resource_url")
	public ArrayList<String> getResources_urls() {
		return resources_urls;
	}

	public void setResources_urls(ArrayList<String> resources_urls) {
		this.resources_urls = resources_urls;
	}
	
}
