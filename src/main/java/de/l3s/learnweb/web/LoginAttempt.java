package de.l3s.learnweb.web;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Holds login attempts by selected ip and username. Used by AnalysingProtectionManager for... analysing... yeah.
 *
 * @author Kate
 */
public class LoginAttempt implements Serializable {
    @Serial
    private static final long serialVersionUID = 8349103575359014279L;

    private String addr;
    private String username;
    private boolean success;
    private LocalDateTime createdAt;

    public LoginAttempt(String addr, String username, boolean success) {
        this.addr = addr;
        this.username = username;
        this.success = success;
        this.createdAt = LocalDateTime.now();
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String iP) {
        addr = iP;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
