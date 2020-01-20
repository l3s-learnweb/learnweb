package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User;

@Named
@ApplicationScoped
public class LearnwebBean implements Serializable
{
    private static final long serialVersionUID = 1286475643761742147L;
    private static final Logger log = Logger.getLogger(LearnwebBean.class);

    private transient Learnweb learnweb;
    private boolean maintenanceMode = false;

    public LearnwebBean() throws ClassNotFoundException, SQLException
    {
        learnweb = Learnweb.createInstance(UtilBean.getServerUrl());
    }

    @PostConstruct
    public void init()
    {
        // initialize stuff which is not required by console tasks
        learnweb.initLearnwebServer();
    }

    public Learnweb getLearnweb()
    {
        if(null == learnweb)
        {
            log.error("LearnwebBean: learnweb is null -> redirect");
            UtilBean.redirect("/lw/error.jsf");
        }
        return learnweb;
    }

    @PreDestroy
    public void onDestroy()
    {
        learnweb.onDestroy();
    }

    /**
     * Returns the path to the users profile image or a default image if no available
     *
     * @param user
     * @return
     * @throws SQLException
     */
    public String getProfileImage(User user) throws SQLException
    {
        if(user != null)
        {
            String url = user.getImage();

            if(null != url)
                return url;
        }
        return learnweb.getServerUrl() + "/resources/images/no-profile-picture.jpg";
    }

    public boolean isMaintenanceMode()
    {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean maintenanceMode)
    {
        this.maintenanceMode = maintenanceMode;
    }

    public String getTrackerApiKey()
    {
        return this.learnweb.getProperties().getProperty("tracker.key");
    }

}
