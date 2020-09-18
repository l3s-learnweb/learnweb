package de.l3s.learnweb.hserver.models;

import java.util.List;

import de.l3s.learnweb.hserver.entities.Annotation;

public class SearchResults {
    private int total;
    private List<Annotation> rows;

    public SearchResults(final List<Annotation> rows) {
        this.total = rows.size();
        this.rows = rows;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(final int total) {
        this.total = total;
    }

    public List<Annotation> getRows() {
        return rows;
    }

    public void setRows(final List<Annotation> rows) {
        this.rows = rows;
    }
}
