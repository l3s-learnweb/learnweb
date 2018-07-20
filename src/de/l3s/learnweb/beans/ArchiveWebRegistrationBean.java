package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

import de.l3s.learnweb.resource.SERVICE;
import de.l3s.util.BeanHelper;
import de.l3s.util.email.Mail;

@ManagedBean
@RequestScoped
public class ArchiveWebRegistrationBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 1506604946829332647L;

    @Size(min = 2, max = 50)
    private String username;

    @NotEmpty
    private String password;

    @NotEmpty
    @Email
    private String email;

    @NotEmpty
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
        String to = "kemkes@l3s.de";

        try
        {
            Mail message = new Mail();// new MimeMessage(session);
            //message.setFrom(new InternetAddress("interweb9@googlemail.com"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject("ArchiveWeb Registration");
            message.setText("username: " + username + "\npassword: " + password + "\nemail: " + email + "\naffiliation: " + affiliation + "\ndescription: " + description + "\n\n-------\n" + BeanHelper.getRequestSummary());
            message.sendMail();
            //Transport.send(message);
        }
        catch(MessagingException mex)
        {
            addFatalMessage(mex);
        }
        clearForm();
        addMessage(FacesMessage.SEVERITY_INFO, "Request sent successfully. We will get back to your shortly.");
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
