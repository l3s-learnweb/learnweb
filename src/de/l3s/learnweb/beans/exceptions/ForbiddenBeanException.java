package de.l3s.learnweb.beans.exceptions;

public class ForbiddenBeanException extends BeanException {
    private static final long serialVersionUID = 7017092619988321819L;

    private static final int STATUS_CODE = BeanException.FORBIDDEN;

    public ForbiddenBeanException() {
        super(STATUS_CODE);
    }

    public ForbiddenBeanException(String message) {
        super(STATUS_CODE, message);
    }

    public ForbiddenBeanException(Throwable cause) {
        super(STATUS_CODE, cause);
    }

    public ForbiddenBeanException(String message, Throwable cause) {
        super(STATUS_CODE, message, cause);
    }
}
