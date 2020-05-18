package de.l3s.learnweb.resource.search.filters;

import java.io.Serializable;
import java.util.Objects;

public class FilterOption implements Serializable, Comparable<FilterOption> {
    private static final long serialVersionUID = -5553821306095298015L;

    private final String title;
    private final String value;
    private final Long totalResults;
    private boolean active;

    public FilterOption(final String title, final String value, final Long totalResults, final boolean active) {
        this.title = title;
        this.value = value;
        this.totalResults = totalResults;
        this.active = active;
    }

    public String getTitle() {
        return title;
    }

    public String getValue() {
        return value;
    }

    public Long getTotalResults() {
        return totalResults;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    @Override
    public int compareTo(FilterOption another) {
        return Long.compare(another.totalResults, totalResults);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final FilterOption that = (FilterOption) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
