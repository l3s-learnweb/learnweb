package org.apache.tomcat.util.http;

import java.io.Serial;

/**
 * Test stub of Tomcat's {@code InvalidParameterException}, which is not available on the test classpath.
 */
public class InvalidParameterException extends IllegalStateException {
    @Serial
    private static final long serialVersionUID = -2766316569306939227L;

    private final int errorCode;

    public InvalidParameterException(String message) {
        this(message, 400);
    }

    public InvalidParameterException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
