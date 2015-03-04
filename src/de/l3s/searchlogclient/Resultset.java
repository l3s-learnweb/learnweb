package de.l3s.searchlogclient;

public class Resultset {
	public int resultsetid;
	public int resourceid;
	public String url;
	public String type;
	public String source;
	public String filename;
	public String shortdescrp;
	public String system;
	public int systemid;
	public String selected;

	public Resultset() {
		// TODO Auto-generated constructor stub
	}
	
	public Resultset(int resultsetid,int resourceid,String url, String type, String source, String filename, String shortdescrp, String system,int systemid,String selected){
		this.resultsetid = resultsetid;
		this.resourceid= resourceid;
		this.url=url;
		this.type = type;
		this.source = source;
		this.filename = filename;
		this.shortdescrp = shortdescrp;
		this.system = system;
		this.systemid = systemid;
		this.selected = selected;
	}

	public int getResourceid() {
		return resourceid;
	}

	public void setResourceid(int resourceid) {
		this.resourceid = resourceid;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getShortdescrp() {
		return shortdescrp;
	}

	public void setShortdescrp(String shortdescrp) {
		this.shortdescrp = shortdescrp;
	}

	public String getSystem() {
		return system;
	}

	public void setSystem(String system) {
		this.system = system;
	}

	public int getSystemid() {
		return systemid;
	}

	public void setSystemid(int systemid) {
		this.systemid = systemid;
	}

	public String getSelected() {
		return selected;
	}

	public void setSelected(String selected) {
		this.selected = selected;
	}

	public int getResultsetid() {
		return resultsetid;
	}

	public void setResultsetid(int resultsetid) {
		this.resultsetid = resultsetid;
	}

}
