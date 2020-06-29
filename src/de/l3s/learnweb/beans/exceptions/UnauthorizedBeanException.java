package de.l3s.learnweb.beans.exceptions;

public class UnauthorizedBeanException extends BeanException {
    private static final long serialVersionUID = -6101926199322120918L;

    public UnauthorizedBeanException() {
        super();
    }

    public UnauthorizedBeanException(final String message) {
        super(message);
    }

    public UnauthorizedBeanException(final Throwable cause) {
        super(cause);
    }

    public UnauthorizedBeanException(final String message, final Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return BeanException.UNAUTHORIZED;
    }
}
