package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;

@Named
@ViewScoped
public class YourInformationBean extends ApplicationBean implements Serializable
{
    private static final Logger log = Logger.getLogger(YourInformationBean.class);
    private static final long serialVersionUID = -2460055775611784132L;

    public YourInformationBean() throws SQLException
    {
        User user = getUser();
        if(user == null)
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "not logged in");
            return;
        }

    }

    @Override
    public User getUser()
    {
        return super.getUser();
    }

}
