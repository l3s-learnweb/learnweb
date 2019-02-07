package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

@Named
@ViewScoped
public class YourGroupsBean extends ApplicationBean implements Serializable {
    private static final Logger logger = Logger.getLogger(YourGroupsBean.class);

    public YourGroupsBean() { }

    public List<Group> getUserGroups() {
        try {
            return this.getUser().getGroups();
        }
        catch(SQLException sqlException) {
            logger.error("Could not properly retrieve user groups." + sqlException);
            return new ArrayList<>();
        }
    }
}
