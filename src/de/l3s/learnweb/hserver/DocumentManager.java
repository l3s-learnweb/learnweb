package de.l3s.learnweb.hserver;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.hserver.entities.Document;

public class DocumentManager {
    private static final Logger log = LogManager.getLogger(DocumentManager.class);

    private final Learnweb learnweb;

    public DocumentManager(Learnweb learnweb) {
        this.learnweb = learnweb;
    }

    public Document get(Integer id) {
        try (Handle handle = learnweb.openJdbiHandle()) {
            Optional<Document> documents = handle.select("SELECT * FROM learnweb_annotations.document WHERE id = ?", id)
                .mapToBean(Document.class)
                .findFirst();

            return documents.orElse(null);
        }
    }

    public Document getOrCreate(Document unsavedDocument) {
        if (unsavedDocument.getId() != null) {
            return unsavedDocument;
        }

        try (Handle handle = learnweb.openJdbiHandle()) {
            Optional<Document> document = handle.select("SELECT * FROM learnweb_annotations.document WHERE web_uri = ?", unsavedDocument.getWebUri())
                .mapToBean(Document.class)
                .findFirst();

            return document.orElseGet(() -> create(unsavedDocument));
        }
    }

    public Document create(Document document) {
        try (Handle handle = learnweb.openJdbiHandle()) {
            Integer documentId = handle.createUpdate("INSERT INTO learnweb_annotations.document (title, web_uri) VALUES(:title, :webUri)")
                .bindBean(document)
                .executeAndReturnGeneratedKeys("id")
                .mapTo(Integer.class)
                .first();

            document.setId(documentId);
            return document;
        }
    }
}
