package de.l3s.learnweb.exceptions;

import javax.annotation.Nullable;

public class HttpException extends RuntimeException {
    private static final long serialVersionUID = 8490079026477441484L;

    /**
     * Indicating the request sent by the client was syntactically incorrect.
     */
    public static final int BAD_REQUEST = 400;
    /**
     * Indicating that the request requires authentication.
     */
    public static final int UNAUTHORIZED = 401;
    /**
     * Indicating the server understood the request but refused to fulfill it.
     * This may be due to the user not having the necessary permissions for a resource or
     * attempting a prohibited action (e.g. creating a duplicate record where only one is allowed).
     */
    public static final int FORBIDDEN = 403;
    /**
     * Indicating the requested resource is not available.
     */
    public static final int NOT_FOUND = 404;
    /**
     * Indicating that the resource is no longer available at the server and no forwarding address is known.
     * This condition <em>SHOULD</em> be considered permanent.
     */
    public static final int GONE = 410;
    /**
     * Indicating the user session is expired.
     */
    public static final int SESSION_EXPIRED = 419;
    /**
     * Indicating an error inside the server which prevented it from fulfilling the request.
     */
    public static final int INTERNAL_SERVER_ERROR = 500;
    /**
     * Indicating the the server is temporarily unable to handle the request.
     */
    public static final int UNAVAILABLE = 503;

    private final int status;
    private final String reason;

    public HttpException(final int status) {
        this(status, null, null);
    }

    public HttpException(final int status, final String reason) {
        this(status, reason, null);
    }

    public HttpException(final int status, final String reason, final Throwable cause) {
        super(null, cause);
        this.status = status;
        this.reason = reason;
    }

    public HttpException(final String reason) {
        this(INTERNAL_SERVER_ERROR, reason, null);
    }

    public HttpException(final String reason, final Throwable cause) {
        super(null, cause);
        this.status = INTERNAL_SERVER_ERROR;
        this.reason = reason;
    }

    public int getStatus() {
        return status;
    }

    @Nullable
    public String getReason() {
        return reason;
    }

    @Override
    public String getMessage() {
        if (getCause() == null) {
            return reason;
        }

        StringBuilder sb = new StringBuilder(64);
        if (reason != null) {
            sb.append(reason).append("; ");
        }
        sb.append("nested exception is ").append(getCause());
        return sb.toString();
    }

    /**
     * Retrieve the innermost cause of the given exception, if any.
     *
     * @return the innermost exception, or {@code null} if none
     */
    @Nullable
    public Throwable getRootCause() {
        Throwable rootCause = null;
        Throwable cause = getCause();
        while (cause != null && cause != rootCause) {
            rootCause = cause;
            cause = cause.getCause();
        }
        return rootCause;
    }

    /**
     * Retrieve the most specific cause of the given exception, that is, either the innermost cause (root cause) or the exception itself.
     * Differs from {@link #getRootCause} in that it falls back to the original exception if there is no root cause.
     *
     * @return the most specific cause (never {@code null})
     */
    public Throwable getMostSpecificCause() {
        Throwable rootCause = getRootCause();
        return (rootCause != null ? rootCause : this);
    }
}
