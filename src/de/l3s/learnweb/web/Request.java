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

    private final String addr;
    private final String url;
    private int requests;
    private int loginCount;
    private String usernames;
    private LocalDateTime createdAt;

    public Request(final String addr, final String url) {
        this.addr = addr;
        this.url = url;
        this.createdAt = LocalDateTime.now();
    }

    public String getAddr() {
        return addr;
    }

    public String getUrl() {
        return url;
    }

    public int getRequests() {
        return requests;
    }

    void setRequests(int requests) {
        this.requests = requests;
    }

    public int getLoginCount() {
        return loginCount;
    }

    void setLoginCount(int loginCount) {
        this.loginCount = loginCount;
    }

    public String getUsernames() {
        return usernames;
    }

    void setUsernames(String usernames) {
        this.usernames = usernames;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
