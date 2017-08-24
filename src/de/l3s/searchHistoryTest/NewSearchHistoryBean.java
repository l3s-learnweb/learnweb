package de.l3s.searchHistoryTest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;

@ManagedBean
@ViewScoped
public class NewSearchHistoryBean extends ApplicationBean implements Serializable
{
    private final static Logger log = Logger.getLogger(NewSearchHistoryBean.class);
    private static final long serialVersionUID = -7682314831788865416L;

    private List<String> queries;
    private List<String> entities;
    private String title;

    /**
     * Load the variables that needs values before the view is rendered
     */
    public void preRenderView()
    {
        if(isAjaxRequest())
            return;

        loadData();
    }

    public NewSearchHistoryBean()
    {
        log.info("Initializing Search History Bean");
        title = "Search History";
        queries = new ArrayList<String>();
        entities = new ArrayList<String>();
    }

    /**
     * You can include the parsing of the dataset to set the
     * queries and entities here
     */
    public void loadData()
    {

    }

    public List<String> getQueries()
    {
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
}
