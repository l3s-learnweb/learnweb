package de.l3s.learnweb.beans.exceptions;

public class GoneBeanException extends BeanException {
    private static final long serialVersionUID = -7494995783143183959L;

    public GoneBeanException() {
        super();
    }

    public GoneBeanException(final String message) {
        super(message);
    }

    public GoneBeanException(final Throwable cause) {
        super(cause);
    }

    public GoneBeanException(final String message, final Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return BeanException.GONE;
    }
}
