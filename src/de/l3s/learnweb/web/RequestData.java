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
    public String IP;
    public Date time;
    public String URL;

    public RequestData(String i, Date t, String u)
    {
        IP = i;
        time = t;
        URL = u;
    }
}
