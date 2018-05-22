package de.l3s.learnweb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.ExternalContext;
import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User;

@ManagedBean
@ApplicationScoped
public class LearnwebBean implements Serializable
{
    private static final long serialVersionUID = 1286475643761742147L;
    private static final Logger log = Logger.getLogger(LearnwebBean.class);

    private transient Learnweb learnweb;
    private final String contextPath;
    private boolean maintenanceMode = false;

    public LearnwebBean() throws IOException, ClassNotFoundException, SQLException
    {
        ServletContext servletContext = (ServletContext) UtilBean.getExternalContext().getContext();

        contextPath = servletContext.getContextPath();

        log.debug("init LearnwebBean: context='" + contextPath + "'");

        learnweb = Learnweb.createInstance(getServerUrl());
    }

    @PostConstruct
    public void init()
    {
        // initialize stuff which is not required by console tasks
        learnweb.initLearnwebServer();
    }

    /**
     *
     * @return Returns the contextpath
     */
    public String getContextPath()
    {
        return contextPath;
    }

    /**
     *
     * @return example http://learnweb.l3s.uni-hannover.de or http://localhost:8080/Learnweb-Tomcat
     */
    private static String getServerUrl()
    {
        try
        {
            ExternalContext ext = UtilBean.getExternalContext();

            if(ext.getRequestServerPort() == 80 || ext.getRequestServerPort() == 443)
                return ext.getRequestScheme() + "://" + ext.getRequestServerName() + ext.getRequestContextPath();
            else
                return ext.getRequestScheme() + "://" + ext.getRequestServerName() + ":" + ext.getRequestServerPort() + ext.getRequestContextPath();
        }
        catch(Exception e)
        {
            log.warn("Can't get server url. This is only expected in console mode");
            return "http://learnweb.l3s.uni-hannover.de";
        }
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
     * returns the path to the users profile image or a default image if no available
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
        return getContextPath() + "/resources/image/no_profile.jpg";
    }

    public boolean isMaintenanceMode()
    {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean maintenanceMode)
    {
        this.maintenanceMode = maintenanceMode;
    }

}
