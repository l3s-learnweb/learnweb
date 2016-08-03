package de.l3s.learnweb.beans;

import java.util.Iterator;

import javax.faces.FacesException;
import javax.faces.context.ExceptionHandler;
import javax.faces.event.ExceptionQueuedEvent;

import org.apache.log4j.Logger;
import org.primefaces.application.exceptionhandler.PrimeExceptionHandler;

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
	    log.fatal("fatal unhandled error", exception);
	}
	super.handle();
    }
    /*
    @Override
    public void handle() throws FacesException
    {
    final Iterator<ExceptionQueuedEvent> i = getUnhandledExceptionQueuedEvents().iterator();
    while(i.hasNext())
    {
        ExceptionQueuedEvent event = i.next();
        ExceptionQueuedEventContext context = (ExceptionQueuedEventContext) event.getSource();
    
        // get the exception from context
        Throwable t = context.getException();
    
        log.fatal("fatal unhandled error", t);
    
    }
    //parent hanle
    getWrapped().handle();
    }*/

}
