package de.l3s.learnweb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;

@ManagedBean
@ApplicationScoped
public class LearnwebBean implements Serializable
{
    private static final long serialVersionUID = 1286475643761742147L;
    private static final Logger log = Logger.getLogger(LearnwebBean.class);

    private transient Learnweb learnweb;
    private final String contextUrl;

    public LearnwebBean() throws IOException, ClassNotFoundException, SQLException
    {
        ExternalContext ext = FacesContext.getCurrentInstance().getExternalContext();

        if(ext.getRequestServerPort() == 80 || ext.getRequestServerPort() == 443)
            contextUrl = ext.getRequestScheme() + "://" + ext.getRequestServerName() + ext.getRequestContextPath();
        else
            contextUrl = ext.getRequestScheme() + "://" + ext.getRequestServerName() + ":" + ext.getRequestServerPort() + ext.getRequestContextPath();

        learnweb = Learnweb.getInstanceRaw();
        learnweb.setContextUrl(contextUrl);

        log.debug("created LearnwebBean: contextUrl=" + contextUrl);
    }

    @PostConstruct
    public void init()
    {
        // initialize stuff which is not required by console tasks
        learnweb.initLearnwebServer();
    }

    /**
     * 
     * @return Returns the servername + contextpath. For the default installation this is: http://learnweb.l3s.uni-hannover.de
     */
    public String getContextUrl()
    {
        return contextUrl; // because we don't use httpS we can cache the url, change it if you want to use httpS too
    }

    /**
     * 
     * @return example for a local installation: http://localhost:8080/jlw/lw/
     */
    public String getBaseUrl()
    {
        ExternalContext ext = FacesContext.getCurrentInstance().getExternalContext();

        String path = ext.getRequestServletPath();
        path = path.substring(0, path.indexOf("/", 1) + 1);

        return contextUrl + path;
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
        return getContextUrl() + "/resources/image/no_profile.jpg";
    }

    /**
     * get a message from the message property files depending on the currently used local
     * 
     * @param msgKey
     * @param args
     * @return
     */
    public String getLocaleMessage(String msgKey, Object... args)
    {
        return UtilBean.getLocaleMessage(msgKey, args);
    }

    public String getLocaleMessage(String msgKey)
    {
        return UtilBean.getLocaleMessage(msgKey);
    }
}
