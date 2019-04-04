package de.l3s.learnweb.gdpr.exceptions;

public class UnknownResourceTypeException extends Exception
{
    public UnknownResourceTypeException(String errorMessage) {
        super(errorMessage);
    }
}
