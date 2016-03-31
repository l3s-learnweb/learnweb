package de.l3s.learnweb;

import java.io.Serializable;
import java.util.Date;

public class ArchiveUrl implements Serializable
{

    private static final long serialVersionUID = 63994605834754451L;
    private String archiveUrl;
    private Date timestamp;
    private long htmlText;
    private long htmlTags;
    private int fileId;

    public ArchiveUrl(String archiveUrl, Date timestamp)
    {
	this.archiveUrl = archiveUrl;
	this.timestamp = timestamp;
    }

    public String getArchiveUrl()
    {
	return archiveUrl;
    }

    public long gethtmltags()
    {
	return htmlTags;
    }

    public long gethtmltext()
    {
	return htmlText;
    }

    public int getfileId()
    {
	return fileId;
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

    @Override
    public boolean equals(Object o)
    {
	if(o == null)
	    return false;
	if(!(o instanceof ArchiveUrl))
	    return false;

	ArchiveUrl other = (ArchiveUrl) o;
	if(!this.archiveUrl.equalsIgnoreCase(other.archiveUrl))
	    return false;
	if(!this.timestamp.equals(other.timestamp))
	    return false;

	return true;
    }

    public void setFileId(int fileId)
    {
	this.fileId = fileId;
    }
}
