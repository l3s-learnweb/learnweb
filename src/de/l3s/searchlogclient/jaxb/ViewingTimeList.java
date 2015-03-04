package de.l3s.searchlogclient.jaxb;

import java.util.LinkedList;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="ViewingTimeList")
public class ViewingTimeList {

	LinkedList<ViewingTime> viewingTimeList;

	public LinkedList<ViewingTime> getViewingTimeList() {
		return viewingTimeList;
	}

	public void setViewingTimeList(LinkedList<ViewingTime> viewingTimeList) {
		this.viewingTimeList = viewingTimeList;
	}

}
