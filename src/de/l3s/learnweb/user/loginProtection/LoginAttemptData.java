package de.l3s.learnweb.user.loginProtection;

import java.util.Date;

/**
 * Holds login attempts by selected ip and username. Used by AnalysingProtectionManager for... analysing... yeah.
 *
 * @author Kate
 */
public class LoginAttemptData {
    private String ip;
    private String username;
    private boolean success;
    private Date timestamp;

    public LoginAttemptData(String ip, String username, boolean success, Date timestamp) {
        this.ip = ip;
        this.username = username;
        this.success = success;
        this.timestamp = timestamp;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String iP) {
        ip = iP;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

}
