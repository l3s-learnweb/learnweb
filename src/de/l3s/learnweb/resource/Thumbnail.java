package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.util.Objects;

public class Thumbnail implements Serializable {
    private static final long serialVersionUID = -792701713759619246L;

    private final String url;
    private final int fileId;

    public Thumbnail(String url, int fileId) {
        this.url = url;
        this.fileId = fileId;
    }

    public Thumbnail(String url) {
        this.url = url;
        this.fileId = 0;
    }

    public Thumbnail(File file) {
        this.url = file.getUrl();
        this.fileId = file.getId();
    }

    public String getUrl() {
        return url;
    }

    /**
     * The file id is null if the thumbnail is not stored in learnweb.
     */
    public int getFileId() {
        return fileId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Thumbnail thumbnail = (Thumbnail) o;
        return fileId == thumbnail.fileId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId);
    }

    @Override
    public String toString() {
        return "Thumbnail [url=" + url + ", fileId=" + fileId + "]";
    }
}
