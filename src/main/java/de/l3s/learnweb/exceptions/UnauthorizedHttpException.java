package de.l3s.learnweb.exceptions;

import java.io.Serial;

public class UnauthorizedHttpException extends HttpException {

    @Serial
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
