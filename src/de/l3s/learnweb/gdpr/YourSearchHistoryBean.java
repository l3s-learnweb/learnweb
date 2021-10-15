package de.l3s.learnweb.gdpr;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.searchhistory.SearchQuery;
import de.l3s.learnweb.searchhistory.SearchSession;
import de.l3s.learnweb.user.User;

/**
 * YourSearchHistoryBean is responsible for displaying user queries supplied to search field.
 */
@Named
@ViewScoped
public class YourSearchHistoryBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 8515265854401597437L;
    //private static final Logger log = LogManager.getLogger(YourSearchHistoryBean.class);

    private List<SearchQuery> userQueries;

    @PostConstruct
    public void init() {
        User user = getUser();
        BeanAssert.authorized(user);

        userQueries = new LinkedList<>();
        List<SearchSession> userSessions = dao().getSearchHistoryDao().findSessionsByUserId(user.getId());

        for (SearchSession session : userSessions) {
            userQueries.addAll(session.getQueries());
        }
    }

    public List<SearchQuery> getUserQueries() {
        return userQueries;
    }
}
