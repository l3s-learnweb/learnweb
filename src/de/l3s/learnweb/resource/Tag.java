package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import de.l3s.learnweb.Learnweb;

public class Tag implements Comparable<Tag>, Serializable {
    private static final long serialVersionUID = 7542445827379987188L;

    private int id;
    private final String name;

    public Tag(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Resource> getResources() throws SQLException {
        return Learnweb.getInstance().getResourceManager().getResourcesByTagId(id);
    }

    @Override
    public int compareTo(Tag tag) {
        return this.getName().compareTo(tag.getName());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Tag tag = (Tag) o;
        return id == tag.id && Objects.equals(name, tag.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
