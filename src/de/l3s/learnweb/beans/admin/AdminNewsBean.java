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





    public AdminNewsBean() throws SQLException
    {
        load();
    }

    private void load() throws SQLException
    {
        if(getUser().isAdmin())
            try{
                newsList = new ArrayList<>(getLearnweb().getNewsManager().getNewsAll());
            }catch(Exception e){
                log.error(e);
            }
        else
            return;
    }

    public void onCreateNews() throws  SQLException{
        try{
            News news = new News();
            news.setTitle(title);
            news.setText(text);
            news.setUser_id(getUser().getId());
            log.debug(news.getTitle()+" - "+news.getText()+" -byUser- "+news.getUser_id());
            getLearnweb().getNewsManager().save(news);
        }catch(Exception e){
            addErrorMessage(e);
        }

    }

    public void onDeleteNews(News news) throws SQLException{
        try{
            getLearnweb().getNewsManager().delete(news);
        }catch(Exception e){
            addErrorMessage(e);
        }
    }

    public void CreateChangeLog() throws SQLException{
        log.debug("AdminChangeLog starts");
        if(isCanSend()){
            log.debug("start to send");

            try{
                ChangeLog changeLog = new ChangeLog();
                changeLog.setTitle(getTitle());
                changeLog.setText(getText());
                changeLog.setId(getUser().getId());
                changeLog.save();
                log.debug("changelog was saved into db");
            }catch(Exception e){
                log.error(e);
                addErrorMessage(e);
            }

            log.debug("AdminChangeLog finished");

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

    public boolean isCanSend(){
        if(getTitle().isEmpty()){
            addMessage(FacesMessage.SEVERITY_ERROR, "Please type a title.");
            log.debug("title is Empty");
            return false;
        }
        if(getText().isEmpty()){
            addMessage(FacesMessage.SEVERITY_ERROR, "Please type a message.");
            log.debug("text is Empty");
            return false;
        }
        log.debug("title and text are fitted");
        return true;
    }

}
