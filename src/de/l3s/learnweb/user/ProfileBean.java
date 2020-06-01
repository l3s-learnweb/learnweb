package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.primefaces.event.FileUploadEvent;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.GroupUser;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.user.User.Gender;

@Named
@ViewScoped
public class ProfileBean extends ApplicationBean implements Serializable {
    private static final Logger log = LogManager.getLogger(ProfileBean.class);
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
    private List<GroupUser> userGroups;

    public void onLoad() throws SQLException {
        User loggedinUser = getUser();
        if (loggedinUser == null) {
            return;
        }

        if (userId == 0 || loggedinUser.getId() == userId) {
            selectedUser = getUser(); // user edits himself
        } else {
            selectedUser = getLearnweb().getUserManager().getUser(userId); // an admin edits an user
            moderatorAccess = true;
        }

        if (null == selectedUser) {
            addInvalidParameterMessage("user_id");
            return;
        }

        if (moderatorAccess && !loggedinUser.canModerateUser(selectedUser)) {
            selectedUser = null;
            addAccessDeniedMessage();
            return;
        }

        email = selectedUser.getEmail();

        for (Course course : selectedUser.getCourses()) {
            if (course.getOption(Course.Option.Users_Require_mail_address)) {
                mailRequired = true;
            }

            if (course.getOption(Course.Option.Users_Require_affiliation)) {
                affiliationRequired = true;
            }

            if (course.getOption(Course.Option.Users_Require_student_id)) {
                studentIdRequired = true;
            }
        }

        anonymizeUsername = selectedUser.getOrganisation().getOption(Organisation.Option.Privacy_Anonymize_usernames);
        userGroups = selectedUser.getGroupsRelations();
    }

    public void handleFileUpload(FileUploadEvent event) {
        try {
            getUser().setImage(event.getFile().getInputStream());
            getUser().save();
        } catch (IllegalArgumentException e) { // image is smaller than 100px
            log.error("unhandled error", e);

            if (e.getMessage().startsWith("Width 100 exceeds")) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Your image is to small.");
            } else {
                throw e;
            }
        } catch (Exception e) {
            log.error("Fatal error while processing a user image", e);
            addMessage(FacesMessage.SEVERITY_FATAL, "Fatal error while processing your image.");
        }
    }

    public void onSaveProfile() throws SQLException {
        // send confirmation mail if mail has been changed
        if (StringUtils.isNotEmpty(email) && !StringUtils.equals(selectedUser.getEmail(), email)) {
            selectedUser.setEmail(email);

            if (selectedUser.sendEmailConfirmation()) {
                addMessage(FacesMessage.SEVERITY_INFO, "email_has_been_sent");
            } else {
                addMessage(FacesMessage.SEVERITY_FATAL, "We were not able to send a confirmation mail");
            }
        }

        getLearnweb().getUserManager().save(selectedUser);

        log(Action.changing_profile, 0, selectedUser.getId());
        addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    public void onChangePassword() {
        UserManager um = getLearnweb().getUserManager();
        try {
            getSelectedUser().setPassword(password);
            um.save(getSelectedUser());

            addGrowl(FacesMessage.SEVERITY_INFO, "password_changed");

            password = "";
            confirmPassword = "";
            currentPassword = "";
        } catch (SQLException e) {
            addErrorMessage(e);
        }
    }

    public String onDeleteAccount() {
        try {
            User user = getUser();

            if (!user.equals(getSelectedUser()) && !user.canModerateUser(getSelectedUser())) {
                addAccessDeniedMessage();
                return null;
            }

            getLearnweb().getUserManager().deleteUserSoft(getSelectedUser());
            log(Action.deleted_user_soft, 0, getSelectedUser().getId());

            addMessage(FacesMessage.SEVERITY_INFO, "user.account.deleted");
            setKeepMessages();

            // perform logout if necessary
            UserBean userBean = getUserBean();
            if (userBean.getModeratorUser() != null && !userBean.getModeratorUser().equals(user)) { // a moderator was logged into another user's account
                userBean.setUser(userBean.getModeratorUser()); // logout user and login moderator
                userBean.setModeratorUser(null);
                return "/lw/admin/users.xhtml?faces-redirect=true";
            } else if (user.isModerator() && !user.equals(getSelectedUser())) { // a moderator deletes another user through his profile page
                return "/lw/admin/users.xhtml?faces-redirect=true";
            }

            // a user deletes himself
            getFacesContext().getExternalContext().invalidateSession(); // end session
            return "/lw/user/login.xhtml?faces-redirect=true";
        } catch (Exception e) {
            addErrorMessage(e);
        }
        return null;
    }

    public void validateUsername(FacesContext context, UIComponent component, Object value) throws ValidatorException, SQLException {
        String newName = ((String) value).trim();
        if (getSelectedUser().getRealUsername().equals(newName)) { // username not changed
            return;
        }

        if (getLearnweb().getUserManager().isUsernameAlreadyTaken(newName)) {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "username_already_taken"));
        }
    }

    public Date getMaxBirthday() {
        return new Date();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isModeratorAccess() {
        return moderatorAccess;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public User getSelectedUser() {
        return selectedUser;
    }

    public List<GroupUser> getUserGroups() {
        return userGroups;
    }

    public void setUserGroups(final List<GroupUser> userGroups) {
        this.userGroups = userGroups;
    }

    public void validateCurrentPassword(FacesContext context, UIComponent component, Object value) throws ValidatorException, SQLException {
        if (getUser().isAdmin()) { // admins can change the password of all users
            return;
        }

        UserManager um = getLearnweb().getUserManager();
        User user = getSelectedUser(); // the current user

        String password = (String) value;

        // returns the same user, if the password is correct
        User checkUser = um.getUser(user.getRealUsername(), password);

        if (!user.equals(checkUser)) {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "password_incorrect"));
        }
    }

    public boolean isAffiliationRequired() {
        return affiliationRequired;
    }

    public boolean isMailRequired() {
        return mailRequired;
    }

    public boolean isStudentIdRequired() {
        return studentIdRequired;
    }

    public boolean isAnonymizeUsername() {
        return anonymizeUsername;
    }

    public void validateConsent(FacesContext context, UIComponent component, Object value) throws ValidatorException, SQLException {
        if (value.equals(Boolean.FALSE)) {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "consent_is_required"));
        }
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Gender[] getGenders() {
        return User.Gender.values();
    }

    /**
     * Sets users preferredNotificationFrequency and the frequency of all his groups.
     */
    public void onSaveAllNotificationFrequencies() throws SQLException {
        selectedUser.save();
        for (GroupUser groupUser : userGroups) {
            groupUser.setNotificationFrequency(selectedUser.getPreferredNotificationFrequency());
            getLearnweb().getGroupManager().updateNotificationFrequency(groupUser.getGroup().getId(), selectedUser.getId(), groupUser.getNotificationFrequency());
        }

        addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    public void onSaveNotificationFrequency(GroupUser group, int userId) throws SQLException {
        getLearnweb().getGroupManager().updateNotificationFrequency(group.getGroup().getId(), userId, group.getNotificationFrequency());

        addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    public List<String> getTimeZonesIds() {
        List<String> zoneList = new ArrayList<>(ZoneId.getAvailableZoneIds());
        Collections.sort(zoneList);
        return zoneList;
    }

    public User.NotificationFrequency[] getNotificationFrequencies() {
        return User.NotificationFrequency.values();
    }
}
