package de.l3s.learnweb.beans.exceptions;

public class BadRequestBeanException extends BeanException {
    private static final long serialVersionUID = 6608015712616634541L;

    private static final int STATUS_CODE = BeanException.BAD_REQUEST;

    public BadRequestBeanException() {
        super(STATUS_CODE);
    }

    public BadRequestBeanException(String message) {
        super(STATUS_CODE, message);
    }

    public BadRequestBeanException(Throwable cause) {
        super(STATUS_CODE, cause);
    }

    public BadRequestBeanException(String message, Throwable cause) {
        super(STATUS_CODE, message, cause);
    }
}
