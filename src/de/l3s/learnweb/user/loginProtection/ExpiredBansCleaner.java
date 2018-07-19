package de.l3s.learnweb.user.loginProtection;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;

/**
 * Cleans up expired bans (>3 days old) from the bans database and forces ProtectionManager and RequestManager to reload their ban lists
 *
 * @author Kate
 *
 */
public class ExpiredBansCleaner implements Runnable
{

    @Override
    public void run()
    {
        try
        {
            Learnweb.getInstance().getProtectionManager().cleanUpOutdatedBans();
        }
        catch(Throwable e)
        {
            Logger.getLogger(ExpiredBansCleaner.class).error("err", e);
        }
    }

}
