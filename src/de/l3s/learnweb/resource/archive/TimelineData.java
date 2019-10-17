package de.l3s.learnweb.resource.archive;

import java.util.Date;

public class TimelineData
{

    private Date timestamp;
    private int numberOfVersions;

    public TimelineData(Date timestamp, int numberOfVersions)
    {
        this.timestamp = timestamp;
        this.numberOfVersions = numberOfVersions;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }

    public int getNumberOfVersions()
    {
        return numberOfVersions;
    }

    public void setNumberOfVersions(int numberOfVersions)
    {
        this.numberOfVersions = numberOfVersions;
    }
}
