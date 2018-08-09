package de.l3s.learnweb.web;

import java.io.Serializable;
import java.util.Date;

/**
 * Entity class holding aggregated request data from the database.
 *
 * @author Kate
 *
 */
public class AggregatedRequestData implements Serializable
{
    private static final long serialVersionUID = 2159959917673844305L;

    private String IP;
    private int requests;
    private int loginCount;
    private String usernames;
    private Date time;

    public AggregatedRequestData()
    {
        super();
    }

    public AggregatedRequestData(String iP, int requests, int loginCount, String usernames, Date time)
    {
        super();
        IP = iP;
        this.requests = requests;
        this.loginCount = loginCount;
        this.usernames = usernames;
        this.time = time;
    }

    public String getIP()
    {
        return IP;
    }

    public int getRequests()
    {
        return requests;
    }

    public int getLoginCount()
    {
        return loginCount;
    }

    public String getUsernames()
    {
        return usernames;
    }

    public void setIP(String iP)
    {
        IP = iP;
    }

    public void setRequests(int requests)
    {
        this.requests = requests;
    }

    public void setLoginCount(int loginCount)
    {
        this.loginCount = loginCount;
    }

    public void setUsernames(String usernames)
    {
        this.usernames = usernames;
    }

    public Date getTime()
    {
        return time;
    }

    public void setTime(Date time)
    {
        this.time = time;
    }

}
