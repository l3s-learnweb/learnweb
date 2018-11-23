package de.l3s.learnweb.beans;

import java.util.Iterator;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.application.ViewExpiredException;
import javax.faces.context.ExceptionHandler;
import javax.faces.event.ExceptionQueuedEvent;

import org.apache.log4j.Logger;
import org.primefaces.application.exceptionhandler.PrimeExceptionHandler;

import de.l3s.util.BeanHelper;

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

            String description = BeanHelper.getRequestSummary();

            if(exception instanceof ViewExpiredException)
                log.info("View expired exception");
            else if(exception instanceof IllegalStateException && exception.getMessage().startsWith("Cannot create a session"))
            {
                log.warn(exception.getMessage() + "; Happens mostly because of error 404; On " + description);

                return;
            }
            else if(exception instanceof IllegalArgumentException && exception.getMessage().startsWith("Illegal base64 character -54"))
            {
                log.warn(exception.getMessage() + "; This happens often due to ; On " + description);

                return;
            }
            else
            {
                log.fatal("Fatal unhandled error on " + description, exception);

                log.error(exception.getStackTrace());
            }

        }
        super.handle();
    }
}
