package de.l3s.learnweb.gdpr;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.User;

/**
 * PersonalInfoBean is responsible for displaying detailed information about user.
 */
@Named
@ViewScoped
public class YourPersonalInfoBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6016324259224515500L;
    //private static final Logger log = LogManager.getLogger(YourPersonalInfoBean.class);

    private String fullName;
    private String address;
    private String dateOfBirth;
    private String studentId;
    private String userImage;
    private String userOrganisation;
    private String userProfession;

    public YourPersonalInfoBean() throws SQLException
    {
        User user = getUser();
        if(null == user)
            // when not logged in
            return;

        // TODO use a helper method to get rid of all these if statements

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
            this.dateOfBirth = birthDate.toString(); // TODO this is bad. USe HSF date formatting
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

        this.userOrganisation = user.getOrganisation().getTitle();
        if(null == userOrganisation)
        {
            this.userOrganisation = "N/A";
        }

        this.userProfession = user.getProfession();
        if(null == userProfession || userProfession.isEmpty())
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

    public String getUserImage()
    {
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
