package de.l3s.learnweb.yourinformation;

import javax.inject.Named;

/*
* PersonalInfoBean is responsible for displaying detailed information about user.
* */
@Named
public class DetailedInfoBean extends GeneralinfoBean {
    private String fullName;
    private String address;
    private String email;
    private String dateOfBirth;
    private String studentId;

    public DetailedInfoBean() {
        this.fullName = user.getFullName();
        if (null == this.fullName){
            this.fullName = "N/A";
        }
        this.address = user.getAddress();
        if (null == this.address){
            this.address = "N/A";
        }
        this.email = user.getEmail();
        if (null != user.getDateOfBirth()){
            this.dateOfBirth = user.getDateOfBirth().toString();
        } else {
            this.dateOfBirth = "N/A";
        }
        if (null != user.getStudentId()){
            this.studentId = user.getStudentId();
        } else {
            this.studentId = "N/A";
        }
    }

    public String getFullName()
    {
        return fullName;
    }

    public void setFullName(final String fullName)
    {
        this.fullName = fullName;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(final String address)
    {
        this.address = address;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(final String email)
    {
        this.email = email;
    }

    public String getDateOfBirth()
    {
        return dateOfBirth;
    }

    public void setDateOfBirth(final String dateOfBirth)
    {
        this.dateOfBirth = dateOfBirth;
    }

    public String getStudentId()
    {
        return studentId;
    }

    public void setStudentId(final String studentId)
    {
        this.studentId = studentId;
    }
}
