package de.l3s.searchlogclient.jaxb;

import java.util.LinkedList;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "QuerylogList")
public class QueryLogList
{

    LinkedList<QueryLog> queryLogList;

    public LinkedList<QueryLog> getQueryLogList()
    {
        return queryLogList;
    }

    public void setQueryLogList(LinkedList<QueryLog> queryLogList)
    {
        this.queryLogList = queryLogList;
    }

}
