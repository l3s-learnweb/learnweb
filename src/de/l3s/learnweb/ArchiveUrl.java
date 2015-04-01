package de.l3s.learnweb;

import java.io.Serializable;

public class ArchiveUrl implements Serializable
{

    private static final long serialVersionUID = 63994605834754451L;
    private String archiveUrl;
    private String timestamp;

    public ArchiveUrl(String archiveUrl, String timestamp)
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

    public String getTimestamp()
    {
	return timestamp;
    }

    public void setTimestamp(String timestamp)
    {
	this.timestamp = timestamp;
    }

}
