package de.l3s.learnweb.user.loginProtection;

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

    private String type;
    private String name;
    private String reason;
    private int attempts;
    private LocalDateTime bannedUntil; // TODO rename: bandate
    private LocalDateTime bannedOn;

    private int allowedAttempts;

    /**
     * Default constructor for new data.
     */
    public Ban(String name) {
        this.name = name;
        this.attempts = 0;
        this.allowedAttempts = 50;
    }

    /**
     * Resets login attempts.
     */
    public void reset() {
        attempts = 0;
    }

    /**
     * Bans the user for a given amount of minutes starting from now.
     */
    public void ban(int days, int hours, int minutes) {
        setBannedUntil(LocalDateTime.now().minusDays(days).minusHours(hours).minusMinutes(minutes));
        setBannedOn(LocalDateTime.now());
    }

    /**
     * Resets ban time and banned on date.
     */
    public void unban() {
        bannedUntil = null;
        bannedOn = null;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        this.attempts = Math.max(attempts, 0);
    }

    public int getAllowedAttempts() {
        return allowedAttempts;
    }

    public void setAllowedAttempts(int allowedAttempts) {
        this.allowedAttempts = allowedAttempts;
    }

    public void logAttempt() {
        attempts++;
        allowedAttempts--;
    }

    public LocalDateTime getBannedUntil() {
        return bannedUntil;
    }

    public void setBannedUntil(LocalDateTime bannedUntil) {
        this.bannedUntil = bannedUntil;
    }

    public LocalDateTime getBannedOn() {
        return bannedOn;
    }

    public void setBannedOn(LocalDateTime bannedOn) {
        this.bannedOn = bannedOn;
    }
}
