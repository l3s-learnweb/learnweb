package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

import de.l3s.learnweb.Course;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.User;

@ManagedBean
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

    @Email
    private String email;

    private String wizardTitle;

    private boolean mailRequired = false;

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

        //addMessage(FacesMessage.SEVERITY_INFO, "register_success");

        // log the user in
        UtilBean.getUserBean().setUser(user);

        //logging
        log(Action.register, 0, 0);
        if(null != course && course.getDefaultGroupId() != 0)
            log(Action.group_joining, course.getDefaultGroupId(), course.getDefaultGroupId());

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

    public void preRenderView() throws ValidatorException, SQLException
    {
        if(wizardTitle == null)
            wizardTitle = getFacesContext().getExternalContext().getRequestParameterMap().get("wizard");

        if(null != getWizard() && getWizard().length() != 0)
        {
            Course course = getLearnweb().getCourseManager().getCourseByWizard(getWizard());
            if(null == course)
            {
                addMessage(FacesMessage.SEVERITY_FATAL, "Invalid wizard parameter");
            }
            else
            {
                if(course.getId() == 505) // extrawurst für yell
                    addMessage(FacesMessage.SEVERITY_INFO, "register_for_community", course.getTitle());
                else
                    addMessage(FacesMessage.SEVERITY_INFO, "register_for_course", course.getTitle());

                mailRequired = course.getOption(Course.Option.Users_Require_mail_address);
            }
        }
        else
            addMessage(FacesMessage.SEVERITY_WARN, "register_without_wizard_warning");
    }

    public boolean isMailRequired()
    {
        return mailRequired;
    }

}
