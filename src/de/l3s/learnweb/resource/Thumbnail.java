package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.util.Objects;

public class Thumbnail implements Serializable {
    private static final long serialVersionUID = -792701713759619246L;

    protected String url;

    protected Thumbnail() {
    }

    public Thumbnail(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
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
        return Objects.equals(url, thumbnail.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return "Thumbnail [url=" + url + "]";
    }
}
