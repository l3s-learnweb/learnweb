package de.l3s.learnweb.beans;

import java.util.Iterator;

import javax.faces.FacesException;
import javax.faces.application.ViewExpiredException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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
		log.info("View expired exception");
	    else if(exception instanceof IllegalStateException && exception.getMessage().startsWith("Cannot create a session"))
	    {
		log.info(exception.getMessage() + "; Happens mostly because of error 404");
		return;
	    }
	    else
	    {
		String url = null;
		Integer userId = -1;
		String referrer = null;
		String ip = null;
		String userAgent = null;
		try
		{
		    FacesContext facesContext = FacesContext.getCurrentInstance();
		    ExternalContext ext = facesContext.getExternalContext();
		    HttpServletRequest servletRequest = (HttpServletRequest) ext.getRequest();
		    referrer = servletRequest.getHeader("referer");
		    ip = servletRequest.getHeader("X-FORWARDED-FOR");
		    if(ip == null)
		    {
			ip = servletRequest.getRemoteAddr();
		    }

		    userAgent = servletRequest.getHeader("User-Agent");
		    url = servletRequest.getRequestURL().toString();
		    if(servletRequest.getQueryString() != null)
			url += '?' + servletRequest.getQueryString();

		    HttpSession session = servletRequest.getSession(false);
		    if(session != null)
			userId = (Integer) session.getAttribute("learnweb_user_id");
		}
		catch(Throwable t)
		{
		    // ignore
		}
		log.fatal("Fatal unhandled error on: " + url + "; userId: " + userId + "; ip: " + ip + "; referrer: " + referrer + "; userAgent: " + userAgent, exception);
	    }

	}
	super.handle();
    }
}
