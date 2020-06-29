package de.l3s.learnweb.beans.exceptions;

public class UnavailableBeanException extends BeanException {
    private static final long serialVersionUID = -9089144711112002356L;

    public UnavailableBeanException() {
        super();
    }

    public UnavailableBeanException(final String message) {
        super(message);
    }

    public UnavailableBeanException(final Throwable cause) {
        super(cause);
    }

    public UnavailableBeanException(final String message, final Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return BeanException.UNAVAILABLE;
    }
}
