package de.l3s.learnweb;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ArchiveUrl implements Serializable
{

    private static final long serialVersionUID = 63994605834754451L;
    private String archiveUrl;
    private String fileUrl;
    private Date timestamp;
    private int fileId;
    private long simhash;
    private int shingleId;
    private SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");

    public String getFileUrl()
    {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl)
    {
        this.fileUrl = fileUrl;
    }

    public ArchiveUrl(String archiveUrl, Date timestamp)
    {
        this.archiveUrl = archiveUrl;
        this.timestamp = timestamp;
    }

    public ArchiveUrl(String archiveUrl, Date timestamp, long simhash)
    {
        this.archiveUrl = archiveUrl;
        this.timestamp = timestamp;
        this.simhash = simhash;
    }

    public ArchiveUrl(String archiveUrl, String fileUrl, Date timestamp)
    {
        this.archiveUrl = archiveUrl;
        this.fileUrl = fileUrl;
        this.timestamp = timestamp;
    }

    public ArchiveUrl(String archiveUrl, Date timestamp, long simhash, int shingleId)
    {
        this.archiveUrl = archiveUrl;
        this.timestamp = timestamp;
        this.simhash = simhash;
        this.shingleId = shingleId;
    }

    public int getShingleId()
    {
        return shingleId;
    }

    public String getArchiveUrl()
    {
        return archiveUrl;
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

    public String getTimestampInArchiveFormat()
    {
        return df.format(timestamp);
    }

    public long getSimhash()
    {
        return simhash;
    }

    public void setSimhash(long simhash)
    {
        this.simhash = simhash;
    }

    @Override
    public boolean equals(Object o)
    {
        if(o == null || !(o instanceof ArchiveUrl))
            return false;

        ArchiveUrl other = (ArchiveUrl) o;
        if(!this.archiveUrl.equalsIgnoreCase(other.archiveUrl) || !this.timestamp.equals(other.timestamp))
            return false;

        return true;
    }

    @Override
    public String toString()
    {
        return "[" + this.archiveUrl + ", " + this.timestamp + "]";
    }

}
