package de.l3s.learnweb.loginprotection.entity;

import java.util.Date;

/**
 * Holds login attempts by selected ip and username. Used by AnalysingProtectionManager for... analysing... yeah.
 *
 * @author Kate
 *
 */
public class LoginAttemptData
{
    private String IP;
    private String username;
    private boolean success;
    private Date timestamp;

    public LoginAttemptData(String iP, String username, boolean success, Date timestamp)
    {
        IP = iP;
        this.username = username;
        this.success = success;
        this.timestamp = timestamp;
    }

    public String getIP()
    {
        return IP;
    }

    public String getUsername()
    {
        return username;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setIP(String iP)
    {
        IP = iP;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public void setSuccess(boolean success)
    {
        this.success = success;
    }

    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }

}
