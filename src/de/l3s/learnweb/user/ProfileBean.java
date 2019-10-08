package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.validator.constraints.Length;
import org.primefaces.event.FileUploadEvent;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogEntry;

@Named
@ViewScoped
public class ProfileBean extends ApplicationBean implements Serializable
{
    private static final Logger log = Logger.getLogger(ProfileBean.class);
    private static final long serialVersionUID = -2460055719611784132L;

    @Length(max = 250)
    private String address;

    private Date dateOfBirth;

    @Email
    private String email;

    @Min(value = 0)
    @Max(value = 2)
    private int gender;

    @Length(max = 50)
    private String interest;

    @NotBlank
    @Length(min = 2, max = 50)
    private String username;

    @Length(max = 50)
    private String studentId;

    @Length(max = 80)
    private String profession;

    @Length(max = 250)
    private String additionalInformation;

    @NotBlank
    private String password;

    @NotBlank
    private String confirmPassword;

    @NotBlank
    private String currentPassword;

    @Length(max = 250)
    private String fullName;

    @Length(max = 250)
    private String affiliation;

    @Length(max = 250)
    private String credits;
    private boolean acceptTermsAndConditions;

    private List<LogEntry> logMessages;

    private User user;
    private boolean moderatorAccess = false;
    private boolean affiliationRequired = false;
    private boolean studentIdRequired = false;
    private boolean mailRequired = false;
    private boolean anonymizeUsername;

    public List<LogEntry> getLogMessages()
    {
        return logMessages;
    }

    public ProfileBean() throws SQLException
    {
        Integer userId = getParameterInt("user_id");

        if(userId != null) // moderator edits the defined user
        {
            log.debug("Edit profile of user: " + userId);

            user = getLearnweb().getUserManager().getUser(userId);

            if(null == user)
            {
                addInvalidParameterMessage("user_id");
                return;
            }

            if(!UtilBean.getUserBean().canModerateCourses(user.getCourses()))
            {
                user = null;
                addMessage(FacesMessage.SEVERITY_ERROR, "You are not allowed to edit this user");
                return;
            }
            else
                moderatorAccess = true;
        }
        else // edit own profile
            user = getUser();

        if(user == null)
            return;

        username = user.getRealUsername();
        email = user.getEmail();
        studentId = user.getStudentId();
        gender = user.getGender();
        dateOfBirth = user.getDateOfBirth();
        additionalInformation = user.getAdditionalInformation();
        interest = user.getInterest();
        address = user.getAddress();
        profession = user.getProfession();
        fullName = user.getFullName();
        affiliation = user.getAffiliation();
        credits = user.getCredits();
        acceptTermsAndConditions = user.isAcceptTermsAndConditions();

        for(Course course : user.getCourses())
        {
            if(course.getOption(Course.Option.Users_Require_mail_address))
                mailRequired = true;

            if(course.getOption(Course.Option.Users_Require_affiliation))
                affiliationRequired = true;

            if(course.getOption(Course.Option.Users_Require_student_id))
                studentIdRequired = true;
        }

        anonymizeUsername = user.getOrganisation().getOption(Organisation.Option.Privacy_Anonymize_usernames);
    }

    public void handleFileUpload(FileUploadEvent event)
    {
        try
        {
            getUser().setImage(event.getFile().getInputstream());
            getUser().save();
        }
        catch(IllegalArgumentException e) // image is smaller than 100px
        {

            log.error("unhandled error", e);

            if(e.getMessage().startsWith("Width 100 exceeds"))
                addMessage(FacesMessage.SEVERITY_ERROR, "Your image is to small.");
            else
                throw e;
        }
        catch(Exception e)
        {
            log.error("Fatal error while processing a user image", e);
            addMessage(FacesMessage.SEVERITY_FATAL, "Fatal error while processing your image.");
        }
    }

