package de.l3s.searchlogclient.jaxb;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "HistorybyDate")
public class HistoryByDate {

	private String date;
	ArrayList<QueryHistory> queryHistory;
	
	public HistoryByDate()
	{
		date="";
		queryHistory = new ArrayList<QueryHistory>();
	}
	
	@XmlElement
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	
	public ArrayList<QueryHistory> getQueryHistory() {
		return queryHistory;
	}
	public void setQueryHistory(ArrayList<QueryHistory> queryHistory) {
		this.queryHistory = queryHistory;
	}
	
}
