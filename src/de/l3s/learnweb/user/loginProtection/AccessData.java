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
 *
 */
public class AccessData implements Serializable
{
    private static final long serialVersionUID = -2074629486516643542L;

    private int attempts;
    private String name;
    private Date banDate;
    private Date bannedOn;
    private int allowedAttempts;

    /**
     * Default constructor for new data.
     *
     * @param name
     */
    public AccessData(String name)
    {
        attempts = 0;
        this.name = name;
        allowedAttempts = 50;
    }

    /**
     * Used when getting data from ban list
     */
    public AccessData(String name, int attempts, Date bannedUntil, Date bannedOn)
    {
        this.attempts = attempts;
        this.name = name;
        this.banDate = bannedUntil;
        this.bannedOn = bannedOn;
        allowedAttempts = 50;
    }

    /**
     * Resets login attempts
     */
    public void reset()
    {
        attempts = 0;
    }

    /**
     * Resets ban time and banned on date
     */
    public void unban()
    {
        banDate = null;
        bannedOn = null;
    }

    /**
     * Bans the user for a given amount of minutes starting from now.
     */
    public void setBan(int days, int hours, int minutes)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());

        bannedOn = cal.getTime();

        cal.add(Calendar.DAY_OF_MONTH, days);
        cal.add(Calendar.HOUR, hours);
        cal.add(Calendar.MINUTE, minutes);

        banDate = cal.getTime();
    }

    /**
     * Bans user for virtually forever (400 years)
     */
    public void permaban()
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.YEAR, 400);

        banDate = cal.getTime();
        bannedOn = cal.getTime();

    }

    public int getAttempts()
    {
        return attempts;
    }

    public void logAttempt()
    {
        attempts++;
        allowedAttempts--;
    }

    public void setAttempts(int attempts)
    {
        if(attempts > 0)
        {
            this.attempts = attempts;
        }
        else
        {
            this.attempts = 0;
        }
    }

    public Date getBanDate()
    {
        return banDate;
    }

    public void setBanDate(Date banDate)
    {
        this.banDate = banDate;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Date getBannedOn()
    {
        return bannedOn;
    }

    public void setBannedOn(Date bannedOn)
    {
        this.bannedOn = bannedOn;
    }

    public int getAllowedAttempts()
    {
        return allowedAttempts;
    }

    public void setAllowedAttempts(int allowedAttempts)
    {
        this.allowedAttempts = allowedAttempts;
    }
}
