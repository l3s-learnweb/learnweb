package de.l3s.searchlogclient.jaxb;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SharedResultset {

	private int userSharing;
	private int resultsetId;
	private String md5value;
	private String query;
	private String queryTimestamp;
	
	public SharedResultset(){
	}
	
	public SharedResultset(int userSharing, int resultsetId, String md5value, String query, String queryTimestamp){
		this.userSharing = userSharing;
		this.resultsetId = resultsetId;
		this.md5value = md5value;
		this.query = query;
		SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat dateToString = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
		Date queryDate = null;
		try {
			queryDate = stringToDate.parse(queryTimestamp);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		this.queryTimestamp = dateToString.format(queryDate);
	}

	@XmlElement
	public int getUserSharing() {
		return userSharing;
	}
	public void setUserSharing(int userSharing) {
		this.userSharing = userSharing;
	}
	
	@XmlElement
	public int getResultsetId() {
		return resultsetId;
	}
	public void setResultsetId(int resultsetId) {
		this.resultsetId = resultsetId;
	}
	
	@XmlElement
	public String getMd5value() {
		return md5value;
	}
	public void setMd5value(String md5value) {
		this.md5value = md5value;
	}
	
	@XmlElement
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}
	
	@XmlElement
	public String getQueryTimestamp() {
		return queryTimestamp;
	}
	public void setQueryTimestamp(String queryTimestamp) {
		this.queryTimestamp = queryTimestamp;
	}

}
