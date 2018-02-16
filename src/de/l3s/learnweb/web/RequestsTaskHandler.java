package de.l3s.learnweb.web;

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
        RequestManager.instance().cleanOldRequests();
        RequestManager.instance().recordRequestsToDB();

    }

}
