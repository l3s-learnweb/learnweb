package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.util.List;
import java.util.Properties;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.util.MD5;

@ManagedBean
@RequestScoped
public class PasswordBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 2237249691336567548L;

    private String email;

    public static class GMailAuthenticator extends Authenticator
    {
	String user;
	String pw;

	public GMailAuthenticator(String username, String password)
	{
	    super();
	    this.user = username;
	    this.pw = password;
	}

	@Override
	public PasswordAuthentication getPasswordAuthentication()
	{
	    return new PasswordAuthentication(user, pw);
	}
    }

    public void onGetPassword()
    {
	try
	{
	    List<User> users = getLearnweb().getUserManager().getUser(email);

	    if(users.size() == 0)
	    {
		addMessage(FacesMessage.SEVERITY_ERROR, "unknown_email");
		return;
	    }

	    for(User user : users)
		sendMail(user);

	    addMessage(FacesMessage.SEVERITY_INFO, "email_has_been_send");
	}
	catch(Exception e)
	{
	    addFatalMessage(e);
	}
    }

    public static String createHash(User user)
    {
	return MD5.hash(Learnweb.salt1 + user.getId() + user.getPassword() + Learnweb.salt2);
    }

    private void sendMail(User user) throws AddressException, MessagingException
    {

	Properties props = new Properties();

	// props.put("mail.smtp.host", "mail.l3s.uni-hannover.de");
	/*
	 * //props.put("mail.smtp.port", "465");
	 * props.put("mail.transport.protocol","smtp");
	 * props.put("mail.smtp.auth", "true");
	 * props.put("mail.smtp.starttls.enable", "true");
	 * props.put("mail.smtp.tls", "true"); props.put("mail.smtp.user",
	 * "kemkes@l3s.de"); props.put("mail.password", "passwordXXXXXX");
	 * 
	 * 
	 * props.put("mail.smtp.auth", "true");
	 * props.put("mail.smtp.starttls.enable", "true");
	 * props.put("mail.smtp.host", "smtp.gmail.com");
	 * props.put("mail.smtp.port", "587"); ;;
	 */

	props.put("mail.smtp.host", "smtp.gmail.com");
	//props.put("mail.smtp.host", "mail.l3s.uni-hannover.de");
	props.put("mail.smtp.socketFactory.port", "465");
	props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
	props.put("mail.smtp.auth", "true");
	props.put("mail.smtp.port", "465");

	//props.put("mail.debug", "true");

	Session session1 = Session.getDefaultInstance(props, new GMailAuthenticator("interweb9@googlemail.com", "QDsG}GM5"));

	// Session session1 = Session.getDefaultInstance(props, new
	// GMailAuthenticator("kemkes", "password"));

	Message message = new MimeMessage(session1);

	message.setFrom(new InternetAddress("interweb9@googlemail.com"));

	message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(user.getEmail(), false));

	message.setSubject("Retrieve learnweb password");

	String link = UtilBean.getLearnwebBean().getContextUrl() + "/lw/user/change_password.jsf?u=" + user.getId() + "_" + createHash(user);

	message.setText("Hi " + user.getUsername() + ",\n\nyou can change the password of your learnweb account '" + user.getUsername() + "' by clicking on this link:\n" + link + "\n\nOr just ignore this email, if you haven't requested it.\n\nBest regards\nLearnweb Team");
	message.saveChanges();
	Transport.send(message);
    }

    public String getEmail()
    {
	return email;
    }

    public void setEmail(String email)
    {
	this.email = email;
    }
}
