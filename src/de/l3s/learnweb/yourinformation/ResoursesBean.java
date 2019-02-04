package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.resource.Resource;

import javax.inject.Named;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Named
public class ResoursesBean extends GeneralinfoBean {
    private List<Resource> userResourses;

    public ResoursesBean() {
        try{
            userResourses = user.getResources();
        } catch(SQLException sqlException){
            this.userResourses = new ArrayList<>();
            logger.error("Could not properly retrieve user resourses." + sqlException);
        }
    }

    public List<Resource> getUserResourses()
    {
        return userResourses;
    }

    public void setUserResourses(final List<Resource> userResourses)
    {
        this.userResourses = userResourses;
    }
}
