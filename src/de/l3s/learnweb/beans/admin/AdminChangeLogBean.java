package de.l3s.learnweb.beans.admin;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.ChangeLog;
import de.l3s.learnweb.user.Message;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;
import de.l3s.learnweb.web.RequestManager;
import de.l3s.util.StringHelper;
import de.l3s.util.email.Mail;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeSet;

@Named
@RequestScoped
public class AdminChangeLogBean extends ApplicationBean
{
    private static final Logger log = Logger.getLogger(AdminChangeLogBean.class);
    @NotEmpty
    private String text;
    @NotEmpty
    private String title;
    private User user;


    public AdminChangeLogBean()
    {
        user = getUser();
        if(user == null || !user.isAdmin())
            return;

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
