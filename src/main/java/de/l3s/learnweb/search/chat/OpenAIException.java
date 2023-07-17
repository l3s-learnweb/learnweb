package de.l3s.learnweb.search.chat;

public class OpenAIException extends Exception {
    public OpenAIException(final String message) {
        super(message);
    }

    public OpenAIException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
