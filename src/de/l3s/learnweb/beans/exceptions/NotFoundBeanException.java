package de.l3s.learnweb.beans.exceptions;

public class NotFoundBeanException extends BeanException {
    private static final long serialVersionUID = -7494995783143183959L;

    public NotFoundBeanException() {
        super();
    }

    public NotFoundBeanException(final String message) {
        super(message);
    }

    public NotFoundBeanException(final Throwable cause) {
        super(cause);
    }

    public NotFoundBeanException(final String message, final Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return BeanException.NOT_FOUND;
    }
}
