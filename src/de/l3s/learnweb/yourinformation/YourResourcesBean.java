package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.Resource;
import org.apache.log4j.Logger;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class YourResourcesBean extends ApplicationBean implements Serializable {
    private static final Logger logger = Logger.getLogger(YourResourcesBean.class);

    public YourResourcesBean() { }

    public List<Resource> getUserResourses() {
        List<Resource> userResourses;
        try {
            userResourses = this.getUser().getResources();
        }
        catch(SQLException sqlException) {
            userResourses = new ArrayList<>();
            logger.error("Could not properly retrieve user resourses." + sqlException);
        }

        return userResourses;
    }
}
