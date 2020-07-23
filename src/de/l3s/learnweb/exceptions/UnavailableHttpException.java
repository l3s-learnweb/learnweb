package de.l3s.learnweb.exceptions;

public class UnavailableHttpException extends HttpException {

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
