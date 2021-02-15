package de.l3s.learnweb.resource.archive;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public class ArchiveUrl implements Serializable {
    private static final long serialVersionUID = 63994605834754451L;

    private final String archiveUrl;
    private final LocalDateTime timestamp;

    public ArchiveUrl(String archiveUrl, LocalDateTime timestamp) {
        this.archiveUrl = archiveUrl;
        this.timestamp = timestamp;
    }

    public String getArchiveUrl() {
        return archiveUrl;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ArchiveUrl that = (ArchiveUrl) o;
        return StringUtils.equalsIgnoreCase(archiveUrl, that.archiveUrl) && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(archiveUrl, timestamp);
    }

    @Override
    public String toString() {
        return "[" + this.archiveUrl + ", " + this.timestamp + "]";
    }

}
