package de.l3s.learnweb;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.l3s.learnweb.beans.UtilBean;

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

    public String getTimestamp()
    {
	return new SimpleDateFormat("MMM d, yyyy HH:mm:ss", UtilBean.getUserBean().getLocale()).format(timestamp);
    }

    public void setTimestamp(Date timestamp)
    {
	this.timestamp = timestamp;
    }

}
