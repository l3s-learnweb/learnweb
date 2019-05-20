package de.l3s.learnweb.gdpr.exceptions;

public class UnknownResourceTypeException extends Exception
{
    private static final long serialVersionUID = 4122804131888834811L;

    public UnknownResourceTypeException(String errorMessage) {
        super(errorMessage);
    }
}
