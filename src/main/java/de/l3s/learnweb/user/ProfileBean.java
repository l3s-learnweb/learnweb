package de.l3s.learnweb.user;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.validator.ValidatorException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Faces;
import org.primefaces.event.FileUploadEvent;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.group.GroupUser;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.FileDao;
import de.l3s.learnweb.user.User.Gender;
import de.l3s.util.Image;

@Named
@ViewScoped
public class ProfileBean extends ApplicationBean implements Serializable {
    private static final Logger log = LogManager.getLogger(ProfileBean.class);
    @Serial
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

    private transient List<SelectItem> timeZoneIds; // A list of all available time zone ids

    @Inject
    private UserDao userDao;

    @Inject
    private FileDao fileDao;

    @Inject
    private GroupDao groupDao;

    public void onLoad() {
        User loggedInUser = getUser();
        BeanAssert.authorized(loggedInUser);

        if (userId == 0 || loggedInUser.getId() == userId) {
            selectedUser = loggedInUser; // user edits himself
        } else {
            selectedUser = userDao.findByIdOrElseThrow(userId); // an admin edits an user
            moderatorAccess = true;
        }

        BeanAssert.hasPermission(!moderatorAccess || loggedInUser.canModerateUser(selectedUser));

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
        userGroups = groupDao.findGroupUserRelations(selectedUser.getId());
    }

    public void handleFileUpload(FileUploadEvent event) {
        try {
            // process image
            Image img = new Image(event.getFile().getInputStream());

            // save image file
            File file = new File(File.FileType.PROFILE_PICTURE, "profile_picture.png", "image/png");
            Image thumbnail = img.getResizedToSquare(200);
            fileDao.save(file, thumbnail.getInputStream());
            thumbnail.dispose();

            selectedUser.getImageFile().ifPresent(image -> fileDao.deleteHard(image)); // delete old image
            selectedUser.setImageFileId(file.getId());
            userDao.save(selectedUser);
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

    public void onSaveProfile() {
        // send confirmation mail if mail has been changed
        if (StringUtils.isNotEmpty(email) && !StringUtils.equals(selectedUser.getEmail(), email)) {
            selectedUser.setEmail(email);

            if (selectedUser.sendEmailConfirmation()) {
                addMessage(FacesMessage.SEVERITY_INFO, "email_has_been_sent");
            } else {
                addMessage(FacesMessage.SEVERITY_FATAL, "We were not able to send a confirmation mail");
            }
        }

        userDao.save(selectedUser);

        log(Action.changing_profile, 0, selectedUser.getId());
        addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    public void onChangePassword() {
        getSelectedUser().setPassword(password);
        userDao.save(getSelectedUser());

        addGrowl(FacesMessage.SEVERITY_INFO, "password_changed");

        password = "";
        confirmPassword = "";
        currentPassword = "";
    }

    public String onDeleteAccount() {
        User user = getUser();
        BeanAssert.hasPermission(user.equals(getSelectedUser()) || user.canModerateUser(getSelectedUser()));

        userDao.deleteSoft(getSelectedUser());
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
        Faces.invalidateSession();
        return "/lw/user/login.xhtml?faces-redirect=true";
    }

    public void onGuideReset() {
        selectedUser.getGuideSteps().clear();
        userDao.save(selectedUser);
        addGrowl(FacesMessage.SEVERITY_INFO, "guide.reset");
    }

    public void onGuideSkip() {
        selectedUser.getGuideSteps().set(0, Long.SIZE, true);
        userDao.save(selectedUser);
        addGrowl(FacesMessage.SEVERITY_INFO, "guide.skipped");
    }

    public void validateUsername(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        String newName = ((String) value).trim();
        if (getSelectedUser().getRealUsername().equals(newName)) { // username not changed
            return;
        }

        if (userDao.findByUsername(newName).isPresent()) {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "username_already_taken"));
        }
    }

    public LocalDate getMaxBirthday() {
        return LocalDate.now();
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

    public void validateCurrentPassword(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (getUser().isAdmin()) { // admins can change the password of all users
            return;
        }

        User user = getSelectedUser(); // the current user

        String password = (String) value;

        // returns the same user, if the password is correct
        Optional<User> checkUser = userDao.findByUsernameAndPassword(user.getRealUsername(), password);

        if (checkUser.isEmpty() || !user.equals(checkUser.get())) {
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

    public void validateConsent(FacesContext context, UIComponent component, Object value) throws ValidatorException {
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
    public void onSaveAllNotificationFrequencies() {
        userDao.save(selectedUser);

        for (GroupUser groupUser : userGroups) {
            groupUser.setNotificationFrequency(selectedUser.getPreferredNotificationFrequency());
            groupDao.updateNotificationFrequency(groupUser.getNotificationFrequency(), groupUser.getGroupId(), selectedUser.getId());
        }

        addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    public void onSaveNotificationFrequency(GroupUser group, int userId) {
        groupDao.updateNotificationFrequency(group.getNotificationFrequency(), group.getGroupId(), userId);
        addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    /**
     *
     * @return A list of all available time zone ids
     */
    public List<SelectItem> getTimeZoneIds() {
        if (null == timeZoneIds) {
            Locale locale = getUserBean().getLocale();

            timeZoneIds = ZoneId.getAvailableZoneIds().stream().filter(id -> !StringUtils.startsWithAny(id, "Etc/", "SystemV/", "PST8PDT", "GMT")).sorted()
                .map(id -> new SelectItem(ZoneId.of(id), id.replace("_", " ") + " (" + ZoneId.of(id).getDisplayName(TextStyle.FULL_STANDALONE, locale) + ")"))
                .collect(Collectors.toList());
        }
        return timeZoneIds;
    }

    public User.NotificationFrequency[] getNotificationFrequencies() {
        return User.NotificationFrequency.values();
    }

    public boolean isEditingDisabled() {
        return selectedUser.getOrganisation().getOption(Organisation.Option.Privacy_Profile_prevent_edit);
    }

    public String rootLogin() {
        return LoginBean.rootLogin(this, selectedUser);
    }
}
