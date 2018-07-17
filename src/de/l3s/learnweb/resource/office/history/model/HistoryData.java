package de.l3s.learnweb.resource.office.history.model;

public class HistoryData
{
    private String changesUrl;

    private String key;

    private FileData previous;

    private String url; // of current file 

    private int version;

    public String getChangesUrl()
    {
        return changesUrl;
    }

    public void setChangesUrl(String changesUrl)
    {
        this.changesUrl = changesUrl;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public FileData getPrevious()
    {
        return previous;
    }

    public void setPrevious(FileData previous)
    {
        this.previous = previous;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

}
