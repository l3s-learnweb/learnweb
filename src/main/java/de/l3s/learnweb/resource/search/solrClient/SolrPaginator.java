package de.l3s.learnweb.resource.search.solrClient;

import java.io.IOException;
import java.io.Serial;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.resource.AbstractPaginator;
import de.l3s.learnweb.resource.ResourceDecorator;

public class SolrPaginator extends AbstractPaginator {
    @Serial
    private static final long serialVersionUID = 3823389610985272265L;
    private static final Logger log = LogManager.getLogger(SolrPaginator.class);

    private final SolrSearch search;
    private int searchLogId;

    private List<FacetField> facetFieldsResults;
    private Map<String, Integer> facetQueriesResults;

    public SolrPaginator(SolrSearch search, final boolean collectSearchHistory) {
        super(search.getResultsPerPage());

        this.search = search;

        if (collectSearchHistory && search.getFilterGroupIds().size() == 1 && search.getQuery() != null && !"*".equals(search.getQuery())) {
            this.searchLogId = Learnweb.dao().getSearchHistoryDao().insertGroupQuery(
                search.getFilterGroupIds().get(0),
                search.getQuery(),
                null,
                search.getFilterLanguage(),
                search.getUserId()
            );
        }
    }

    @Override
    public synchronized List<ResourceDecorator> getCurrentPage() throws IOException {
        if (getCurrentPageCache() != null) {
            return getCurrentPageCache();
        }

        List<ResourceDecorator> results = search.getResourcesByPage(getPageIndex() + 1);
        setTotalResults((int) search.getResultsFound());
        facetFieldsResults = search.getResultsFacetFields();
        facetQueriesResults = search.getResultsFacetQuery();

        setCurrentPageCache(results);
        try {
            if (searchLogId != 0) {
                Learnweb.dao().getSearchHistoryDao().insertResources(searchLogId, results);
            }
        } catch (Exception e) {
            log.error("Failed to save search results", e);
        }
        return results;
    }

    public List<FacetField> getFacetFields() throws IOException {
        if (facetFieldsResults == null) {
            getCurrentPage();
        }

        return facetFieldsResults;
    }

    public Map<String, Integer> getFacetQueries() throws IOException {
        if (facetQueriesResults == null) {
            getCurrentPage();
        }

        return facetQueriesResults;
    }
}
