package de.l3s.searchlogclient;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.sun.jersey.api.client.ClientHandlerException;

import de.l3s.learnweb.Learnweb;

public class MyHttpSessionListener implements HttpSessionListener
{

    @Override
    public void sessionCreated(HttpSessionEvent se)
    {
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se)
    {
	SearchLogClient searchLogClient = Learnweb.getInstance().getSearchlogClient();
	try
	{
	    searchLogClient.pushBatchResultsetList();
	    searchLogClient.postResourceLog();
	    searchLogClient.passUpdateResultset();
	    searchLogClient.pushTagList();
	}
	catch (ClientHandlerException e)
	{
	    System.out.println("Search Tracker service is down");
	}
	catch (RuntimeException e)
	{
	    System.out.println(e.getMessage());
	}
	HttpSession session = se.getSession();
	System.out.println(" (session) Destroyed:ID=" + session.getId());
    }
}
