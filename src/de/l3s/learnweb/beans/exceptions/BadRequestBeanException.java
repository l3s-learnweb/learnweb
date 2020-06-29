package de.l3s.learnweb.beans.exceptions;

public class BadRequestBeanException extends BeanException {
    private static final long serialVersionUID = 6608015712616634541L;

    public BadRequestBeanException() {
        super();
    }

    public BadRequestBeanException(final String message) {
        super(message);
    }

    public BadRequestBeanException(final Throwable cause) {
        super(cause);
    }

    public BadRequestBeanException(final String message, final Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return BeanException.BAD_REQUEST;
    }
}
