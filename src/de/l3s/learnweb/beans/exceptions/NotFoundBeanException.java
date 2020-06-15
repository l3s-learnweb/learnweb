package de.l3s.learnweb.beans.exceptions;

public class NotFoundBeanException extends BeanException {
    private static final long serialVersionUID = -7494995783143183959L;

    private static final int STATUS_CODE = BeanException.NOT_FOUND;

    public NotFoundBeanException() {
        super(STATUS_CODE);
    }

    public NotFoundBeanException(String message) {
        super(STATUS_CODE, message);
    }

    public NotFoundBeanException(Throwable cause) {
        super(STATUS_CODE, cause);
    }

    public NotFoundBeanException(String message, Throwable cause) {
        super(STATUS_CODE, message, cause);
    }
}
