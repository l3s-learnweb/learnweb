package de.l3s.searchHistoryTest;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.searchHistoryTest.SearchHistoryManager.Session;

@ManagedBean
@ViewScoped
public class NewSearchHistoryBean extends ApplicationBean implements Serializable
{
    private final static Logger log = Logger.getLogger(NewSearchHistoryBean.class);
    private static final long serialVersionUID = -7682314831788865416L;

    private List<String> queries;
    private List<String> entities;
    private List<Session> sessions;
    private String title;
    private int userId;
    private String selectedSessionId;
    private DateFormat dateFormatter;
    private DateFormat timeFormatter;

    /**
     * Load the variables that needs values before the view is rendered
     */
    public void preRenderView()
    {
        if(isAjaxRequest())
            return;

        loadData();
        if(getUser() != null)
            userId = getUser().getId();
    }

    public NewSearchHistoryBean()
    {
        log.info("Initializing Search History Bean");
        title = "Search History";
        queries = new ArrayList<String>();
        entities = new ArrayList<String>();
        //sessions = new Linked<String>();
    }

    /**
     * You can load the queries and entities
     * from the database here
     */
    public void loadData()
    {

    }

    public List<String> getQueries()
    {
        queries = getLearnweb().getSearchHistoryManager().getQueriesForSessionId("");
        return queries;
    }

    public List<String> getEntities()
    {
        return entities;
    }

    public String getTitle()
    {
        return title;
    }

    public List<Session> getSessions()
    {
        if(sessions == null)
        {
            try
            {
                sessions = getLearnweb().getSearchHistoryManager().getSessionsForUser(userId);
            }
            catch(SQLException e)
            {
                log.error("Error while fetching list of sessions for particular user: " + userId, e);
            }
        }

        return sessions;
    }

    public void actionUpdateKGData()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String sessionId = params.get("session-id");
        selectedSessionId = sessionId;
        System.out.println(sessionId);
    }

    public String formatDate(Date date, Locale locale)
    {
        if(dateFormatter == null)
            dateFormatter = DateFormat.getDateInstance(DateFormat.DEFAULT, locale);
        return dateFormatter.format(date);
    }

    public String formatTime(Date date, Locale locale)
    {
        if(timeFormatter == null)
            timeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT, locale);
        return timeFormatter.format(date);
    }
}
