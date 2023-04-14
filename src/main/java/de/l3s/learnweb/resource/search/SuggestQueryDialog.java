package de.l3s.learnweb.resource.search;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
    private final List<SuggestedQuery> queries = new ArrayList<>();

    @PostConstruct
    public void init() {
        List<String> bing = (List<String>) FacesContext.getCurrentInstance().getExternalContext().getFlash().get("bing");
        List<String> edurec = (List<String>) FacesContext.getCurrentInstance().getExternalContext().getFlash().get("edurec");
        if (bing != null) {
            bing = bing.subList(0, Math.min(5, bing.size()));
            for (String query : bing) {
                queries.add(new SuggestedQuery("bing", query));
            }
        }
        if (edurec != null) {
            edurec = edurec.subList(0, Math.min(5, edurec.size()));
            for (String query : edurec) {
                queries.add(new SuggestedQuery("edurec", query));
            }
        }
        Collections.shuffle(queries);
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

    public record SuggestedQuery(int id, String source, String query) {
        public SuggestedQuery(String source, String query) {
            this(new Random().nextInt(), source, query);
        }
    }
}
