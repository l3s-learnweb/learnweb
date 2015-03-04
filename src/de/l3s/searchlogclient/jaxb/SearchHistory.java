package de.l3s.searchlogclient.jaxb;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "SearchHistory")
public class SearchHistory {

	private int resultsetId;
	private int resourceRank;
	private int userId;
	private String query;
	private String searchType;
	private Date queryTimestamp;
	private String URL;
	private String filename;
	private String source;
	private String action;
	private Date actionTimestamp;
	
	public SearchHistory(){
		
	}
	
	public SearchHistory(int resultsetId,int resourceRank,int userId,String query,String searchType,String queryTimestamp,String URL,String filename,
				  String source,String action,String actionTimestamp){
		
		//For converting timestamp in String to Date object
		SimpleDateFormat stringtodatef = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		this.resultsetId = resultsetId;
		this.resourceRank = resourceRank;
		this.userId = userId;
		this.query = query;
		this.searchType = searchType;
		this.URL = URL;
		this.filename = filename;
		this.source = source;
		this.action = action;
		try {
			this.queryTimestamp = stringtodatef.parse(queryTimestamp);
			if((actionTimestamp!=null)&&(!actionTimestamp.equals("")))
				this.actionTimestamp = stringtodatef.parse(actionTimestamp);
			else
				this.actionTimestamp = null;
		} catch (ParseException e) {
			e.printStackTrace();
		}
			
	}

	@XmlElement
	public int getResultsetId() {
		return resultsetId;
	}

	public void setResultsetId(int resultsetId) {
		this.resultsetId = resultsetId;
	}

	@XmlElement
	public int getResourceRank() {
		return resourceRank;
	}

	public void setResourceRank(int resourceRank) {
		this.resourceRank = resourceRank;
	}

	@XmlElement
	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	@XmlElement
	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	@XmlElement
	public String getSearchType() {
		return searchType;
	}

	public void setSearchType(String searchType) {
		this.searchType = searchType;
	}

	@XmlElement
	public Date getQueryTimestamp() {
		return queryTimestamp;
	}

	public void setQueryTimestamp(Date queryTimestamp) {
		this.queryTimestamp = queryTimestamp;
	}

	@XmlElement
	public String getURL() {
		return URL;
	}

	public void setURL(String uRL) {
		URL = uRL;
	}

	@XmlElement
	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	@XmlElement
	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	@XmlElement
	public Date getActionTimestamp() {
		return actionTimestamp;
	}

	public void setActionTimestamp(Date actionTimestamp) {
		this.actionTimestamp = actionTimestamp;
	}

	@XmlElement
	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
	
}
