package de.l3s.learnweb.beans;

import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;

import org.primefaces.application.exceptionhandler.PrimeExceptionHandlerFactory;

/**
 * Used to log errors which are redirect to /lw/error.jsf
 * 
 * @author Kemkes
 *
 */
public class LearnwebExceptionHandlerFactory extends PrimeExceptionHandlerFactory
{
    // this injection handles jsf
    public LearnwebExceptionHandlerFactory(ExceptionHandlerFactory parent)
    {
        super(parent);
    }

    @Override
    public ExceptionHandler getExceptionHandler()
    {
        return new LearnwebExceptionHandler(getWrapped().getExceptionHandler());
    }
}
