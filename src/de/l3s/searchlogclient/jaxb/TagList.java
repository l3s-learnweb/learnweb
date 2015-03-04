package de.l3s.searchlogclient.jaxb;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TagList {

	private ArrayList<Tag> tags;

	public TagList(){
		tags = new ArrayList<Tag>();
	}
	
	@XmlElement(name="tag")
	public ArrayList<Tag> getTags() {
		return tags;
	}

	public void setTags(ArrayList<Tag> tags) {
		this.tags = tags;
	}
	
}
