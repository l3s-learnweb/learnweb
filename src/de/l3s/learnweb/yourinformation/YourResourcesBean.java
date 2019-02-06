package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.resource.Resource;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@ManagedBean(name = "yourResourcesBean", eager = true)
@SessionScoped
public class YourResourcesBean extends YourGeneralInfoBean {
    private List<Resource> userResourses;

    public YourResourcesBean() {
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
