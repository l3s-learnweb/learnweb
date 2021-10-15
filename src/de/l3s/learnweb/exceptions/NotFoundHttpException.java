package de.l3s.learnweb.exceptions;

import java.io.Serial;

public class NotFoundHttpException extends HttpException {

    @Serial
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
