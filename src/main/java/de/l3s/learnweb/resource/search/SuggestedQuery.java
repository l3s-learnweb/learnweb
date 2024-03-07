package de.l3s.learnweb.resource.search;

import java.io.Serializable;

public record SuggestedQuery(int id, int index, String source, String query) implements Serializable {
    public SuggestedQuery(int id, String source, String query) {
        this(id, 0, source, query);
    }
}
