package de.l3s.learnweb.beans.exceptions;

public class ForbiddenBeanException extends BeanException {
    private static final long serialVersionUID = 7017092619988321819L;

    public ForbiddenBeanException() {
        super();
    }

    public ForbiddenBeanException(final String message) {
        super(message);
    }

    public ForbiddenBeanException(final Throwable cause) {
        super(cause);
    }

    public ForbiddenBeanException(final String message, final Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return BeanException.FORBIDDEN;
    }
}
