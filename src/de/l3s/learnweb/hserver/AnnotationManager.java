package de.l3s.learnweb.hserver;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.hserver.entities.Annotation;
import de.l3s.learnweb.user.User;

public class AnnotationManager {
    private static final Logger log = LogManager.getLogger(AnnotationManager.class);

    private static final DateTimeFormatter MYSQL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final Learnweb learnweb;

    public AnnotationManager(Learnweb learnweb) {
        this.learnweb = learnweb;
    }

    public Annotation get(int annotationId) {
        try (Handle handle = learnweb.openJdbiHandle()) {
            Optional<Annotation> annotation = handle.select("SELECT * FROM learnweb_annotations.annotation WHERE id = ?", annotationId)
                .mapToBean(Annotation.class)
                .findFirst();

            return annotation.orElse(null);
        }
    }

    public List<Annotation> search(final Integer limit, final Integer offset, final String sort, final String order, final String searchAfter,
        String uri, final String uriParts, final Integer userId, final Integer groupId, final List<String> tags,
        final String any, final String quote, final String references, final String text) {

        try (Handle handle = learnweb.openJdbiHandle()) {
            StringJoiner filters = new StringJoiner(" AND ");
            if (StringUtils.isNotBlank(searchAfter)) {
                String formattedTime = ZonedDateTime.parse(searchAfter, DateTimeFormatter.ISO_DATE_TIME).format(MYSQL_DATE_FORMAT);
                filters.add("created > " + formattedTime);
            }
            if (StringUtils.isNotBlank(uri)) {
                filters.add("target_uri_normalized = '" + HUtils.normalizeUri(uri) + "'");
            }
            if (StringUtils.isNotBlank(uriParts)) {
                filters.add("target_uri_normalized LIKE '%" + uriParts + "%'");
            }
            if (userId != null) {
                filters.add("user_id = " + userId);
            }
            if (groupId != null) {
                filters.add("group_id = " + groupId);
            }
            if (tags != null && !tags.isEmpty()) {
                for (String tag : tags) {
                    filters.add("tags LIKE '%" + tag + "%'");
                }
            }
            if (StringUtils.isNotBlank(quote)) {
                filters.add("target_selectors LIKE '%{\"type\":\"TextQuoteSelector\",\"exact\":\"%" + quote + "%\"}%'");
            }
            if (StringUtils.isNotBlank(text)) {
                filters.add("text LIKE '%" + text + "%'");
            }
            if (StringUtils.isNotBlank(references)) {
                filters.add("references LIKE '%" + references + "%'");
            }
            if (StringUtils.isNotBlank(any)) {
                filters.add("target_uri_normalized LIKE '%" + any + "%' OR tags LIKE '%" + any + "%' OR text LIKE '%" + any + "%' OR " +
                    "target_selectors LIKE '%{\"type\":\"TextQuoteSelector\",\"exact\":\"%" + any + "%\"}%'");
            }

            return handle.createQuery("SELECT * FROM learnweb_annotations.annotation WHERE deleted = 0 "
                + (filters.length() > 0 ? " AND " + filters : "") + " ORDER BY :sort :order LIMIT :limit OFFSET :offset")
                .bind("limit", limit)
                .bind("offset", offset)
                .bind("sort", sort)
                .bind("order", order)
                .mapToBean(Annotation.class)
                .list();
        }
    }

    public Annotation save(Annotation annotation) {
        if (annotation.getId() != null) {
            return update(annotation);
        }

        try (Handle handle = learnweb.openJdbiHandle()) {
            Integer annotationId = handle.createUpdate("INSERT INTO learnweb_annotations.annotation (`user_id`, `group_id`, `document_id`, `text`, `text_rendered`, `tags`, "
                + "`shared`, `target_uri`, `target_uri_normalized`, `target_selectors`, `references`, `extra`, `deleted`, `updated`, `created`) "
                + "VALUES(:userId, :groupId, :documentId, :text, :textRendered, :tags, "
                + ":shared, :targetUri, :targetUriNormalized, :targetSelectors, :references, :extra, :deleted, :updated, :created)")
                .bindBean(annotation)
                .executeAndReturnGeneratedKeys("id")
                .mapTo(Integer.class)
                .first();

            annotation.setId(annotationId);
            return annotation;
        }
    }

    public Annotation update(Annotation annotation) {
        try (Handle handle = learnweb.openJdbiHandle()) {
            // handle.createUpdate("INSERT INTO learnweb_annotations.annotation (type, name, bandate, bannedon, attempts, reason) "
            //     + "VALUES(:type, :name, :bannedUntil, :bannedOn, :attempts, :reason) ON DUPLICATE KEY UPDATE bandate = VALUES(bandate)")
            //     .bindBean(annotation)
            //     .execute();
            return null;
        }
    }

    public int delete(int annotationId) {
        try (Handle handle = learnweb.openJdbiHandle()) {
            return handle.execute("DELETE FROM learnweb_annotations.annotation WHERE id = ?", annotationId);
        }
    }

    public int flag(int annotationId, User user) {
        try (Handle handle = learnweb.openJdbiHandle()) {
            return handle.execute("INSERT INTO learnweb_annotations.flag (annotation_id, user_id) VALUES(?, ?)", annotationId, user.getId());
        }
    }

    public int hide(int annotationId) {
        try (Handle handle = learnweb.openJdbiHandle()) {
            return handle.execute("INSERT INTO learnweb_annotations.annotation_moderation (annotation_id) VALUES(?)", annotationId);
        }
    }

    public int show(int annotationId) {
        try (Handle handle = learnweb.openJdbiHandle()) {
            return handle.execute("DELETE FROM learnweb_annotations.annotation_moderation WHERE annotation_id = ?", annotationId);
        }
    }
}
