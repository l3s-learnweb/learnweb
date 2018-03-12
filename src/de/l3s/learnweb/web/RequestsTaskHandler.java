package de.l3s.learnweb.web;

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
        RequestManager requestManager = Learnweb.getInstance().getRequestManager();
        requestManager.cleanOldRequests();
        requestManager.recordRequestsToDB();

    }

}
