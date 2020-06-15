package de.l3s.learnweb.user;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.exceptions.BeanAsserts;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.user.Course.Option;

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
    private String fastLogin;

    private String affiliation;

    private Course course;
    private boolean mailRequired = false;
    private boolean affiliationRequired = false;
    private boolean studentIdRequired = false;
    private String timeZone;
    private Locale locale;

    @Inject
    private ConfirmRequiredBean confirmRequiredBean;

    public String onLoad() throws IOException, SQLException {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        UIViewRoot viewRoot = FacesContext.getCurrentInstance().getViewRoot();
        locale = viewRoot != null ? viewRoot.getLocale() : externalContext.getRequestLocale();

        if (StringUtils.isNotEmpty(wizard)) {
            course = getLearnweb().getCourseManager().getCourseByWizard(wizard);
            BeanAsserts.validateNotNull(course, "register_invalid_wizard_error");
            BeanAsserts.validate(!course.getOption(Option.Users_Disable_wizard), "registration.wizard_disabled");

            // special message for yell
            if (course.getId() == 505) {
                addMessage(FacesMessage.SEVERITY_INFO, "register_for_community", course.getTitle());
            } else {
                addMessage(FacesMessage.SEVERITY_INFO, "register_for_course", course.getTitle());
            }

            mailRequired = course.getOption(Course.Option.Users_Require_mail_address);
            affiliationRequired = course.getOption(Course.Option.Users_Require_affiliation);
            studentIdRequired = course.getOption(Course.Option.Users_Require_student_id);

            if (StringUtils.isNotEmpty(fastLogin)) {
                return fastLogin();
            }
        } else {
            addMessage(FacesMessage.SEVERITY_WARN, "register_without_wizard_warning");
        }

        return null;
    }

    private String fastLogin() throws SQLException, IOException {
        User user = getLearnweb().getUserManager().getUserByUsername(fastLogin);

        if (user != null) {
            if (user.getPassword() == null && user.isMemberOfCourse(course.getId())) {
                return LoginBean.loginUser(this, user);
            } else {
                addMessage(FacesMessage.SEVERITY_FATAL, "You should use password to login.");
                return "/lw/user/login.xhtml?faces-redirect=true";
            }
        } else {
            user = new User();
            user.setUsername(fastLogin);
            user.setEmail(null);
            user.setPassword(null);
            user.setTimeZone(ZoneId.of("Europe/Berlin"));
            user.setLocale(locale);

            getLearnweb().getUserManager().registerUser(user, course);
            return LoginBean.loginUser(this, user);
        }
    }

    /**
     * Handles errors that may occur while retrieving the users time zone
     *
     * @return ZoneId given by the user or GMT as default value
     */
    private ZoneId getZoneId() {
        try {
            return ZoneId.of(getTimeZone());
        } catch (NullPointerException | DateTimeException e) {
            log.error("Invalid timezone '{}' given for user '{}' will use default value.", getTimeZone(), getUsername(), e);
        }

        return ZoneId.of("GMT");
    }

    public String register() throws IOException, SQLException {
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

        getLearnweb().getUserManager().registerUser(user, course);

        log(Action.register, 0, 0, null, user);
        if (null != course && course.getDefaultGroupId() != 0) {
            user.joinGroup(course.getDefaultGroupId());
            log(Action.group_joining, course.getDefaultGroupId(), course.getDefaultGroupId(), null, user);
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

    public void validateUsername(FacesContext context, UIComponent component, Object value) throws SQLException {
        String newName = ((String) value).trim();

        if (newName.length() < 2) {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "The username is to short."));
        } else if (getLearnweb().getUserManager().isUsernameAlreadyTaken(newName)) {
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
        this.wizard = wizard;
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
