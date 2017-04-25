package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;
import de.l3s.util.MD5;
import de.l3s.util.Mail;

@ManagedBean
@RequestScoped
public class PasswordBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 2237249691336567548L;

    private String email;

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

            Mail message = new Mail();
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));

            String url = getLearnweb().getContextUrl() + "/lw/user/change_password.jsf?u=";

            for(User user : users)
            {
                String link = url + user.getId() + "_" + createHash(user);
                String text = "Hi " + user.getUsername() + ",\n\nyou can change the password of your learnweb account '" + user.getUsername() + "' by clicking on this link:\n" + link + "\n\nOr just ignore this email, if you haven't requested it.\n\nBest regards\nLearnweb Team";

                message.setText(text);
                message.setSubject("Retrieve learnweb password: " + user.getUsername());
                message.sendMail();
            }
            //sendMail(user);

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

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }
}
