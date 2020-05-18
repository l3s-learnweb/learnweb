package de.l3s.learnweb.web;

import java.io.Serializable;
import java.util.Date;

/**
 * Small object holding the IP name, time and URL that the request visited.
 *
 * @author Kate
 */
public class RequestData implements Serializable {
    private static final long serialVersionUID = -5311597999329037961L;

    private String ip;
    private Date time;
    private String url;

    public RequestData(String ip, Date time, String url) {
        setIp(ip);
        setTime(time);
        setUrl(url);
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String iP) {
        ip = iP;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String uRL) {
        url = uRL;
    }

}
