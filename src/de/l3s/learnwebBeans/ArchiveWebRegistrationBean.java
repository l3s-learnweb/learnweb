package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Properties;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

import de.l3s.learnwebBeans.PasswordBean.GMailAuthenticator;

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
	String to = "fernando@l3s.de";

	Properties props = new Properties();
	props.put("mail.smtp.host", "smtp.gmail.com");
	props.put("mail.smtp.socketFactory.port", "465");
	props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
	props.put("mail.smtp.auth", "true");
	props.put("mail.smtp.port", "465");

	Session session = Session.getDefaultInstance(props, new GMailAuthenticator("interweb9@googlemail.com", "QDsG}GM5"));

	try
	{

	    MimeMessage message = new MimeMessage(session);
	    message.setFrom(new InternetAddress("interweb9@googlemail.com"));
	    message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
	    message.setSubject("ArchiveWeb Registration");
	    message.setText("username: " + username + "\npassword: " + password + "\nemail:" + email);
	    Transport.send(message);
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
    }

    public void validateUsername(FacesContext context, UIComponent component, Object value) throws ValidatorException, SQLException
    {
	if(getLearnweb().getUserManager().isUsernameAlreadyTaken((String) value))
	{
	    throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "username_already_taken"));
	}
    }
}
