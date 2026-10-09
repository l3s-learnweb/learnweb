package de.l3s.collabrec;

import java.io.IOException;
import java.io.Serial;

public class GraphException extends IOException {
    @Serial
    private static final long serialVersionUID = -3540735373479528183L;

    public GraphException(String message) {
        super(message);
    }

    public GraphException(String message, Throwable cause) {
        super(message, cause);
    }
}
