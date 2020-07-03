package de.l3s.learnweb.user.loginProtection;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 * Small object to hold data about login attempts per IP or username.
 * <br>
 * Contains: number of failed attempts and the time when the ban is lifted (set to 1970 if empty)
 *
 * @author Kate
 */
public class AccessData implements Serializable {
    private static final long serialVersionUID = -2074629486516643542L;

    private String type;
    private String name;
    private Date banDate;
    private Date bannedOn;
    private int attempts;
    private int allowedAttempts;
    private String reason;

    /**
     * Default constructor for new data.
     */
    public AccessData(String name) {
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
     * Resets ban time and banned on date.
     */
    public void unban() {
        banDate = null;
        bannedOn = null;
    }

    /**
     * Bans the user for a given amount of minutes starting from now.
     */
    public void setBan(int days, int hours, int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());

        bannedOn = cal.getTime();

        cal.add(Calendar.DAY_OF_MONTH, days);
        cal.add(Calendar.HOUR, hours);
        cal.add(Calendar.MINUTE, minutes);

        banDate = cal.getTime();
    }

    /**
     * Bans user for virtually forever (400 years).
     */
    public void permaban() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.YEAR, 400);

        banDate = cal.getTime();
        bannedOn = cal.getTime();
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

    public Date getBanDate() {
        return banDate;
    }

    public void setBanDate(Date banDate) {
        this.banDate = banDate;
    }

    public Date getBannedOn() {
        return bannedOn;
    }

    public void setBannedOn(Date bannedOn) {
        this.bannedOn = bannedOn;
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

    public String getReason() {
        return reason;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }
}
