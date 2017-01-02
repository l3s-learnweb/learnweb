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
    private ExceptionHandlerFactory parent;

    // this injection handles jsf
    public LearnwebExceptionHandlerFactory(ExceptionHandlerFactory parent)
    {
        super(parent);
        this.parent = parent;
    }

    @Override
    public ExceptionHandler getExceptionHandler()
    {

        ExceptionHandler handler = new LearnwebExceptionHandler(parent.getExceptionHandler());

        return handler;
    }

}
