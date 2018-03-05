package de.l3s.learnweb.web;

import java.util.Date;

/**
 * Small object holding the IP name, time and URL that the request visited.
 *
 * @author Kate
 *
 */
public class RequestData
{
    private String IP;
    private Date time;
    private String URL;

    public RequestData(String i, Date t, String u)
    {
        setIP(i);
        setTime(t);
        setURL(u);
    }

    public String getIP()
    {
        return IP;
    }

    public void setIP(String iP)
    {
        IP = iP;
    }

    public Date getTime()
    {
        return time;
    }

    public void setTime(Date time)
    {
        this.time = time;
    }

    public String getURL()
    {
        return URL;
    }

    public void setURL(String uRL)
    {
        URL = uRL;
    }

}
