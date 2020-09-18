package de.l3s.learnweb.hserver.adapters;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.bind.adapter.JsonbAdapter;
import javax.json.stream.JsonParser;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.hserver.HUtils;
import de.l3s.learnweb.hserver.entities.Annotation;
import de.l3s.learnweb.hserver.entities.Document;
import de.l3s.util.StringHelper;

public class AnnotationAdapter implements JsonbAdapter<Annotation, JsonObject> {

    @Override
    public JsonObject adaptToJson(Annotation annotation) throws Exception {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (annotation.getId() != null) {
            builder.add("id", annotation.getId());
        }
        if (annotation.getTargetUri() != null) {
            builder.add("uri", annotation.getTargetUri());
        }
        if (annotation.getText() != null) {
            builder.add("text", annotation.getText());
        }
        if (annotation.getDeleted() != null) {
            builder.add("hidden", annotation.getDeleted());
        }
        if (annotation.getUserId() != null) {
            builder.add("user", HUtils.getUserForUserId(annotation.getUserId()));
            if (annotation.getUser() != null) {
                builder.add("user_info", Json.createObjectBuilder().add("display_name", annotation.getUser().getUsername()).build());
            }
        }
        if (annotation.getGroupId() != null) {
            builder.add("group", annotation.getGroupId().toString());
        }

        if (annotation.getTargetSelectors() != null) {
            JsonParser parser = Json.createParser(new StringReader(annotation.getTargetSelectors()));
            parser.next();
            builder.add("target", parser.getArray());
        }
        if (annotation.getReferences() != null) {
            JsonParser parser = Json.createParser(new StringReader(annotation.getReferences()));
            parser.next();
            builder.add("references", parser.getArray());
        }
        if (annotation.getTags() != null) {
            JsonParser parser = Json.createParser(new StringReader(annotation.getTags()));
            parser.next();
            builder.add("tags", parser.getArray());
        } else {
            builder.add("tags", JsonValue.EMPTY_JSON_ARRAY);
        }
        if (annotation.getUpdated() != null) {
            builder.add("updated", annotation.getUpdated().format(DateTimeFormatter.ISO_DATE_TIME));
        }
        if (annotation.getCreated() != null) {
            builder.add("created", annotation.getCreated().format(DateTimeFormatter.ISO_DATE_TIME));
        }
        if (annotation.getDocumentId() != null) {
            builder.add("document", Json.createObjectBuilder()
                .add("title", annotation.getDocument().getTitle())
                .build());
        }

        builder.add("permissions", Json.createObjectBuilder()
            .add("read", Json.createArrayBuilder().add(annotation.getShared() ? "group:" + annotation.getGroupId() : HUtils.getUserForUserId(annotation.getUserId())).build())
            .add("admin", Json.createArrayBuilder().add(HUtils.getUserForUserId(annotation.getUserId())).build())
            .add("update", Json.createArrayBuilder().add(HUtils.getUserForUserId(annotation.getUserId())).build())
            .add("delete", Json.createArrayBuilder().add(HUtils.getUserForUserId(annotation.getUserId())).build())
            .build());

        builder.add("flagged", false);
        return builder.build();
    }

    @Override
    public Annotation adaptFromJson(JsonObject jsonObject) throws Exception {
        Annotation annotation = new Annotation();
        if (jsonObject.containsKey("group")) {
            annotation.setGroupId(StringHelper.parseInt(jsonObject.getString("group"), 0));
        }
        if (jsonObject.containsKey("permissions")) {
            JsonObject permissions = jsonObject.getJsonObject("permissions");
            String read = permissions.getJsonArray("read").getString(0);
            // String update = permissions.getJsonArray("update").getString(0);
            // String delete = permissions.getJsonArray("delete").getString(0);

            annotation.setShared(read.contains("group"));
        }
        if (jsonObject.containsKey("tags")) {
            annotation.setTags(jsonObject.get("tags").toString());
        }
        if (jsonObject.containsKey("text")) {
            annotation.setText(jsonObject.getString("text"));
        }
        if (jsonObject.containsKey("user")) {
            annotation.setUserId(HUtils.getUserIdFromUser(jsonObject.getString("user")));
        }
        // if (jsonObject.containsKey("user_info")) {
        // currently ignore
        // }
        if (jsonObject.containsKey("hidden")) {
            annotation.setDeleted(jsonObject.getBoolean("hidden")); // not sure about this
        }
        // if (jsonObject.containsKey("links")) {
        // currently ignore
        // }
        if (jsonObject.containsKey("document")) {
            JsonObject docObject = jsonObject.getJsonObject("document");
            Document document = new Document();
            if (docObject.containsKey("title")) {
                document.setTitle(docObject.getString("title"));
            }
            if (docObject.containsKey("link")) {
                for (JsonValue jsonValue : docObject.getJsonArray("link")) {
                    JsonObject linkObject = jsonValue.asJsonObject();
                    if (linkObject.containsKey("href") && StringUtils.isNotBlank(linkObject.getString("href"))) {
                        document.setWebUri(HUtils.normalizeUri(linkObject.getString("href")));
                        break;
                    }
                }
            }
            annotation.setUnsavedDocument(document);
        }
        if (jsonObject.containsKey("references")) {
            annotation.setReferences(jsonObject.get("references").toString());
        }
        if (jsonObject.containsKey("uri")) {
            annotation.setTargetUri(jsonObject.getString("uri"));
            annotation.setTargetUriNormalized(HUtils.normalizeUri(jsonObject.getString("uri")));
        }
        if (jsonObject.containsKey("target")) {
            annotation.setTargetSelectors(jsonObject.get("target").toString());
        }
        if (jsonObject.containsKey("updated")) {
            annotation.setUpdated(LocalDateTime.parse(jsonObject.getString("updated"), DateTimeFormatter.ISO_DATE_TIME));
        }
        if (jsonObject.containsKey("created")) {
            annotation.setCreated(LocalDateTime.parse(jsonObject.getString("created"), DateTimeFormatter.ISO_DATE_TIME));
        }
        return annotation;
    }
}
