package org.apache.tomcat.util.http;

import java.io.Serial;

/**
 * Test stub of Tomcat's {@code InvalidParameterException}, which is not available on the test classpath.
 * It has to use the same package and name, because {@code LearnwebExceptionHandler} matches the exception by class name.
 * Remove this stub if {@code tomcat-embed-core} or {@code tomcat-coyote} is ever added to the test classpath,
 * otherwise the class that wins depends on the classpath order.
 */
public class InvalidParameterException extends IllegalStateException {
    @Serial
    private static final long serialVersionUID = -2766316569306939227L;

    public InvalidParameterException(String message) {
        super(message);
    }
}
