package de.l3s.learnweb.web;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;

/**
 * Cleans up requests from RequestManager that are older than specified number of days
 *
 * @author Kate
 *
 */
public class RequestsTaskHandler implements Runnable
{
    @Override
    public void run()
    {
        try
        {
            RequestManager requestManager = Learnweb.getInstance().getRequestManager();
            requestManager.cleanOldRequests();
            requestManager.recordRequestsToDB();
        }
        catch(Throwable e)
        {
            Logger.getLogger(RequestsTaskHandler.class).error("error", e);
        }
    }

}
