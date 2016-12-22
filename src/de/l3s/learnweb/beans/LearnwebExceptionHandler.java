package de.l3s.learnweb.beans;

import java.util.Iterator;

import javax.el.ELException;
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

            while((exception instanceof FacesException || exception instanceof ELException) && exception.getCause() != null)
            {
                exception = exception.getCause();
            }

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
                    HttpServletRequest request = (HttpServletRequest) ext.getRequest();
                    referrer = request.getHeader("referer");
                    ip = request.getHeader("X-FORWARDED-FOR");
                    if(ip == null)
                    {
                        ip = request.getRemoteAddr();
                    }

                    userAgent = request.getHeader("User-Agent");
                    url = request.getRequestURL().toString();
                    if(request.getQueryString() != null)
                        url += '?' + request.getQueryString();

                    HttpSession session = request.getSession(false);
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

    /**
     * 
     * @return some attributes of the current http request like url, referrer, ip etc.
     */
    public static String getRequestSummary()
    {
        return getRequestSummary(null);
    }

    /**
     * 
     * @param request
     * @return some attributes of a request like url, referrer, ip etc.
     */
    public static String getRequestSummary(HttpServletRequest request)
    {
        String url = null;
        String referrer = null;
        String ip = null;
        String userAgent = null;
        Integer userId = null;

        try
        {
            if(request == null)
            {
                ExternalContext ext = FacesContext.getCurrentInstance().getExternalContext();
                request = (HttpServletRequest) ext.getRequest();
            }

            referrer = request.getHeader("referer");
            ip = request.getHeader("X-FORWARDED-FOR");
            if(ip == null)
            {
                ip = request.getRemoteAddr();
            }

            userAgent = request.getHeader("User-Agent");
            url = request.getRequestURL().toString();
            if(request.getQueryString() != null)
                url += '?' + request.getQueryString();

            HttpSession session = request.getSession(false);
            if(session != null)
                userId = (Integer) session.getAttribute("learnweb_user_id");
        }
        catch(Throwable t)
        {
            // ignore
        }
        return "page: " + url + "; userId: " + userId + "; ip: " + ip + "; referrer: " + referrer + "; userAgent: " + userAgent;
    }
}
