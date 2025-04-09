package de.l3s.learnweb.searchhistory;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.search.SearchMode;

public record SearchHistoryQuery(int searchId, String query, SearchMode mode, ResourceService service, LocalDateTime createdAt) implements Serializable {
    @Serial
    private static final long serialVersionUID = 4391998336381044255L;
}
