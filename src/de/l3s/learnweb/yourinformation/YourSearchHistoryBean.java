package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.searchHistoryTest.SearchHistoryManager;
import org.apache.log4j.Logger;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * YourSearchHistoryBean is responsible for displaying user queries supplied to search field.
 */
@Named
@ViewScoped
public class YourSearchHistoryBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 8515265854401597437L;
    private static final Logger log = Logger.getLogger(YourSearchHistoryBean.class);

    private List<SearchHistoryManager.Query> userQueries;

    public YourSearchHistoryBean()
    {
        List<SearchHistoryManager.Session> userSessions;
        this.userQueries = new LinkedList<>();
        try
        {
            userSessions = this.getLearnweb().getSearchHistoryManager().getSessionsForUser(this.getUser().getId());

            for (SearchHistoryManager.Session session : userSessions){
                userQueries.addAll(session.getQueries());
            }
        }
        catch(SQLException e)
        {
            log.error("Can't properly retrieve user sessions", e);
        }
    }

    public List<SearchHistoryManager.Query> getUserQueries()
    {
        return userQueries;
    }
}
