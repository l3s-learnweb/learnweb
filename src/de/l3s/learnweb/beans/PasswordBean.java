package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;
import de.l3s.util.MD5;
import de.l3s.util.Mail;

@ManagedBean
@RequestScoped
public class PasswordBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 2237249691336567548L;
    private static final Logger log = Logger.getLogger(PasswordBean.class);
    // this list contains addresses that did not except the password recovery mail
    private static Set<String> invalidMailAdresses = new HashSet<>(Arrays.asList("au576393@uni.au.dk", "au576393@au.dk", "josefine.mai.kraemer@au.dk", "au586648@uni.au.dk", "au587963@uni.au.dk", "139420@aulecsit.uniud.it", "au580386@uni.au.dk",
            "au567200@uni.au.dk", "139272@aulecsit.uniud.it", "au566300@uni.au.dk", "au568597@uni.au.dk"));

    private String email;

    public void onGetPassword()
    {
        try
        {
            if(invalidMailAdresses.contains(email))
            {
                String mailServer = email.substring(email.indexOf("@") + 1);
                addMessage(FacesMessage.SEVERITY_ERROR, "The mail server at " + mailServer + " responds that the address " + email + " doesn't exist. Contact our support team to solve this issue: learnweb-support@l3s.de");
                log.error("Can't send password recovery mail to " + email);
                return;
            }

            List<User> users = getLearnweb().getUserManager().getUser(email);

            if(users.size() == 0)
            {
                addMessage(FacesMessage.SEVERITY_ERROR, "unknown_email");
                return;
            }

            Mail message = new Mail();
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));

            String url = getLearnweb().getServerUrl() + "/lw/user/change_password.jsf?u=";

            for(User user : users)
            {
                String link = url + user.getId() + "_" + createHash(user);
                String text = "Hi " + user.getRealUsername() + ",\n\nyou can change the password of your learnweb account '" + user.getRealUsername() + "' by clicking on this link:\n" + link
                        + "\n\nOr just ignore this email, if you haven't requested it.\n\nBest regards,\nLearnweb Team";

                message.setText(text);
                message.setSubject("Retrieve learnweb password: " + user.getRealUsername());
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
