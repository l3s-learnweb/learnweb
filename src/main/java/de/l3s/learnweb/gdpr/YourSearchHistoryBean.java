package de.l3s.learnweb.gdpr;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.searchhistory.SearchHistoryDao;
import de.l3s.learnweb.searchhistory.SearchHistoryQuery;
import de.l3s.learnweb.user.User;

/**
 * YourSearchHistoryBean is responsible for displaying user queries supplied to search field.
 */
@Named
@ViewScoped
public class YourSearchHistoryBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 8515265854401597437L;

    private LazyDataModel<SearchHistoryQuery> userQueries;

    @Inject
    private SearchHistoryDao searchHistoryDao;

    @PostConstruct
    public void init() {
        User user = getUser();
        BeanAssert.authorized(user);

        userQueries = new UserQueriesModel(searchHistoryDao, user.getId());
    }

    public LazyDataModel<SearchHistoryQuery> getUserQueries() {
        return userQueries;
    }

    /**
     * Loads only the displayed page of the queries, the exporters load all of them.
     */
    private static class UserQueriesModel extends LazyDataModel<SearchHistoryQuery> {
        @Serial
        private static final long serialVersionUID = -3482365436203581196L;

        private final SearchHistoryDao searchHistoryDao;
        private final int userId;

        UserQueriesModel(SearchHistoryDao searchHistoryDao, int userId) {
            this.searchHistoryDao = searchHistoryDao;
            this.userId = userId;
        }

        @Override
        public int count(Map<String, FilterMeta> filterBy) {
            return searchHistoryDao.countQueriesByUserId(userId);
        }

        @Override
        public List<SearchHistoryQuery> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
            return searchHistoryDao.findQueriesByUserId(userId, pageSize, first);
        }
    }
}
