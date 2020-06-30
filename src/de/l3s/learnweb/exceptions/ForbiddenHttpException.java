package de.l3s.learnweb.exceptions;

public class ForbiddenHttpException extends HttpException {

    public ForbiddenHttpException() {
        super(HttpException.FORBIDDEN);
    }

    public ForbiddenHttpException(final String reason) {
        super(HttpException.FORBIDDEN, reason);
    }

    public ForbiddenHttpException(final String reason, final Throwable cause) {
        super(HttpException.FORBIDDEN, reason, cause);
    }
}
