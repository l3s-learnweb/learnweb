package de.l3s.learnweb.exceptions;

import java.io.Serial;

public class UnavailableHttpException extends HttpException {

    @Serial
    private static final long serialVersionUID = -7948413557549460098L;

    public UnavailableHttpException() {
        super(HttpException.UNAVAILABLE);
    }

    public UnavailableHttpException(final String reason) {
        super(HttpException.UNAVAILABLE, reason, null, true);
    }

    public UnavailableHttpException(final String reason, final Throwable cause) {
        super(HttpException.UNAVAILABLE, reason, cause, true);
    }
}
