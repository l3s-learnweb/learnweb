package de.l3s.learnweb.resource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import de.l3s.util.HasId;

public class File implements Serializable, HasId
{
    private static final long serialVersionUID = 6573841175365679674L;

    public enum TYPE
    {
        UNKNOWN, // 0
        THUMBNAIL_SQUARED, // 1
        THUMBNAIL_SMALL, // 2
        THUMBNAIL_MEDIUM, // 3
        FILE_MAIN, // 4 the file that can be downloaded/viewed
        THUMBNAIL_LARGE, // 5
        THUMBNAIL_VERY_SMALL, // 6
        FILE_ORIGINAL, // 7 if the file was converted the original file should be moved to this location
        PROFILE_PICTURE, // 8
        SYSTEM_FILE, // 9 for example course header images
        HISTORY_FILE, // 10 previous version of an office file
        CHANGES, // 11 zip file with changes for office resources
    }

    private int fileId = -1;
    private String name;
    private String mimeType;
    private int resourceId;
    private TYPE type;
    private String url;
    private boolean downloadLogActivated = false;
    private Date lastModified;

    private java.io.File actualFile;
    private boolean actualFileExists = true; // when the actual file doesn't exist it is replaced by an error image. For this reason we have to store if the file exists

    public File()
    {
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = StringUtils.defaultString(name).replaceAll("[\\\\/:*?\"<>|]", "_"); // replace invalid characters
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

    @Override
    public int getId()
    {
        return fileId;
    }

    public void setId(int fileId)
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

    public boolean exists()
    {
        return actualFileExists;
    }

    public void setExists(boolean actualFileExists)
    {
        this.actualFileExists = actualFileExists;
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

    @Override
    public String toString()
    {
        return "File [fileId=" + fileId + ", name=" + name + ", mimeType=" + mimeType + ", resourceId=" + resourceId + ", type=" + type + ", url=" + url + ", lastModified=" + lastModified + "]";
    }

    public TYPE getType()
    {
        return type;
    }

    public void setType(TYPE type)
    {
        this.type = type;
    }

}
