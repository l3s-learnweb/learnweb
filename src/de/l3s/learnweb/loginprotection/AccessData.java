package de.l3s.learnweb.loginprotection;

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
public class AccessData
{
    public int attempts;
    public Date banDate;

    public AccessData()
    {
        attempts = 1;
        banDate = new Date(0);
    }

    public AccessData(int attempts, Date banDate)
    {
        this.attempts = attempts;
        this.banDate = banDate;
    }

    /**
     * Resets the ban and login attempts on given access data; used on successful login
     */
    public void reset()
    {
        attempts = 0;
        banDate = new Date(0);
    }

    /**
     * Bans the user for a given amount of hours starting from now.
     */
    public void setBan(int hours)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.HOUR_OF_DAY, hours);
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
    }
}
