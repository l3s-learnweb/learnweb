package de.l3s.learnweb.user;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.inject.Named;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.util.MD5;
import de.l3s.util.email.Mail;

@Named
@RequestScoped
public class PasswordBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 2237249691336567548L;
    //private static final Logger log = LogManager.getLogger(PasswordBean.class);

    private String email;

    public void onGetPassword() {
        try {
            List<User> users = getLearnweb().getUserManager().getUser(email);

            if (users.isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "unknown_email");
                return;
            }

            Mail message = new Mail();
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));

            String url = getLearnweb().getServerUrl() + "/lw/user/change_password.jsf?u=";

            for (User user : users) {
                String link = url + user.getId() + "_" + createPasswordChangeHash(user);
                String text = "Hi " + user.getRealUsername() + ",\n\nyou can change the password of your learnweb account '" + user.getRealUsername() + "' by clicking on this link:\n" + link
                    + "\n\nOr just ignore this email, if you haven't requested it.\n\nBest regards,\nLearnweb Team";

                message.setText(text);
                message.setSubject("Retrieve learnweb password: " + user.getRealUsername());
                message.sendMail();
            }
            //sendMail(user);

            addMessage(FacesMessage.SEVERITY_INFO, "email_has_been_sent");
        } catch (Exception e) {
            addErrorMessage(e);
        }
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public static String createPasswordChangeHash(User user) {
        return MD5.hash(Learnweb.SALT_1 + user.getId() + user.getPassword() + Learnweb.SALT_2);
    }
}
