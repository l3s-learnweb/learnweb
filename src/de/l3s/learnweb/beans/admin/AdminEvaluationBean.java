package de.l3s.learnweb.beans.admin;

import java.sql.SQLException;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.inject.Named;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.User;

/**
 * 
 * example
 * 
 * do what every you want with this class or delete it if you don't need it
 * 
 * @author Kemkes
 * 
 */
@Named
@RequestScoped
public class AdminEvaluationBean extends ApplicationBean
{
    private final static Logger log = Logger.getLogger(AdminEvaluationBean.class);

    private List<User> users;

    public AdminEvaluationBean() throws SQLException
    {
        User user = getUser();

        if(null == user) // not logged in
        {
            log.debug("User is not logged in");
        }
        else
        {
            users = getLearnweb().getUserManager().getUsersByOrganisationId(user.getOrganisationId());

            if(user.isAdmin() || user.isModerator())
                log.info("User has special rights");
        }

    }

    public List<User> getUsers()
    {
        return users;
    }

    public void onClick()
    {
        addMessage(FacesMessage.SEVERITY_INFO, "a message");
    }
}
