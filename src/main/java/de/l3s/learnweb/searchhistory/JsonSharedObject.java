package de.l3s.learnweb.searchhistory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonSharedObject {
    public static class User {
        private int id;
        private String name;

        public User(final int id, final String name) {
            this.name = name;
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public static class Entity {
        private String uri;
        private String query;
        private double weight;
        private transient String type;
        private transient int id;

        public Entity(final String uri, final String query, final double weight, final String type, final int id) {
            this.uri = uri;
            this.query = query;
            this.weight = weight;
            this.type = type;
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(final String uri) {
            this.uri = uri;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(final String query) {
            this.query = query;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(final double weight) {
            this.weight = weight;
        }

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }
    }

    public static class Link {
        private int source;
        private int target;

        public Link(final int source, final int target) {
            this.source = source;
            this.target = target;
        }

        public int getSource() {
            return source;
        }

        public void setSource(final int source) {
            this.source = source;
        }

        public int getTarget() {
            return target;
        }

        public void setTarget(final int target) {
            this.target = target;
        }
    }

    private List<Entity> entities;
    private List<Link> links;
    private User user;
    private String application;
    private int id;

    public JsonSharedObject(final String content, boolean isJson) {
        if (isJson) {
            Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new SearchHistoryBean.LocalDateTimeAdapter().nullSafe())
                .create();
            JsonSharedObject object = gson.fromJson(content, JsonSharedObject.class);

            this.entities = object.getEntities();
            this.links = object.getLinks();
            this.user = object.user;
            this.application = object.getApplication();
        } else {
            this.entities = new ArrayList<>();
            this.links = new ArrayList<>();
            this.application = content;
        }
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public void setEntities(final List<Entity> entities) {
        this.entities = entities;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(final List<Link> links) {
        this.links = links;
    }

    public User getUser() {
        return user;
    }

    public void setUser(final User user) {
        this.user = user;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(final String application) {
        this.application = application;
    }
}
