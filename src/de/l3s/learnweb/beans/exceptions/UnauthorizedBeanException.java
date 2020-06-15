package de.l3s.learnweb.beans.exceptions;

public class UnauthorizedBeanException extends BeanException {
    private static final long serialVersionUID = -6101926199322120918L;

    private static final int STATUS_CODE = BeanException.UNAUTHORIZED;

    public UnauthorizedBeanException() {
        super(STATUS_CODE);
    }

    public UnauthorizedBeanException(String message) {
        super(STATUS_CODE, message);
    }

    public UnauthorizedBeanException(Throwable cause) {
        super(STATUS_CODE, cause);
    }

    public UnauthorizedBeanException(String message, Throwable cause) {
        super(STATUS_CODE, message, cause);
    }
}
