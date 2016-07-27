package de.l3s.learnweb.beans;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

@ManagedBean
@SessionScoped
public class FactSheetBean
{

    String search;

    public String getSearch()
    {
	return search;
    }

    public void setSearch(String search)
    {
	this.search = search;
    }

}
