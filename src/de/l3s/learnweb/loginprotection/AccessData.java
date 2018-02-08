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
    private int attempts;
    private Date banDate;
    private String name;

    public AccessData(String name)
    {
        setAttempts(0);
        setBanDate(new Date(0));
        this.setName(name);
    }

    public AccessData(int attempts, Date banDate, String name)
    {
        this.setAttempts(attempts);
        this.setBanDate(banDate);
        this.setName(name);
    }

    /**
     * Resets the ban and login attempts on given access data; used on successful login
     */
    public void reset()
    {
        setAttempts(0);
        setBanDate(new Date(0));
    }

    /**
     * Bans the user for a given amount of hours starting from now.
     */
    public void setBan(int hours)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.HOUR_OF_DAY, hours);
        setBanDate(cal.getTime());
    }

    /**
     * Bans user for virtually forever (400 years)
     */
    public void permaban()
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.YEAR, 400);
        setBanDate(cal.getTime());
    }

    public int getAttempts()
    {
        return attempts;
    }

    public void setAttempts(int attempts)
    {
        this.attempts = attempts;
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
}
