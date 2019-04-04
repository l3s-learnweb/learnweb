package de.l3s.learnweb.gdpr;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.User;
import org.apache.log4j.Logger;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

/**
 * PersonalInfoBean is responsible for displaying detailed information about user.
 */
@Named
@ViewScoped
public class YourDetailedInfoBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6016324259224515500L;
    private static final Logger log = Logger.getLogger(YourDetailedInfoBean.class);

    private String fullName;
    private String address;
    private String dateOfBirth;
    private String studentId;
    private String userImage;
    private String userOrganisation;
    private String userProfession;

    public YourDetailedInfoBean() throws SQLException
    {
        User user = getUser();
        if(null == user)
            // when not logged in
            return;

        this.fullName = user.getFullName();
        if(null == fullName)
        {
            this.fullName = "N/A";
        }

        this.address = user.getAddress();
        if(null == address)
        {
            this.address = "N/A";
        }

        Date birthDate = user.getDateOfBirth();
        if(null != birthDate)
        {
            this.dateOfBirth = user.getDateOfBirth().toString();
        }
        else
        {
            this.dateOfBirth = "N/A";
        }

        this.studentId = user.getStudentId();
        if(null == studentId)
        {
            this.studentId = "N/A";
        }

        this.userImage = user.getImage();
        if(null == userImage){
            this.userImage = "no_profile.jpg";
        }

        this.userOrganisation = user.getOrganisation().getTitle();
        if(null == userOrganisation)
        {
            this.userOrganisation = "N/A";
        }

        this.userProfession = user.getProfession();
        if(null == userProfession || userProfession.equals(""))
        {
            this.userProfession = "N/A";
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

    public String getUserImage(){
        return this.userImage;
    }

    public String getUserOrganisation()
    {
        return userOrganisation;
    }

    public String getUserProfession()
    {
        return userProfession;
    }
}
