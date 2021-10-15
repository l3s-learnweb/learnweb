package de.l3s.learnweb.resource.archive;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public record ArchiveUrl(String archiveUrl, LocalDateTime timestamp) implements Serializable {
    @Serial
    private static final long serialVersionUID = 63994605834754451L;

    @Override
    public String toString() {
        return "[" + this.archiveUrl + ", " + this.timestamp + "]";
    }

}
