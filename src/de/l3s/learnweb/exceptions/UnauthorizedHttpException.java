package de.l3s.learnweb.exceptions;

public class UnauthorizedHttpException extends HttpException {

    private static final long serialVersionUID = 4327049922463494760L;

    public UnauthorizedHttpException() {
        super(HttpException.UNAUTHORIZED);
    }

    public UnauthorizedHttpException(final String reason) {
        super(HttpException.UNAUTHORIZED, reason, null, true);
    }

    public UnauthorizedHttpException(final String reason, final Throwable cause) {
        super(HttpException.UNAUTHORIZED, reason, cause, true);
    }
}
