package de.l3s.searchlogclient.jaxb;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Tag
{

    private int tagId;
    private String name;
    private int userId;
    private int resultsetId;

    public Tag()
    {
    }

    public Tag(int userId, int resultsetId, String name)
    {
        this.userId = userId;
        this.resultsetId = resultsetId;
        this.name = name;
    }

    public Tag(int userId, int resultsetId, int tagId, String name)
    {
        this.userId = userId;
        this.resultsetId = resultsetId;
        this.tagId = tagId;
        this.name = name;
    }

    @XmlElement
    public int getTagId()
    {
        return tagId;
    }

    public void setTagId(int tagId)
    {
        this.tagId = tagId;
    }

    @XmlElement
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @XmlElement
    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
    }

    @XmlElement
    public int getResultsetId()
    {
        return resultsetId;
    }

    public void setResultsetId(int resultsetId)
    {
        this.resultsetId = resultsetId;
    }

}
