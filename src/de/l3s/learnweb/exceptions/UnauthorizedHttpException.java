package de.l3s.learnweb.exceptions;

public class UnauthorizedHttpException extends HttpException {

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
