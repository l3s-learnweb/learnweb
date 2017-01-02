package de.l3s.searchlogclient.jaxb;

import java.util.LinkedList;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "SearchHistoryList")
public class SearchHistoryList
{

    LinkedList<SearchHistory> searchHistoryList;

    public LinkedList<SearchHistory> getSearchHistoryList()
    {
        return searchHistoryList;
    }

    public void setSearchHistoryList(LinkedList<SearchHistory> searchHistoryList)
    {
        this.searchHistoryList = searchHistoryList;
    }

}
