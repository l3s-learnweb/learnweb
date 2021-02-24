package de.l3s.learnweb.web;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Holds login attempts by selected ip and username. Used by AnalysingProtectionManager for... analysing... yeah.
 *
 * @author Kate
 */
public class LoginAttempt implements Serializable {
    private static final long serialVersionUID = 8349103575359014279L;

    private String ip;
    private String username;
    private boolean success;
    private LocalDateTime dateTime;

    public LoginAttempt(String ip, String username, boolean success) {
        this.ip = ip;
        this.username = username;
        this.success = success;
        this.dateTime = LocalDateTime.now();
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

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

}