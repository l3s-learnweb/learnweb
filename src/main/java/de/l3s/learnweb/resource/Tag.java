package de.l3s.learnweb.resource;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.util.HasId;

public class Tag implements Comparable<Tag>, Serializable, HasId {
    @Serial
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

    public List<Resource> getResources() {
        return Learnweb.dao().getResourceDao().findByTagId(id);
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
