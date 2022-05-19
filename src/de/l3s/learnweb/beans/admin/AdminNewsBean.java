package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.inject.Named;
import javax.validation.constraints.NotEmpty;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.News;

//TODO Matvey: beans must implement Serializable
@Named
@RequestScoped
public class AdminNewsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -5638619427036990427L;
    private static final Logger log = Logger.getLogger(AdminNewsBean.class);

    private List<News> newsList; // TODO this is never used
    private List<News> newsListAll; // rename to news
    @NotEmpty
    private String text;
    @NotEmpty
    private String title;
    private int newsId;
    private News news;

    public AdminNewsBean() throws SQLException
    {
        load();
    }

    private void load() throws SQLException
    {

        try
        {
            newsList = new ArrayList<>(getLearnweb().getNewsManager().getNewsAll());
            newsListAll = new ArrayList<>(getLearnweb().getNewsManager().getNewsAll());
        }
        catch(Exception e)
        {
            log.error(e);
        }
        try
        {
            //TODO Matvey look at the admin organisation page for an example how to retrieve a get parameter
            newsId = Integer.parseInt(getFacesContext().getExternalContext().getRequestParameterMap().get("news_id"));
        }
        catch(Exception e)
        {
            return;
        }

        news = getLearnweb().getNewsManager().getNewsById(newsId);
        log.debug(news.toString());
        if(news == null)
        {
            addGrowl(FacesMessage.SEVERITY_FATAL, "invalid news_id parameter");
            return;
        }

    }

    public void onCreateNews() throws SQLException
    {
        if(isCanSend())
        {
            try
            {
                News news = new News();
                news.setTitle(title);
                news.setText(text);
                news.setUser_id(getUser().getId());
                log.debug(news);
                getLearnweb().getNewsManager().save(news);
                addGrowl(FacesMessage.SEVERITY_INFO, "News was added !");
                load();
            }
            catch(Exception e)
            {
                addErrorMessage(e);
                //TODO Matvey addErrorMessage will already send a message. no need to addGrowl.
                addGrowl(FacesMessage.SEVERITY_ERROR, "Fatal error !");
            }
        }
    }

    public void onDeleteNews(News news) throws SQLException
    {
        try
        {
            getLearnweb().getNewsManager().delete(news);
            addGrowl(FacesMessage.SEVERITY_INFO, "News was deleted !");
            load();
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public void onUpdateNews(int newsId) throws SQLException
    {
        try
        {
            News news = new News();
            news.setTitle(this.news.getTitle());
            news.setText(this.news.getText());
            news.setId(newsId);
            log.debug(news);
            getLearnweb().getNewsManager().update(news);
            addGrowl(FacesMessage.SEVERITY_INFO, "News was updated !");
        }
        catch(Exception e)
        {
            addErrorMessage(e);
            addGrowl(FacesMessage.SEVERITY_ERROR, "Fatal error !");
        }

    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public List<News> getNewsList()
    {
        if(newsList.size() > 3)
        {
            newsList.remove(3); // TODO Matvey: I think this method does not do what you expect. It any way makes no sense. On the admin page you should show all news entrys.
        }
        return newsList;
    }

    public List<News> getAllNewsList()
    {

        return newsList;
    }

    public List<News> getNewsListAll()
    {

        return newsListAll;
    }

    public News getNews()
    {
        return news;
    }

    public int getNewsId()
    {
        return newsId;
    }

    private boolean isCanSend() // TODO this is unnecessary. These checks are done through JSF validators. The @NotEmpty annotation should already solve this task. Otherwise you can use the "required" attribute of the input form
    {
        if(getTitle() == null)
        {
            addGrowl(FacesMessage.SEVERITY_ERROR, "Please, add the title !");
            return false;
        }
        if(getText() == null)
        {
            addGrowl(FacesMessage.SEVERITY_ERROR, "Please, fill the text area !");
            return false;
        }
        return true;
    }

}
