package de.l3s.util.email;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;

/**
 * Every scheduled amount of time gets all emails from the inbox that were sent after last scan.
 *
 * @author Kate
 *
 */
public class BounceFetcher implements Runnable
{

    @Override
    public void run()
    {
        try
        {
            Learnweb.getInstance().getBounceManager().parseInbox();
        }
        catch(Throwable e)
        {
            Logger.getLogger(BounceFetcher.class).error("err", e);
        }
    }

}
