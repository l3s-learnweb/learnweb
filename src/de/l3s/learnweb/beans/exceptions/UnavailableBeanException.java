package de.l3s.learnweb.beans.exceptions;

public class UnavailableBeanException extends BeanException {
    private static final long serialVersionUID = -9089144711112002356L;

    private static final int STATUS_CODE = BeanException.UNAVAILABLE;

    public UnavailableBeanException() {
        super(STATUS_CODE);
    }

    public UnavailableBeanException(String message) {
        super(STATUS_CODE, message);
    }

    public UnavailableBeanException(Throwable cause) {
        super(STATUS_CODE, cause);
    }

    public UnavailableBeanException(String message, Throwable cause) {
        super(STATUS_CODE, message, cause);
    }
}
