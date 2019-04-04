package de.l3s.learnweb.beans.admin;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.ChangeLog;
import de.l3s.learnweb.user.News;
import de.l3s.learnweb.user.NewsManager;
import de.l3s.learnweb.user.User;
import org.apache.log4j.Logger;
import org.hibernate.validator.constraints.NotEmpty;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.inject.Named;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Named
@RequestScoped
public class AdminNewsBean extends ApplicationBean
{
    private static final Logger log = Logger.getLogger(AdminNewsBean.class);

    private List<News> newsList;
    @NotEmpty
    private String text;
    @NotEmpty
    private String title;
    private User user;



    private int newsId;
    private News news;





    public AdminNewsBean() throws SQLException
    {
        load();
    }

    private void load() throws SQLException
    {

        if(getUser().isAdmin())
        {
            try
            {
                newsList = new ArrayList<>(getLearnweb().getNewsManager().getNewsAll());
            }
            catch(Exception e)
            {
                log.error(e);
            }


                try
                {
                    newsId = Integer.parseInt(getFacesContext().getExternalContext().getRequestParameterMap().get("news_id"));

                }
                catch(Exception e)
                {
                    return;
                }

            news = getLearnweb().getNewsManager().getNewsByNewsId(newsId);
            log.debug(news.getText());
            if(null == news)
            {
                addGrowl(FacesMessage.SEVERITY_FATAL, "invalid news_id parameter");
                return;
            }
        }
        else
            return;
    }

    private  void loadNewsById() throws SQLException{
        if(getUser() == null) // not logged in
            return;


    }


    public void onCreateNews() throws  SQLException{
        try{
            News news_save = new News();
            news_save.setTitle(title);
            news_save.setText(text);
            news_save.setUser_id(getUser().getId());
            log.debug(news_save.getTitle()+" - "+news_save.getText()+" -byUser- "+news_save.getUser_id());
            getLearnweb().getNewsManager().save(news_save);
            addGrowl(FacesMessage.SEVERITY_INFO, "News was added !" );
        }catch(Exception e){
            addErrorMessage(e);
            addGrowl(FacesMessage.SEVERITY_ERROR, "Fatal error !");
        }

    }

    public void onDeleteNews(News news) throws SQLException{
        try{
            getLearnweb().getNewsManager().delete(news);
        }catch(Exception e){
            addErrorMessage(e);
        }
    }

    public void onUpdateNews(int newsId) throws  SQLException{
        try{
            News news_up = new News();
            news_up.setTitle(news.getTitle());
            news_up.setText(news.getText());
            news_up.setUser_id(getUser().getId());
            log.debug(news_up.getTitle()+" - "+news_up.getText()+" -byUser- "+news_up.getUser_id());
            getLearnweb().getNewsManager().update(news);
            addGrowl(FacesMessage.SEVERITY_INFO, "News was updated !" );
        }catch(Exception e){
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
        return newsList;
    }

    public News getNews()
    {
        return news;
    }

    public int getNewsId()
    {
        return newsId;
    }


}
