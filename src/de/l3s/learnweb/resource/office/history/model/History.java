package de.l3s.learnweb.resource.office.history.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.l3s.util.HasId;

public class History implements HasId {
    // only used to store and retrieve data from database
    private int id;
    private Integer resourceId;
    private Integer fileId;
    private Integer prevFileId;
    private Integer changesFileId;

    // values received from OnlyOffice and should be send back by request
    private JsonArray changes;
    private String created;
    private String key;
    private Integer version;
    private JsonObject user;
    private String serverVersion;

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public Integer getResourceId() {
        return resourceId;
    }

    public void setResourceId(final Integer resourceId) {
        this.resourceId = resourceId;
    }

    public Integer getFileId() {
        return fileId;
    }

    public void setFileId(final Integer fileId) {
        this.fileId = fileId;
    }

    public Integer getPrevFileId() {
        return prevFileId;
    }

    public void setPrevFileId(final Integer prevFileId) {
        this.prevFileId = prevFileId;
    }

    public Integer getChangesFileId() {
        return changesFileId;
    }

    public void setChangesFileId(final Integer changesFileId) {
        this.changesFileId = changesFileId;
    }

    public JsonArray getChanges() {
        return changes;
    }

    public void setChanges(final JsonArray changes) {
        this.changes = changes;
    }

    public void setChanges(final String changes) {
        this.changes = JsonParser.parseString(changes).getAsJsonArray();
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(final String created) {
        this.created = created;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public JsonObject getUser() {
        return user;
    }

    public void setUser(final JsonObject user) {
        this.user = user;
    }

    public void setUser(final int userId) {
        this.user = new JsonObject();
        this.user.addProperty("id", String.valueOf(userId));
    }

    public void setUser(final String userId, final String name) {
        this.user = new JsonObject();
        this.user.addProperty("id", userId);
        this.user.addProperty("name", name);
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(final String serverVersion) {
        this.serverVersion = serverVersion;
    }
}
