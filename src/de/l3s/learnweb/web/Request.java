package de.l3s.learnweb.web;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity class holding aggregated request data from the database.
 *
 * @author Kate
 */
public class Request implements Serializable {
    private static final long serialVersionUID = 2159959917673844305L;

    private final String ip;
    private final String url;
    private int requests;
    private int loginCount;
    private String usernames;
    private LocalDateTime time;

    public Request(final String ip, final String url) {
        this.ip = ip;
        this.url = url;
        this.time = LocalDateTime.now();
    }

    public String getIp() {
        return ip;
    }

    public String getUrl() {
        return url;
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

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}
