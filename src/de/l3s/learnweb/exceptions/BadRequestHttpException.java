package de.l3s.learnweb.exceptions;

public class BadRequestHttpException extends HttpException {

    public BadRequestHttpException() {
        super(HttpException.BAD_REQUEST);
    }

    public BadRequestHttpException(final String reason) {
        super(HttpException.BAD_REQUEST, reason);
    }

    public BadRequestHttpException(final String reason, final boolean silent) {
        super(HttpException.BAD_REQUEST, reason, null, silent);
    }

    public BadRequestHttpException(final String reason, final Throwable cause) {
        super(HttpException.BAD_REQUEST, reason, cause);
    }
}
