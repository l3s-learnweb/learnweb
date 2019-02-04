package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.group.Group;

import javax.inject.Named;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Named
public class PersonalGroupsBean extends GeneralinfoBean {
    private List<Group> userGroups;

    public PersonalGroupsBean() {
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
