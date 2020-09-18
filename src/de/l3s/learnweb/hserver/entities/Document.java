
package de.l3s.learnweb.hserver.entities;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Document implements Serializable {
    private static final long serialVersionUID = 4740383181194846344L;

    private Integer id;
    private String title;
    private String webUri;
    private LocalDateTime updated;
    private LocalDateTime created;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getWebUri() {
        return webUri;
    }

    public void setWebUri(final String webUri) {
        this.webUri = webUri;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(final LocalDateTime updated) {
        this.updated = updated;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(final LocalDateTime created) {
        this.created = created;
    }
}
