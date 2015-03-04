package de.l3s.searchlogclient.jaxb;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Resource")
public class Resource {

	int resourceId;
	String url;
	String type;
	String source;
	String filename;
	String shortdescrp;
	int thumbnail_height;
	int thumbnail_width;
	String thumbnail_url;
	int thumbnail4_height;
	int thumbnail4_width;
	String thumbnail4_url;
	
	int resultsetId;
	int resource_rank;

	public Resource(){}
	
	//For creating the resultset
	public Resource(int resourceId, String url, String type, String source,String filename,String shortdescrp,int thumbnail_height,int thumbnail_width,String thumbnail_url,
			        int thumbnail4_height, int thumbnail4_width, String thumbnail4_url, int resultsetId,int resource_rank){
		this.resourceId=resourceId;
		this.url=url;
		this.type=type;
		this.source=source;
		this.filename=filename;
		this.shortdescrp=shortdescrp;
		this.thumbnail_height = thumbnail_height;
		this.thumbnail_width = thumbnail_width;
		this.thumbnail_url = thumbnail_url;
		this.thumbnail4_height = thumbnail4_height;
		this.thumbnail4_width = thumbnail4_width;
		this.thumbnail4_url = thumbnail4_url;
		this.resultsetId = resultsetId;
		this.resource_rank = resource_rank;
	}
	
	//For creating the resultset
	public Resource(int resourceId, String url, String type, String source,String filename,String shortdescrp,int resultsetId){
		this.resourceId=resourceId;
		this.url=url;
		this.type=type;
		this.source=source;
		this.filename=filename;
		this.shortdescrp=shortdescrp;
		this.thumbnail_height = 0;
		this.thumbnail_width = 0;
		this.thumbnail_url = "";
		this.thumbnail4_height = 0;
		this.thumbnail4_width = 0;
		this.thumbnail4_url = "";
		this.resultsetId = resultsetId;
		this.resource_rank = 0;
	}
	
	public Resource(int resourceId, String url, String type, String source,String filename,String shortdescrp,int resultsetId,int resource_rank){
		this.resourceId=resourceId;
		this.url=url;
		this.type=type;
		this.source=source;
		this.filename=filename;
		this.shortdescrp=shortdescrp;
		this.thumbnail_height = 0;
		this.thumbnail_width = 0;
		this.thumbnail_url = "";
		this.thumbnail4_height = 0;
		this.thumbnail4_width = 0;
		this.thumbnail4_url = "";
		this.resultsetId = resultsetId;
		this.resource_rank = resource_rank;
	}
	
	//For updating the resultset
	public Resource(int resourceId, int resultsetId, int systemId){
		this.resourceId = systemId;
		this.resultsetId = resultsetId;
		this.url = "null";
		this.type = "null";
		this.source = "null";
		this.filename = "null";
		this.shortdescrp = "null";
		this.thumbnail_height = 0;
		this.thumbnail_width = 0;
		this.thumbnail_url = "";
		this.thumbnail4_height = 0;
		this.thumbnail4_width = 0;
		this.thumbnail4_url = "";		
		this.resource_rank = resourceId;
	}
	
	
	@XmlElement
	public int getResourceId() {
		return resourceId;
	}
	public void setResourceId(int resourceId) {
		this.resourceId = resourceId;
	}
	
	@XmlElement
	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	@XmlElement
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	@XmlElement
	public String getSource() {
		return source;
	}
	
	public void setSource(String source) {
		this.source = source;
	}
	
	@XmlElement
	public String getFilename() {
		return filename;
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	@XmlElement
	public String getShortdescrp() {
		return shortdescrp;
	}
	public void setShortdescrp(String shortdescrp) {
		this.shortdescrp = shortdescrp;
	}
	
	@XmlElement
	public int getResultsetId() {
		return resultsetId;
	}
	public void setResultsetId(int resultsetId) {
		this.resultsetId = resultsetId;
	}

	@XmlElement
	public int getResource_rank() {
		return resource_rank;
	}

	public void setResource_rank(int resource_rank) {
		this.resource_rank = resource_rank;
	}

	@XmlElement
	public int getThumbnail_height() {
		return thumbnail_height;
	}

	
	public void setThumbnail_height(int thumbnail_height) {
		this.thumbnail_height = thumbnail_height;
	}

	@XmlElement
	public int getThumbnail_width() {
		return thumbnail_width;
	}

	public void setThumbnail_width(int thumbnail_width) {
		this.thumbnail_width = thumbnail_width;
	}

	@XmlElement
	public String getThumbnail_url() {
		return thumbnail_url;
	}

	public void setThumbnail_url(String thumbnail_url) {
		this.thumbnail_url = thumbnail_url;
	}

	@XmlElement
	public int getThumbnail4_height() {
		return thumbnail4_height;
	}

	public void setThumbnail4_height(int thumbnail4_height) {
		this.thumbnail4_height = thumbnail4_height;
	}
	
	@XmlElement
	public int getThumbnail4_width() {
		return thumbnail4_width;
	}

	public void setThumbnail4_width(int thumbnail4_width) {
		this.thumbnail4_width = thumbnail4_width;
	}

	@XmlElement
	public String getThumbnail4_url() {
		return thumbnail4_url;
	}

	public void setThumbnail4_url(String thumbnail4_url) {
		this.thumbnail4_url = thumbnail4_url;
	}
	
}
