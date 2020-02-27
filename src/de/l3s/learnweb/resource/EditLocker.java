package de.l3s.learnweb.resource;

import java.util.Date;

import de.l3s.learnweb.user.User;

public class EditLocker
{
    private static final int LOCK_SESSION_EXPIRES_IN_MS = 2 * 60 * 1000;

    private final User user;
    private final Date editStarted;
    private Date lastActivity;

    public EditLocker(final User user)
    {
        this.user = user;
        this.lastActivity = new Date();
        this.editStarted = new Date();
    }

    public User getUser()
    {
        return user;
    }

    public Date getEditStarted()
    {
        return editStarted;
    }

    public Date getLastActivity()
    {
        return lastActivity;
    }

    public void setLastActivity(final Date lastActivity)
    {
        this.lastActivity = lastActivity;
    }

    public boolean isSessionExpired()
    {
        return (new Date().getTime() - lastActivity.getTime()) > LOCK_SESSION_EXPIRES_IN_MS;
    }
}
