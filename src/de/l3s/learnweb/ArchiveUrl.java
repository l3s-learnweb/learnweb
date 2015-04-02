package de.l3s.learnweb;

import java.io.Serializable;
import java.util.Date;

public class ArchiveUrl implements Serializable
{

    private static final long serialVersionUID = 63994605834754451L;
    private String archiveUrl;
    private Date timestamp;

    public ArchiveUrl(String archiveUrl, Date timestamp)
    {
	this.archiveUrl = archiveUrl;
	this.timestamp = timestamp;
    }

    public String getArchiveUrl()
    {
	return archiveUrl;
    }

    public void setArchiveUrl(String archiveUrl)
    {
	this.archiveUrl = archiveUrl;
    }

    public Date getTimestamp()
    {
	return timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
	this.timestamp = timestamp;
    }

}
