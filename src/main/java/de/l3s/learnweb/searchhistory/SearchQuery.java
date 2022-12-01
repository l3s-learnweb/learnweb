package de.l3s.learnweb.searchhistory;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;


public record SearchQuery(int searchId, String query, String mode, LocalDateTime timestamp, String service) implements Serializable {
    @Serial
    private static final long serialVersionUID = 4391998336381044255L;
}
