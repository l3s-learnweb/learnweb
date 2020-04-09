package de.l3s.learnweb.beans;

import javax.faces.context.ExceptionHandler;

import org.apache.log4j.Logger;
import org.primefaces.application.exceptionhandler.PrimeExceptionHandler;

import de.l3s.util.bean.BeanHelper;

/**
 * Used to log errors which are redirect to /lw/error.jsf
 *
 * @author Kemkes
 */
public class LearnwebExceptionHandler extends PrimeExceptionHandler
{
    private static final Logger log = Logger.getLogger(LearnwebExceptionHandler.class.getCanonicalName());

    public LearnwebExceptionHandler(ExceptionHandler exception)
    {
        super(exception);
    }

    @Override
    protected void logException(Throwable rootCause) {
        String description = BeanHelper.getRequestSummary();

        if(rootCause instanceof IllegalStateException && rootCause.getMessage().startsWith("Cannot create a session"))
        {
            log.warn(rootCause.getMessage() + "; Happens mostly because of error 404; On " + description);
        }
        else if(rootCause instanceof IllegalArgumentException && rootCause.getMessage().startsWith("Illegal base64 character -54"))
        {
            log.warn(rootCause.getMessage() + "; This happens often due to ; On " + description);
        }
        else
        {
            log.fatal("Fatal unhandled error on " + description, rootCause);
        }
    }
}
