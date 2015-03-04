package de.l3s.searchlogclient.jaxb;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="HistorybyDatelist")
public class HistoryByDateList {

	ArrayList<HistoryByDate> historyByDates;

	public HistoryByDateList()
	{
		historyByDates = new ArrayList<HistoryByDate>();
	}
	
	public ArrayList<HistoryByDate> getHistoryByDates() {
		return historyByDates;
	}

	public void setHistoryByDates(ArrayList<HistoryByDate> historyByDates) {
		this.historyByDates = historyByDates;
	}
}


