package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.inject.Named;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import org.hibernate.validator.constraints.Length;

import de.l3s.learnweb.resource.SERVICE;
import de.l3s.util.bean.BeanHelper;
import de.l3s.util.email.Mail;

@Named
@RequestScoped
public class ArchiveWebRegistrationBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 1506604946829332647L;

    @NotBlank
    @Length(min = 2, max = 50)
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String affiliation;

    private String description;

    public ArchiveWebRegistrationBean()
    {
        // set default search options for anonymous archive it users
        setPreference("SEARCH_SERVICE_TEXT", SERVICE.archiveit.name());
        //setPreference("search_action", MODE.image.name());
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

    public void sendMail()
    {
        String adminEmail = getLearnweb().getProperties().getProperty("ADMIN_MAIL");

        try
        {
            Mail message = new Mail();
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(adminEmail));
            message.setSubject("ArchiveWeb Registration");
            message.setText("username: " + username + "\npassword: " + password + "\nemail: " + email + "\naffiliation: " + affiliation + "\ndescription: " + description + "\n\n-------\n" + BeanHelper.getRequestSummary());
            message.sendMail();

            addMessage(FacesMessage.SEVERITY_INFO, "Request sent successfully. We will get back to you shortly.");
            clearForm();
        }
        catch(MessagingException mex)
        {
            addErrorMessage(mex);
        }

    }

    public void clearForm()
    {
        username = null;
        password = null;
        email = null;
        affiliation = null;
        description = null;
    }

    public void validateUsername(FacesContext context, UIComponent component, Object value) throws ValidatorException, SQLException
    {
        if(getLearnweb().getUserManager().isUsernameAlreadyTaken((String) value))
        {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "username_already_taken"));
        }
    }

    public String getAffiliation()
    {
        return affiliation;
    }

    public void setAffiliation(String affiliation)
    {
        this.affiliation = affiliation;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
}
