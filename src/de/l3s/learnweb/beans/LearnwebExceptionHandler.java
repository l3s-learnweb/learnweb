package de.l3s.learnweb.beans;

import java.util.Iterator;

import javax.faces.FacesException;
import javax.faces.application.ViewExpiredException;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.primefaces.application.exceptionhandler.PrimeExceptionHandler;

/**
 * Used to log errors which are redirect to /lw/error.jsf
 * 
 * @author Kemkes
 *
 */
public class LearnwebExceptionHandler extends PrimeExceptionHandler
{
    private static final Logger log = Logger.getLogger(LearnwebExceptionHandler.class.getCanonicalName());

    public LearnwebExceptionHandler(ExceptionHandler exception)
    {
	super(exception);
    }

    @Override
    public void handle() throws FacesException
    {
	Iterator<ExceptionQueuedEvent> unhandledExceptionQueuedEvents = getUnhandledExceptionQueuedEvents().iterator();
	if(unhandledExceptionQueuedEvents.hasNext())
	{
	    Throwable exception = unhandledExceptionQueuedEvents.next().getContext().getException();
	    if(exception instanceof ViewExpiredException)
		log.info("view expired exception", exception);
	    else
	    {
		String queryString = null;
		String page = null;

		try
		{
		    FacesContext facesContext = FacesContext.getCurrentInstance();
		    ExternalContext ext = facesContext.getExternalContext();
		    HttpServletRequest servletRequest = (HttpServletRequest) ext.getRequest();
		    UIViewRoot viewRoot = facesContext.getViewRoot();
		    page = viewRoot.getViewId();
		    queryString = servletRequest.getQueryString();
		}
		catch(Throwable t)
		{
		    // ignore
		}
		log.fatal("Fatal unhandled error on page: " + page + "; QueryString: " + queryString, exception);
	    }

	}
	super.handle();
    }
}
