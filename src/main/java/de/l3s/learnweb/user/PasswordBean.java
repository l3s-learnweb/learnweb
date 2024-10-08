package de.l3s.learnweb.user;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import de.l3s.mail.MailService;
import de.l3s.util.HashHelper;

@Named
@RequestScoped
public class PasswordBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 2237249691336567548L;

    private String identifier; // email or username

    @Inject
    private TokenDao tokenDao;

    @Inject
    private MailService mailService;

    public void submit() {
        try {
            List<User> users = new ArrayList<>();
            if (identifier != null && identifier.contains("@")) {
                List<User> foundUsers = tokenDao.getUserDao().findByEmail(identifier);
                users.addAll(foundUsers);
            } else if (identifier != null) {
                Optional<User> foundUser = tokenDao.getUserDao().findByUsername(identifier);
                foundUser.ifPresent(users::add);
            }

            String url = config().getServerUrl() + "/lw/user/change_password.jsf?token=";
            for (User user : users) {
                String token = RandomStringUtils.secure().nextAlphanumeric(32);
                int tokenId = tokenDao.insert(user.getId(), Token.TokenType.PASSWORD_RESET, HashHelper.sha256(token), LocalDateTime.now().plusDays(1));
                String link = url + tokenId + ":" + token;

                Mail mail = MailFactory.buildPasswordChangeEmail(user.getDisplayName(), user.getEmail(), link).build(user.getLocale());
                mail.addRecipient(user.getEmail());
                mailService.send(mail);
            }

            addMessage(FacesMessage.SEVERITY_INFO, "email_has_been_sent");
        } catch (MessagingException e) {
            throw new HttpException("Failed to handle change password", e);
        }
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
