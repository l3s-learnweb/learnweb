package de.l3s.searchlogclient.jaxb;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ResultsetIdList")
public class ResultSetIdList
{

    ArrayList<Integer> resultsetId;

    public ResultSetIdList()
    {
        resultsetId = new ArrayList<Integer>();
    }

    @XmlElement
    public ArrayList<Integer> getResultsetId()
    {
        return resultsetId;
    }

    public void setResultsetId(ArrayList<Integer> resultsetId)
    {
        this.resultsetId = resultsetId;
    }
}
