package de.l3s.learnweb.resource.search;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;

@Named
@ViewScoped
public class SuggestQueryDialog implements Serializable {
    @Serial
    private static final long serialVersionUID = 7572402402655403989L;

    private SuggestedQuery query;
    private List<SuggestedQuery> queries = new ArrayList<>();

    @PostConstruct
    public void init() {
        this.queries = (List<SuggestedQuery>) FacesContext.getCurrentInstance().getExternalContext().getFlash().get("queries");
    }

    public void setQuery(final SuggestedQuery query) {
        this.query = query;
    }

    public SuggestedQuery getQuery() {
        return query;
    }

    public List<SuggestedQuery> getQueries() {
        return queries;
    }

    public void onQuerySelect(SelectEvent<SuggestedQuery> query) {
        PrimeFaces.current().dialog().closeDynamic(query.getObject());
    }
}
