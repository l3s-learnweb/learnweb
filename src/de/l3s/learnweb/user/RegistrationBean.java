package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.Size;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.user.Course.Option;

@Named
@ViewScoped
public class RegistrationBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 4567220515408089722L;

    @Size(min = 2, max = 50)
    private String username;

    @NotEmpty
    private String password;

    @NotEmpty
    private String confirmPassword;

    @NotEmpty
    private String studentId;

    @Email
    private String email;

    private String wizardTitle;
    private boolean wizardParamInvalid = false; // true if an invalid wizard parameter was given; no parameter is ok for the public course

    private String affiliation;

    private boolean mailRequired = false;
    private boolean affiliationRequired = false;
    private boolean studentIdRequired = false;

    @Inject
    private ConfirmRequiredBean confirmRequiredBean;

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

    public String getConfirmPassword()
    {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword)
    {
        this.confirmPassword = confirmPassword;
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
        return wizardTitle;
    }

    public void setWizard(String wizard)
    {
        this.wizardTitle = wizard;
    }

    public String register() throws Exception
    {
        Course course = null;
        Learnweb learnweb = getLearnweb();

        if(null != wizardTitle && wizardTitle.length() != 0)
        {
            course = learnweb.getCourseManager().getCourseByWizard(wizardTitle);

            if(null == course)
            {
                addMessage(FacesMessage.SEVERITY_FATAL, "invalid wizard parameter");
                return null;
            }
        }

        final User user = learnweb.getUserManager().registerUser(username, password, email, wizardTitle);

        if(studentIdRequired || affiliationRequired)
        {
            user.setStudentId(studentId);
            user.setAffiliation(affiliation);
            user.save();
        }
        //addMessage(FacesMessage.SEVERITY_INFO, "register_success");

        //logging
        log(Action.register, 0, 0, null, user);
        if(null != course && course.getDefaultGroupId() != 0)
            log(Action.group_joining, course.getDefaultGroupId(), course.getDefaultGroupId(), null, user);

        if(!user.isEmailConfirmed())
        {
            user.sendEmailConfirmation();
            confirmRequiredBean.setLoggedInUser(user);

            return "/lw/user/confirm_required.xhtml?faces-redirect=true";
        }

        return LoginBean.loginUser(this, user);
    }

    public void validatePassword(FacesContext context, UIComponent component, Object value) throws ValidatorException
    {
        // Find the actual JSF component for the first password field.
        UIInput passwordInput = (UIInput) context.getViewRoot().findComponent("registerform:password");

        // Get its value, the entered password of the first field.
        String password = (String) passwordInput.getValue();

        if(null != password && !password.equals(value))
        {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "passwords_do_not_match"));
        }
    }

    public void validateUsername(FacesContext context, UIComponent component, Object value) throws ValidatorException, SQLException
    {
        if(getLearnweb().getUserManager().isUsernameAlreadyTaken((String) value))
        {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "username_already_taken"));
        }
    }

    public void validateEmailAddress(FacesContext context, UIComponent component, Object value) throws ValidatorException, SQLException
    {
        String email = (String) value;
        if(email != null && email.trim().endsWith("aulecsit.uniud.it"))
        {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "This mail address is invalid!"
                    + "Usually it is surname.name@spes.uniud.it"));
        }
    }

    public void preRenderView() throws ValidatorException, SQLException
    {
        if(wizardTitle == null)
            wizardTitle = getFacesContext().getExternalContext().getRequestParameterMap().get("wizard");

        if(null != wizardTitle && wizardTitle.length() != 0)
        {
            Course course = getLearnweb().getCourseManager().getCourseByWizard(wizardTitle);
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
                if(course.getId() == 505) // special message for yell
                    addMessage(FacesMessage.SEVERITY_INFO, "register_for_community", course.getTitle());
                else
                    addMessage(FacesMessage.SEVERITY_INFO, "register_for_course", course.getTitle());

                mailRequired = course.getOption(Course.Option.Users_Require_mail_address);
                affiliationRequired = course.getOption(Course.Option.Users_Require_affiliation);
                studentIdRequired = course.getOption(Course.Option.Users_Require_student_id);
            }
        }
        else
            addMessage(FacesMessage.SEVERITY_WARN, "register_without_wizard_warning");
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
}