    public void onSaveProfile() throws SQLException
    {
        // send confirmation mail if mail has been changed
        if(StringUtils.isNotEmpty(email) && !StringUtils.equals(user.getEmail(), email))
        {
            user.setEmail(email);

            if(user.sendEmailConfirmation())
                addMessage(FacesMessage.SEVERITY_INFO, "email_has_been_send");
            else
                addMessage(FacesMessage.SEVERITY_FATAL, "We were not able to send a confirmation mail");
        }

        user.setAdditionalInformation(additionalInformation);
        user.setAddress(address);
        user.setDateOfBirth(dateOfBirth);
        user.setGender(gender);
        user.setInterest(interest);
        user.setStudentId(studentId);
        user.setProfession(profession);
        user.setUsername(username);
        user.setAffiliation(affiliation);
        user.setFullName(fullName);
        user.setAcceptTermsAndConditions(acceptTermsAndConditions);

        if(isModeratorAccess())
            user.setCredits(credits);

        getLearnweb().getUserManager().save(user);

        log(Action.changing_profile, 0, user.getId());
        addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    public void onChangePassword()
    {
        UserManager um = getLearnweb().getUserManager();
        try
        {
            getSelectedUser().setPassword(password);
            um.save(getSelectedUser());

            addMessage(FacesMessage.SEVERITY_INFO, "password_changed");

            password = "";
            confirmPassword = "";
            currentPassword = "";
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
        }
    }

    public String onDeleteAccountSoft()
    {
        return onDelete(false);
    }

    public String onDeleteAccountHard()
    {
        return onDelete(true);
    }

    /**
     * @see de.l3s.learnweb.user.UserManager#deleteUserSoft
     * @see de.l3s.learnweb.user.UserManager#deleteUserHard
     * @param hardDelete
     * @return
     */
    private String onDelete(boolean hardDelete)
    {
        try
        {
            if(hardDelete)
            {
                getLearnweb().getUserManager().deleteUserHard(getSelectedUser());

                log(Action.deleted_user_hard, 0, 0);
            }
            else
            {
                getLearnweb().getUserManager().deleteUserSoft(getSelectedUser());

                log(Action.deleted_user_soft, 0, 0);
            }

            getFacesContext().getExternalContext().invalidateSession(); // end session
            addMessage(FacesMessage.SEVERITY_INFO, "user.account.deleted");
            setKeepMessages();
            return "/lw/user/login.jsf&faces-redirect=true";
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
        return null;

    }

    public void validateUsername(FacesContext context, UIComponent component, Object value) throws ValidatorException, SQLException
    {
        if(getSelectedUser().getRealUsername().equals(value))
        { // username not changed
            return;
        }

        if(getLearnweb().getUserManager().isUsernameAlreadyTaken((String) value))
        {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "username_already_taken"));
        }
    }

    public Date getMaxBirthday()
    {
        return new Date();
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public Date getDateOfBirth()
    {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth)
    {
        this.dateOfBirth = dateOfBirth;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public int getGender()
    {
        return gender;
    }

    public void setGender(int gender)
    {
        this.gender = gender;
    }

    public String getInterest()
    {
        return interest;
    }

    public void setInterest(String interest)
    {
        this.interest = interest;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getStudentId()
    {
        return studentId;
    }

    public void setStudentId(String phone)
    {
        this.studentId = phone;
    }

    public String getProfession()
    {
        return profession;
    }

    public boolean isModeratorAccess()
    {
        return moderatorAccess;
    }

    public void setProfession(String profession)
    {
        this.profession = profession;
    }

    public String getAdditionalInformation()
    {
        return additionalInformation;
    }

    public void setAdditionalInformation(String additionalInformation)
    {
        this.additionalInformation = additionalInformation;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getConfirmPassword()
    {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword)
    {
        this.confirmPassword = confirmPassword;
    }

    public String getCurrentPassword()
    {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword)
    {
        this.currentPassword = currentPassword;
    }

    public String getFullName()
    {
        return fullName;
    }

    public void setFullName(String fullName)
    {
        this.fullName = fullName;
    }

    public String getAffiliation()
    {
        return affiliation;
    }

    public void setAffiliation(String affiliation)
    {
        this.affiliation = affiliation;
    }

    public String getCredits()
    {
        return credits;
    }

    public void setCredits(String credits)
    {
        this.credits = credits;
    }

    public User getSelectedUser()
    {
        return user;
    }

    public void validateCurrentPassword(FacesContext context, UIComponent component, Object value) throws ValidatorException, SQLException
    {
        if(getUser().isAdmin()) // admins can change the password of all users
            return;

        UserManager um = getLearnweb().getUserManager();
        User user = getSelectedUser(); // the current user

        String password = (String) value;

        // returns the same user, if the password is correct
        User checkUser = um.getUser(user.getRealUsername(), password);

        if(!user.equals(checkUser))
        {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "password_incorrect"));
        }
    }

    public void validatePassword(FacesContext context, UIComponent component, Object value) throws ValidatorException
    {
        // Find the actual JSF component for the first password field.
        UIInput passwordInput = (UIInput) context.getViewRoot().findComponent("passwordform:password");

        // Get its value, the entered password of the first field.
        String password = (String) passwordInput.getValue();

        if(null != password && !password.equals(value))
        {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "passwords_do_not_match"));
        }
    }

    public boolean isAffiliationRequired()
    {
        return affiliationRequired;
    }

    public boolean isMailRequired()
    {
        return mailRequired;
    }

    public boolean isStudentIdRequired()
    {
        return studentIdRequired;
    }

    public boolean isAnonymizeUsername()
    {
        return anonymizeUsername;
    }

    public boolean isAcceptTermsAndConditions()
    {
        return acceptTermsAndConditions;
    }

    public void setAcceptTermsAndConditions(boolean acceptTermsAndConditions)
    {
        this.acceptTermsAndConditions = acceptTermsAndConditions;
    }

    public void validateConsent(FacesContext context, UIComponent component, Object value) throws ValidatorException, SQLException
    {
        if(value.equals(Boolean.FALSE))
        {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "consent_is_required"));
        }
    }
}
