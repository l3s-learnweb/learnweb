package de.l3s.learnweb.web;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Small object to hold data about login attempts per IP or username.
 * Contains: number of failed attempts and the time when the ban is lifted (set to 1970 if empty)
 *
 * @author Kate
 */
public class Ban implements Serializable {
    private static final long serialVersionUID = -2074629486516643542L;

    private final String addr;
    private String reason;
    private int attempts;
    private LocalDateTime expires;
    private LocalDateTime createdAt;

    private int allowedAttempts;

    /**
     * Default constructor for new data.
     */
    public Ban(String addr) {
        this.addr = addr;
        this.attempts = 0;
        this.allowedAttempts = 50;
    }

    public void logAttempt() {
        attempts++;
        allowedAttempts--;
    }

    public void resetAttempts() {
        attempts = 0;
    }

    public String getAddr() {
        return addr;
    }

    public LocalDateTime getExpires() {
        return expires;
    }

    public void setExpires(final LocalDateTime expires) {
        this.expires = expires;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getAllowedAttempts() {
        return allowedAttempts;
    }

    public void setAllowedAttempts(int allowedAttempts) {
        this.allowedAttempts = allowedAttempts;
    }
}
