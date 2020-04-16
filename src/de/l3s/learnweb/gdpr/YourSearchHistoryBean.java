package de.l3s.learnweb.gdpr;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.User;
import de.l3s.searchHistoryTest.SearchHistoryManager;

/**
 * YourSearchHistoryBean is responsible for displaying user queries supplied to search field.
 */
@Named
@ViewScoped
public class YourSearchHistoryBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 8515265854401597437L;
    //private static final Logger log = LogManager.getLogger(YourSearchHistoryBean.class);

    private List<SearchHistoryManager.Query> userQueries;

    public YourSearchHistoryBean() throws SQLException
    {
        User user = getUser();
        if(null == user)
            // when not logged in
            return;

        List<SearchHistoryManager.Session> userSessions;
        this.userQueries = new LinkedList<>();
        userSessions = this.getLearnweb().getSearchHistoryManager().getSessionsForUser(user.getId());

        for(SearchHistoryManager.Session session : userSessions)
        {
            userQueries.addAll(session.getQueries());
        }
    }

    public List<SearchHistoryManager.Query> getUserQueries()
    {
        return userQueries;
    }
}
