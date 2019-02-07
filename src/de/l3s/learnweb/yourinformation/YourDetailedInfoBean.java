package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.beans.ApplicationBean;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Date;

/*
* PersonalInfoBean is responsible for displaying detailed information about user.
* */
@Named
@ViewScoped
public class YourDetailedInfoBean extends ApplicationBean implements Serializable {

    public YourDetailedInfoBean() { }

    public String getFullName() {
        String fullName = this.getUser().getFullName();
        if(null == fullName) {
            fullName = "N/A";
        }
        return fullName;
    }

    public String getAddress() {
        String address = this.getUser().getAddress();
        if(null == address) {
            address = "N/A";
        }
        return address;
    }

    public String getEmail() {
        return this.getUser().getEmail();
    }

    public String getDateOfBirth() {
        Date birthDate = this.getUser().getDateOfBirth();
        String dateOfBirth;
        if (null != birthDate){
            dateOfBirth = this.getUser().getDateOfBirth().toString();
        } else {
            dateOfBirth = "N/A";
        }
        return dateOfBirth;
    }

    public String getStudentId() {
        String studentId = this.getUser().getStudentId();
        if(null == studentId) {
            studentId = "N/A";
        }
        return studentId;
    }
}
