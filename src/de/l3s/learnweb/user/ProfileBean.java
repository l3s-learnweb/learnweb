package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.primefaces.event.FileUploadEvent;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.user.User.Gender;

@Named
@ViewScoped
public class ProfileBean extends ApplicationBean implements Serializable
{
    private static final Logger log = Logger.getLogger(ProfileBean.class);
    private static final long serialVersionUID = -2460055719611784132L;

    private int userId;

    @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String confirmPassword;

    @NotBlank
    private String currentPassword;

    private User selectedUser;
    private boolean moderatorAccess = false; // the user is edited by a moderator
    private boolean affiliationRequired = false;
    private boolean studentIdRequired = false;
    private boolean mailRequired = false;
    private boolean anonymizeUsername;

    public void loadUser() throws SQLException
    {
        User loggedinUser = getUser();
        if(loggedinUser == null)
            return;

        if(userId == 0 || loggedinUser.getId() == userId)
            selectedUser = getUser(); // user edits himself
        else
        {
            selectedUser = getLearnweb().getUserManager().getUser(userId); // an admin edits an user
            moderatorAccess = true;
        }

        if(null == selectedUser)
        {
            addInvalidParameterMessage("user_id");
            return;
        }

        if(moderatorAccess && !UtilBean.getUserBean().canModerateCourses(selectedUser.getCourses()))
        {
            selectedUser = null;
            addMessage(FacesMessage.SEVERITY_ERROR, "You are not allowed to edit this user");
            return;
        }

        email = selectedUser.getEmail();

        for(Course course : selectedUser.getCourses())
        {
            if(course.getOption(Course.Option.Users_Require_mail_address))
                mailRequired = true;

            if(course.getOption(Course.Option.Users_Require_affiliation))
                affiliationRequired = true;

            if(course.getOption(Course.Option.Users_Require_student_id))
                studentIdRequired = true;
        }

        anonymizeUsername = selectedUser.getOrganisation().getOption(Organisation.Option.Privacy_Anonymize_usernames);
    }

    public void handleFileUpload(FileUploadEvent event)
    {
        try
        {
            getUser().setImage(event.getFile().getInputStream());
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
        if(StringUtils.isNotEmpty(email) && !StringUtils.equals(selectedUser.getEmail(), email))
        {
            selectedUser.setEmail(email);

            if(selectedUser.sendEmailConfirmation())
                addMessage(FacesMessage.SEVERITY_INFO, "email_has_been_send");
            else
                addMessage(FacesMessage.SEVERITY_FATAL, "We were not able to send a confirmation mail");
        }

        getLearnweb().getUserManager().save(selectedUser);

        log(Action.changing_profile, 0, selectedUser.getId());
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

                log(Action.deleted_user_hard, 0, getSelectedUser().getId());
            }
            else
            {
                getLearnweb().getUserManager().deleteUserSoft(getSelectedUser());

                log(Action.deleted_user_soft, 0, getSelectedUser().getId());
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
        String newName = ((String) value).trim();
        if(getSelectedUser().getRealUsername().equals(newName))
        { // username not changed
            return;
        }

        if(newName.length() < 2)
        {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "The username is to short."));
        }

        if(getLearnweb().getUserManager().isUsernameAlreadyTaken(newName))
        {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "username_already_taken"));
        }
    }

    public Date getMaxBirthday()
    {
        return new Date();
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public boolean isModeratorAccess()
    {
        return moderatorAccess;
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

    public User getSelectedUser()
    {
        return selectedUser;
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

    public void validateConsent(FacesContext context, UIComponent component, Object value) throws ValidatorException, SQLException
    {
        if(value.equals(Boolean.FALSE))
        {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "consent_is_required"));
        }
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
    }

    public Gender[] getGenders()
    {
        return User.Gender.values();
    }

}
