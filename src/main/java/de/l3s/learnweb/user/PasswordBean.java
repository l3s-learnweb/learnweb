package de.l3s.learnweb.user;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.mail.MessagingException;

import org.apache.commons.lang3.RandomStringUtils;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.exceptions.HttpException;
import de.l3s.mail.Mail;
import de.l3s.mail.MailFactory;
import de.l3s.util.HashHelper;

@Named
@RequestScoped
public class PasswordBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 2237249691336567548L;
    //private static final Logger log = LogManager.getLogger(PasswordBean.class);

    private String email;

    @Inject
    private TokenDao tokenDao;

    public void onGetPassword() {
        try {
            List<User> users = tokenDao.getUserDao().findByEmail(email);

            String url = config().getServerUrl() + "/lw/user/change_password.jsf?token=";
            for (User user : users) {
                String token = RandomStringUtils.randomAlphanumeric(32);
                int tokenId = tokenDao.insert(user.getId(), Token.TokenType.PASSWORD_RESET, HashHelper.sha256(token), LocalDateTime.now().plusDays(1));
                String link = url + tokenId + ":" + token;

                Mail mail = MailFactory.buildPasswordChangeEmail(user.getRealUsername(), link).build(user.getLocale());
                mail.setRecipient(this.email);
                mail.send();
            }

            addMessage(FacesMessage.SEVERITY_INFO, "email_has_been_sent");
        } catch (MessagingException e) {
            throw new HttpException("Failed to handle change password", e);
        }
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
