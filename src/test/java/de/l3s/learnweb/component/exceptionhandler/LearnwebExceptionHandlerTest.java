package de.l3s.learnweb.component.exceptionhandler;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.tomcat.util.http.InvalidParameterException;
import org.junit.jupiter.api.Test;

class LearnwebExceptionHandlerTest {

    @Test
    void isMalformedRequest() {
        IllegalArgumentException viewStateError = new IllegalArgumentException("Illegal base64 character 7c");
        viewStateError.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("java.util.Base64$Decoder", "decode", "Base64.java", 570),
            new StackTraceElement("org.apache.myfaces.application.viewstate.StateUtils", "decode", "StateUtils.java", 398),
        });
        assertTrue(LearnwebExceptionHandler.isMalformedRequest(viewStateError));

        assertFalse(LearnwebExceptionHandler.isMalformedRequest(new IllegalArgumentException("Illegal base64 character 7c")));
        assertFalse(LearnwebExceptionHandler.isMalformedRequest(new IllegalStateException()));
    }

    @Test
    void isMalformedRequestTomcat() {
        assertTrue(LearnwebExceptionHandler.isMalformedRequest(new InvalidParameterException("Character decoding failed")));
        assertTrue(LearnwebExceptionHandler.isMalformedRequest(new InvalidParameterException("More than the maximum number of request parameters")));
    }
}
