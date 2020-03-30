package de.l3s.learnweb.user;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Length;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.user.Course.Option;

@Named
@ViewScoped
public class RegistrationBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 4567220515408089722L;

    @NotBlank
    @Length(min = 2, max = 50)
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
    private boolean wizardParamInvalid = false; // true if an invalid wizard parameter was given; no parameter is ok for the public course

    private String affiliation;

    private Course course;
    private boolean mailRequired = false;
    private boolean affiliationRequired = false;
    private boolean studentIdRequired = false;

    @Inject
    private ConfirmRequiredBean confirmRequiredBean;

    public String onLoad() throws IOException, SQLException
    {
        if(StringUtils.isNotEmpty(wizard))
        {
            course = getLearnweb().getCourseManager().getCourseByWizard(wizard);
            if(null == course)
            {
                addMessage(FacesMessage.SEVERITY_FATAL, "register_invalid_wizard_error");
                wizardParamInvalid = true;
            }
            else if(course.getOption(Option.Users_Disable_wizard))
            {
                addMessage(FacesMessage.SEVERITY_ERROR, "registration.wizard_disabled");
                wizardParamInvalid = true;
            }
            else
            {
                // special message for yell
                if(course.getId() == 505)
                    addMessage(FacesMessage.SEVERITY_INFO, "register_for_community", course.getTitle());
                else
                    addMessage(FacesMessage.SEVERITY_INFO, "register_for_course", course.getTitle());

                mailRequired = course.getOption(Course.Option.Users_Require_mail_address);
                affiliationRequired = course.getOption(Course.Option.Users_Require_affiliation);
                studentIdRequired = course.getOption(Course.Option.Users_Require_student_id);

                if(StringUtils.isNotEmpty(fastLogin))
                    return fastLogin();
            }
        }
        else
        {
            addMessage(FacesMessage.SEVERITY_WARN, "register_without_wizard_warning");
        }

        return null;
    }

    private String fastLogin() throws SQLException, IOException
    {
        User user = getLearnweb().getUserManager().getUserByUsername(fastLogin);

        if(user != null)
        {
            if(user.getPassword() == null && user.isMemberOfCourse(course.getId()))
            {
                return LoginBean.loginUser(this, user);
            }
            else
            {
                addMessage(FacesMessage.SEVERITY_FATAL, "You should use password to login.");
                return "/lw/user/login.xhtml?faces-redirect=true";
            }
        }
        else
        {
            user = getLearnweb().getUserManager().registerUser(fastLogin, null, null, course);
            return LoginBean.loginUser(this, user);
        }
    }

    public String register() throws IOException, SQLException
    {
        final User user = getLearnweb().getUserManager().registerUser(username, password, email, course);

        if(StringUtils.isNotEmpty(studentId) || StringUtils.isNotEmpty(affiliation))
        {
            user.setStudentId(studentId);
            user.setAffiliation(affiliation);
            user.save();
        }

        log(Action.register, 0, 0, null, user);
        if(null != course && course.getDefaultGroupId() != 0)
        {
            user.joinGroup(course.getDefaultGroupId());
            log(Action.group_joining, course.getDefaultGroupId(), course.getDefaultGroupId(), null, user);
        }

        if((mailRequired || StringUtils.isNotEmpty(email)) && !user.isEmailConfirmed())
        {
            user.sendEmailConfirmation();

            if(mailRequired)
            {
                confirmRequiredBean.setLoggedInUser(user);
                return "/lw/user/confirm_required.xhtml?faces-redirect=true";
            }
        }

        return LoginBean.loginUser(this, user);
    }

    public void validateUsername(FacesContext context, UIComponent component, Object value) throws SQLException
    {
        String newName = ((String) value).trim();

        if(newName.length() < 2)
        {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "The username is to short."));
        }
        if(getLearnweb().getUserManager().isUsernameAlreadyTaken(newName))
        {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "username_already_taken"));
        }
    }

    public void validateEmailAddress(FacesContext context, UIComponent component, Object value) throws ValidatorException, SQLException
    {
        String email = (String) value;
        if(email == null)
            return;
        email = email.trim().toLowerCase();

        if(StringUtils.endsWithAny(email, "aulecsit.uniud.it", "uni.au.dk", "studeniti.unisalento.it"))
        {
            if(email.endsWith("aulecsit.uniud.it"))
            {
                throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "This mail address is invalid! Usually it is surname.name@spes.uniud.it"));
            }
            else
            {
                throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "This mail address is invalid! Check the domain."));
            }
        }
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getWizard()
    {
        return wizard;
    }

    public void setWizard(String wizard)
    {
        this.wizard = wizard;
    }

    public String getFastLogin()
    {
        return fastLogin;
    }

    public void setFastLogin(final String fastLogin)
    {
        this.fastLogin = fastLogin;
    }

    public boolean isMailRequired()
    {
        return mailRequired;
    }

    public String getAffiliation()
    {
        return affiliation;
    }

    public void setAffiliation(String affiliation)
    {
        this.affiliation = affiliation;
    }

    public String getStudentId()
    {
        return studentId;
    }

    public void setStudentId(String studentId)
    {
        this.studentId = studentId;
    }

    public boolean isAffiliationRequired()
    {
        return affiliationRequired;
    }

    public boolean isStudentIdRequired()
    {
        return studentIdRequired;
    }

    public boolean isWizardParamInvalid()
    {
        return wizardParamInvalid;
    }

    public ConfirmRequiredBean getConfirmRequiredBean()
    {
        return confirmRequiredBean;
    }

    public void setConfirmRequiredBean(final ConfirmRequiredBean confirmRequiredBean)
    {
        this.confirmRequiredBean = confirmRequiredBean;
    }

    public boolean isAcceptPrivacyPolicy()
    {
        return acceptPrivacyPolicy;
    }

    public void setAcceptPrivacyPolicy(boolean acceptPrivacyPolicy)
    {
        this.acceptPrivacyPolicy = acceptPrivacyPolicy;
    }

    public boolean isAcceptTracking()
    {
        return acceptTracking;
    }

    public void setAcceptTracking(boolean acceptTracking)
    {
        this.acceptTracking = acceptTracking;
    }

    public Course getCourse()
    {
        return course;
    }

}
