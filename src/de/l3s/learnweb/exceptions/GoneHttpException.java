package de.l3s.learnweb.exceptions;

public class GoneHttpException extends HttpException {

    private static final long serialVersionUID = 7531919021172513707L;

    public GoneHttpException() {
        super(HttpException.GONE);
    }

    public GoneHttpException(final String reason) {
        super(HttpException.GONE, reason, null, true);
    }

    public GoneHttpException(final String reason, final Throwable cause) {
        super(HttpException.GONE, reason, cause, true);
    }
}
