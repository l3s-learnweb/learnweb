package de.l3s.learnweb.loginprotection;

import de.l3s.learnweb.Learnweb;

/**
 * Cleans up expired bans (>3 days old) from the bans database and forces ProtectionManager and RequestManager to reload their banlists
 *
 * @author Kate
 *
 */
public class ExpiredBansCleaner implements Runnable
{

    @Override
    public void run()
    {
        Learnweb.getInstance().getProtectionManager().cleanUpOutdatedBans();
    }

}
