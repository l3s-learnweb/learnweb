package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.User;
import org.apache.log4j.Logger;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Date;

/**
 * PersonalInfoBean is responsible for displaying detailed information about user.
 */
@Named
@ViewScoped
public class YourDetailedInfoBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6016324259224515500L;
    private static final Logger log = Logger.getLogger(YourCoursesBean.class);


    private String fullName;
    private String address;
    private String dateOfBirth;
    private String studentId;

    public YourDetailedInfoBean()
    {
        User user = getUser();
        if(null == user)
            // when not logged in
            return;

        this.fullName = this.getUser().getFullName();
        if(null == fullName)
        {
            this.fullName = "N/A";
        }

        this.address = this.getUser().getAddress();
        if(null == address)
        {
            this.address = "N/A";
        }

        Date birthDate = this.getUser().getDateOfBirth();
        if(null != birthDate)
        {
            this.dateOfBirth = this.getUser().getDateOfBirth().toString();
        }
        else
        {
            this.dateOfBirth = "N/A";
        }

        this.studentId = this.getUser().getStudentId();
        if(null == studentId)
        {
            this.studentId = "N/A";
        }
    }

    public String getFullName()
    {
        return this.fullName;
    }

    public String getAddress()
    {
        return this.address;
    }

    public String getEmail()
    {
        return this.getUser().getEmail();
    }

    public String getDateOfBirth()
    {
        return this.dateOfBirth;
    }

    public String getStudentId()
    {
        return this.studentId;
    }
}
