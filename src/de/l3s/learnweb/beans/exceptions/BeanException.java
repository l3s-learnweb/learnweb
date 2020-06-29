package de.l3s.learnweb.beans.exceptions;

import javax.faces.FacesException;
import javax.servlet.http.HttpServletResponse;

public class BeanException extends FacesException {
    private static final long serialVersionUID = 8490079026477441484L;

    public static final int BAD_REQUEST = HttpServletResponse.SC_BAD_REQUEST;
    public static final int UNAUTHORIZED = HttpServletResponse.SC_UNAUTHORIZED;
    public static final int FORBIDDEN = HttpServletResponse.SC_FORBIDDEN;
    public static final int NOT_FOUND = HttpServletResponse.SC_NOT_FOUND;
    public static final int GONE = HttpServletResponse.SC_GONE;
    public static final int SESSION_EXPIRED = 419;
    public static final int SERVER_ERROR = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    public static final int UNAVAILABLE = HttpServletResponse.SC_SERVICE_UNAVAILABLE;

    public BeanException() {
        super();
    }

    public BeanException(final String message) {
        super(message);
    }

    public BeanException(final Throwable cause) {
        super(cause);
    }

    public BeanException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public int getStatusCode() {
        return SERVER_ERROR;
    }
}
