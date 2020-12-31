package de.l3s.learnweb.component.exceptionhandler;

import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerFactory;

public class LearnwebExceptionHandlerFactory extends ExceptionHandlerFactory {
    public LearnwebExceptionHandlerFactory(ExceptionHandlerFactory wrapped) {
        super(wrapped);
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        return new LearnwebExceptionHandler(getWrapped().getExceptionHandler());
    }
}
