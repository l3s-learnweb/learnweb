package de.l3s.learnweb.searchhistory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.app.ConfigProvider;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.search.SearchMode;
import de.l3s.learnweb.searchhistory.SearchHistoryDao.SearchAction;
import de.l3s.learnweb.user.User;

/**
 * Records the web, image and video searches (not those within a group) in the search history:
 * the query with its user and session, each page of results when it is loaded the first time, and the clicked or saved results.
 * Controlled by {@link ConfigProvider#isCollectSearchHistory()}, not by the organisation's privacy settings.
 *
 * <p>Only the query is stored synchronously, because the following calls refer to its id.
 * The rest is stored in the background, in the order it was recorded. Failures are logged and never break the search.
 */
@ApplicationScoped
public class SearchHistoryRecorder {
    private static final Logger log = LogManager.getLogger(SearchHistoryRecorder.class);

    @Inject
    private SearchHistoryDao searchHistoryDao;

    @Inject
    private ConfigProvider config;

    private ExecutorService executor;

    @PostConstruct
    void init() {
        executor = Executors.newSingleThreadExecutor(Thread.ofVirtual().name("search-history").factory());
    }

    /**
     * Stores the pending results and actions before shutting down.
     */
    @PreDestroy
    void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("The search history wasn't stored completely before shutdown");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * @return the id of the stored query, 0 if it wasn't stored; the results and actions of a search with id 0 are ignored
     */
    public int recordQuery(String query, SearchMode mode, ResourceService service, String language, String filters, User user, String sessionId) {
        if (!config.isCollectSearchHistory()) {
            return 0;
        }

        try {
            return searchHistoryDao.insertQuery(query, mode, service, language, filters, user, sessionId);
        } catch (RuntimeException e) {
            log.error("Error storing search query: {}", query, e);
            return 0;
        }
    }

    /**
     * @param results a page of results that was loaded for the first time, their details are copied right away
     */
    public void recordResults(int searchId, List<ResourceDecorator> results) {
        if (searchId == 0 || results.isEmpty()) {
            return;
        }

        List<SearchHistoryResult> snapshot = results.stream().map(SearchHistoryResult::of).toList();
        executor.execute(() -> {
            try {
                searchHistoryDao.insertResources(searchId, snapshot);
            } catch (RuntimeException e) {
                log.error("Error storing the results of search {}", searchId, e);
            }
        });
    }

    /**
     * @param rank the rank of the clicked or saved result
     */
    public void recordAction(int searchId, int rank, SearchAction action) {
        if (searchId == 0) {
            return;
        }

        executor.execute(() -> {
            try {
                searchHistoryDao.insertAction(searchId, rank, action);
            } catch (RuntimeException e) {
                log.error("Error storing {} of search {}, rank {}", action, searchId, rank, e);
            }
        });
    }
}
