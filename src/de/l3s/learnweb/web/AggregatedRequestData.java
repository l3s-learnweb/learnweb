package de.l3s.learnweb.web;

import java.io.Serializable;
import java.util.Date;

/**
 * Entity class holding aggregated request data from the database.
 *
 * @author Kate
 */
public class AggregatedRequestData implements Serializable {
    private static final long serialVersionUID = 2159959917673844305L;

    private String ip;
    private int requests;
    private int loginCount;
    private String usernames;
    private Date time;

    public AggregatedRequestData() {
    }

    public AggregatedRequestData(String ip, int requests, int loginCount, String usernames, Date time) {
        this.ip = ip;
        this.requests = requests;
        this.loginCount = loginCount;
        this.usernames = usernames;
        this.time = time;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String iP) {
        ip = iP;
    }

    public int getRequests() {
        return requests;
    }

    public void setRequests(int requests) {
        this.requests = requests;
    }

    public int getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(int loginCount) {
        this.loginCount = loginCount;
    }

    public String getUsernames() {
        return usernames;
    }

    public void setUsernames(String usernames) {
        this.usernames = usernames;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

}
