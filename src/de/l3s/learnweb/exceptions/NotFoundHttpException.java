package de.l3s.learnweb.exceptions;

public class NotFoundHttpException extends HttpException {

    private static final long serialVersionUID = -935734518733420953L;

    public NotFoundHttpException() {
        super(HttpException.NOT_FOUND);
    }

    public NotFoundHttpException(final String reason) {
        super(HttpException.NOT_FOUND, reason, null, true);
    }

    public NotFoundHttpException(final String reason, final Throwable cause) {
        super(HttpException.NOT_FOUND, reason, cause, true);
    }
}
