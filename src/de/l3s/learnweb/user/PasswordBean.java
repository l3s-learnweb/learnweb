package de.l3s.learnweb.user;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.RandomStringUtils;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.util.HashHelper;
import de.l3s.util.email.Mail;

@Named
@RequestScoped
public class PasswordBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 2237249691336567548L;
    //private static final Logger log = LogManager.getLogger(PasswordBean.class);

    private String email;

    @Inject
    private TokenDao tokenDao;

    public void onGetPassword() {
        try {
            List<User> users = tokenDao.getUserDao().findByEmail(email);

            if (users.isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "unknown_email");
                return;
            }

            Mail message = new Mail();
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));

            String url = config().getServerUrl() + "/lw/user/change_password.jsf?token=";
            for (User user : users) {
                String token = RandomStringUtils.randomAlphanumeric(32);
                int tokenId = tokenDao.insert(user.getId(), Token.TokenType.PASSWORD_RESET, HashHelper.sha256(token), LocalDateTime.now().plusDays(1));

                String link = url + tokenId + ":" + token;
                String text = "Hi " + user.getRealUsername() + ",\n\nyou can change the password of your learnweb account '" + user.getRealUsername() +
                    "' by clicking on this link:\n" + link + "\n\nThe link will expire in 24 hours.\n" +
                    "You can just ignore this email, if you haven't requested it.\n\nBest regards,\nLearnweb Team";

                message.setText(text);
                message.setSubject("Retrieve learnweb password: " + user.getRealUsername());
                message.sendMail();
            }

            addMessage(FacesMessage.SEVERITY_INFO, "email_has_been_sent");
        } catch (MessagingException e) {
            addErrorMessage(e);
        }
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
