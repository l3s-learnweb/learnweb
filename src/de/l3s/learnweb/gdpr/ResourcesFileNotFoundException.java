package de.l3s.learnweb.gdpr;

public class ResourcesFileNotFoundException extends Exception
{
    private static final long serialVersionUID = -7942183019101772318L;

    public ResourcesFileNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}
