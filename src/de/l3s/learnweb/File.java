package de.l3s.learnweb;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;

import de.l3s.util.HasId;

public class File implements Serializable, HasId
{
    private static final long serialVersionUID = 6573841175365679674L;

    public final static int ORIGINAL_FILE = 4;

    private int fileId = -1;
    private String name;
    private String mimeType;
    private int resourceId;
    private int resourceFileNumber;
    private String url;
    private boolean downloadLogActivated = false;
    private Date lastModified;

    public java.io.File actualFile;

    public File()
    {
    }

    public String getName()
    {
	return name;
    }

    public void setName(String name)
    {
	this.name = name;
    }

    public String getMimeType()
    {
	return mimeType;
    }

    public void setMimeType(String mimeType)
    {
	this.mimeType = mimeType;
    }

    public int getResourceId()
    {
	return resourceId;
    }

    public void setResourceId(int resourceId)
    {
	this.resourceId = resourceId;
    }

    public int getResourceFileNumber()
    {
	return resourceFileNumber;
    }

    public void setResourceFileNumber(int resourceFileNumber)
    {
	this.resourceFileNumber = resourceFileNumber;
    }

    @Override
    public int getId()
    {
	return fileId;
    }

    protected void setId(int fileId)
    {
	this.fileId = fileId;
    }

    public String getUrl()
    {
	return url;
    }

    public void setUrl(String url)
    {
	this.url = url;
    }

    public boolean isDownloadLogActivated()
    {
	return downloadLogActivated;
    }

    public void setDownloadLogActivated(boolean downloadLogActivated)
    {
	this.downloadLogActivated = downloadLogActivated;
    }

    /**
     * 
     * @return The actual file in the file system
     */
    public java.io.File getActualFile()
    {
	if(null == actualFile)
	{
	    throw new IllegalStateException("The file should be saved first: FileManager.save()");
	}
	return actualFile;
    }

    protected void setActualFile(java.io.File actualFile)
    {
	this.actualFile = actualFile;
    }

    public OutputStream getOutputStream()
    {
	try
	{
	    return new FileOutputStream(getActualFile());
	}
	catch(FileNotFoundException e) // the FileManager has to take care that this exception never occurs
	{
	    throw new RuntimeException(e);
	}
    }

    public InputStream getInputStream()
    {
	try
	{
	    return new FileInputStream(getActualFile());
	}
	catch(FileNotFoundException e) // the FileManager has to take care that is exception never occurs
	{
	    throw new RuntimeException(e);
	}
    }

    public long getLength()
    {
	return getActualFile().length();
    }

    public Date getLastModified()
    {
	return lastModified;
    }

    public void setLastModified(Date lastModified)
    {
	this.lastModified = lastModified;
    }

}
