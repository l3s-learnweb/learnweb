package de.l3s.learnweb.user;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Faces;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.exceptions.BadRequestHttpException;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.File;
import de.l3s.util.HashHelper;
import de.l3s.util.ProfileImageHelper;
import de.l3s.util.bean.BeanHelper;

@Named
@ViewScoped
public class RegistrationBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 4567220515408089722L;
    private static final Logger log = LogManager.getLogger(RegistrationBean.class);

    @Size(min = 2, max = 50)
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String studentId;

    @Email
    private String email;

    private boolean acceptPrivacyPolicy = false;
    private boolean acceptTracking = false;

    private String wizard;
    private String group;
    private String fastLogin;

    private String affiliation;

    private Course course;
    private boolean mailRequired = false;
    private boolean affiliationRequired = false;
    private boolean studentIdRequired = false;
    private String timeZone;
    private Locale locale;

    @Inject
    private CourseDao courseDao;

    @Inject
    private UserDao userDao;

    @Inject
    private ConfirmRequiredBean confirmRequiredBean;

    public String onLoad() {
        locale = Faces.getLocale();

        if (null == locale) {
            log.warn("locale is null; request: {}", BeanHelper.getRequestSummary());
            locale = Locale.ENGLISH;
        }

        if (StringUtils.isNotEmpty(wizard)) {
            course = courseDao.findByWizard(wizard).orElseThrow(() -> new BadRequestHttpException("register_invalid_wizard_error"));
            BeanAssert.validate(!course.isWizardDisabledOrNull(), "registration.wizard_disabled");

            // special message for yell
            if (course.getId() == 505) {
                addMessage(FacesMessage.SEVERITY_INFO, "register_for_community", course.getTitle());
            } else {
                addMessage(FacesMessage.SEVERITY_INFO, "register_for_course", course.getTitle());
            }

            if (StringUtils.isNotEmpty(fastLogin)) {
                return fastLogin();
            }
        } else {
            course = courseDao.findByWizard("default").orElseThrow(BeanAssert.NOT_FOUND);

            addMessage(FacesMessage.SEVERITY_WARN, "register_without_wizard_warning");
        }

        mailRequired = course.getOption(Course.Option.Users_Require_mail_address);
        affiliationRequired = course.getOption(Course.Option.Users_Require_affiliation);
        studentIdRequired = course.getOption(Course.Option.Users_Require_student_id);
        return null;
    }

    private String fastLogin() {
        Optional<User> existingUser = userDao.findByUsername(fastLogin);

        if (existingUser.isPresent()) {
            if (existingUser.get().getPassword() == null && existingUser.get().isMemberOfCourse(course.getId())) {
                return LoginBean.loginUser(this, existingUser.get());
            } else {
                addMessage(FacesMessage.SEVERITY_FATAL, "You should use password to login.");
                return "/lw/user/login.xhtml?faces-redirect=true";
            }
        } else {
            User user = new User();
            user.setUsername(fastLogin);
            user.setEmail(null);
            user.setPassword(null);
            user.setTimeZone(ZoneId.of("Europe/Berlin"));
            user.setLocale(locale);

            registerUser(user);
            return LoginBean.loginUser(this, user);
        }
    }

    /**
     * Handles errors that may occur while retrieving the users time zone.
     *
     * @return ZoneId given by the user or GMT as default value
     */
    private ZoneId getZoneId() {
        try {
            return ZoneId.of(getTimeZone());
        } catch (RuntimeException e) {
            log.error("Invalid timezone '{}' given for user '{}' will use default value.", getTimeZone(), getUsername(), e);
        }

        return ZoneId.of("GMT");
    }

    public String register() {
        final User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setTimeZone(getZoneId());
        user.setLocale(locale);

        if (StringUtils.isNotEmpty(studentId) || StringUtils.isNotEmpty(affiliation)) {
            user.setStudentId(studentId);
            user.setAffiliation(affiliation);
        }

        registerUser(user);

        if (course.getDefaultGroupId() != 0 || StringUtils.isNumeric(group)) {
            joinDefaultGroup(user);
        }

        if ((mailRequired || StringUtils.isNotEmpty(email)) && !user.isEmailConfirmed()) {
            user.sendEmailConfirmation();

            if (mailRequired) {
                confirmRequiredBean.setLoggedInUser(user);
                return "/lw/user/confirm_required.xhtml?faces-redirect=true";
            }
        }

        return LoginBean.loginUser(this, user);
    }

    private void registerUser(final User user) {
        user.setOrganisationId(course.getOrganisationId());
        user.setRegistrationDate(LocalDateTime.now());
        user.setPreferences(new HashMap<>());

        if (user.getEmail() != null) {
            try {
                ImmutableTriple<String, String, InputStream> gravatar = ProfileImageHelper.getGravatarAvatar(HashHelper.md5(user.getEmail()));

                if (gravatar != null) {
                    File file = new File(File.FileType.PROFILE_PICTURE, gravatar.getLeft(), gravatar.getMiddle());
                    dao().getFileDao().save(file, gravatar.getRight());
                    user.setImageFileId(file.getId());
                }
            } catch (IOException e) {
                log.error("Unable to save default avatar for user {}", user, e);
            }
        }

        userDao.save(user);
        log(Action.register, null, null, null, user);

        course.addUser(user);
    }

    private void joinDefaultGroup(final User user) {
        int groupToJoin = course.getDefaultGroupId();
        if (StringUtils.isNumeric(group)) {
            groupToJoin = NumberUtils.toInt(group);
        }

        if (groupToJoin != 0) {
            user.joinGroup(dao().getGroupDao().findByIdOrElseThrow(groupToJoin));
            log(Action.group_joining, groupToJoin, groupToJoin, null, user);
        }
    }

    public void validateUsername(FacesContext context, UIComponent component, Object value) {
        String newName = ((String) value).trim();

        if (newName.length() < 2) {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "The username is to short."));
        } else if (userDao.findByUsername(newName).isPresent()) {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "username_already_taken"));
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWizard() {
        return wizard;
    }

    public void setWizard(String wizard) {
        if (StringUtils.startsWith(wizard, "yell")) { // there exist many broken links in publications to wizards like: yell'A
            wizard = "yell";
        }
        this.wizard = wizard;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

    public String getFastLogin() {
        return fastLogin;
    }

    public void setFastLogin(final String fastLogin) {
        this.fastLogin = fastLogin;
    }

    public boolean isMailRequired() {
        return mailRequired;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public boolean isAffiliationRequired() {
        return affiliationRequired;
    }

    public boolean isStudentIdRequired() {
        return studentIdRequired;
    }

    public ConfirmRequiredBean getConfirmRequiredBean() {
        return confirmRequiredBean;
    }

    public void setConfirmRequiredBean(final ConfirmRequiredBean confirmRequiredBean) {
        this.confirmRequiredBean = confirmRequiredBean;
    }

    public boolean isAcceptPrivacyPolicy() {
        return acceptPrivacyPolicy;
    }

    public void setAcceptPrivacyPolicy(boolean acceptPrivacyPolicy) {
        this.acceptPrivacyPolicy = acceptPrivacyPolicy;
    }

    public boolean isAcceptTracking() {
        return acceptTracking;
    }

    public void setAcceptTracking(boolean acceptTracking) {
        this.acceptTracking = acceptTracking;
    }

    public Course getCourse() {
        return course;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(final String preferredTimeZone) {
        this.timeZone = preferredTimeZone.replaceAll("\"", "");
    }
}
