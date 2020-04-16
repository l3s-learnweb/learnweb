package de.l3s.learnweb.gdpr;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;

/**
 * YourResourcesBean is responsible for displaying user resources.
 */
@Named
@ViewScoped
public class YourResourcesBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 5490911640532982940L;
    //private static final Logger log = LogManager.getLogger(YourResourcesBean.class);

    private List<Resource> userResources;

    public YourResourcesBean() throws SQLException
    {
        User user = getUser();
        if(null == user)
            // when not logged in
            return;

        this.userResources = user.getResources();
    }

    public List<Resource> getUserResources()
    {
        return userResources;
    }
}
