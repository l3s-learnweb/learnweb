package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.group.Group;

import javax.enterprise.context.SessionScoped;
import javax.faces.bean.ManagedBean;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@ManagedBean(name = "yourGroupsBean", eager = true)
@SessionScoped
public class YourGroupsBean extends YourGeneralInfoBean {
    private List<Group> userGroups;

    public YourGroupsBean() {
        try{
            this.userGroups = this.getUser().getGroups();
        } catch(SQLException sqlException){
            this.userGroups = new ArrayList<>();
            logger.error("Could not properly retrieve user groups." + sqlException);
        }
    }

    public List<Group> getUserGroups()
    {
        return userGroups;
    }

    public void setUserGroups(final List<Group> userGroups)
    {
        this.userGroups = userGroups;
    }
}
